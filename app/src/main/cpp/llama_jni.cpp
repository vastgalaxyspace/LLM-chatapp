// JNI bridge between com.example.chatapp.data.engine.LlamaBridge and llama.cpp.
//
// Threading contract: all calls for a given session handle are serialized by the
// Kotlin side (ChatEngineManager holds a generation mutex). The only cross-thread
// access is the abort flag, which is atomic.

#include <jni.h>
#include <android/log.h>

#include <atomic>
#include <cstring>
#include <string>
#include <vector>

#include "llama.h"

#define TAG "InnoAILlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace {

struct Session {
    llama_model * model = nullptr;
    llama_context * ctx = nullptr;
    const llama_vocab * vocab = nullptr;

    // Per-completion state.
    llama_sampler * sampler = nullptr;
    llama_token current_token = 0;
    int n_generated = 0;
    int n_predict = 0;
    std::string pending_utf8;
    std::atomic_bool abort_requested{false};
};

Session * from_handle(jlong handle) {
    return reinterpret_cast<Session *>(handle);
}

// Returns true when `s` is complete, valid UTF-8 (no truncated trailing sequence).
bool is_valid_utf8(const std::string & s) {
    const auto * bytes = reinterpret_cast<const unsigned char *>(s.data());
    size_t len = s.size();
    size_t i = 0;
    while (i < len) {
        unsigned char c = bytes[i];
        size_t seq;
        if (c < 0x80) seq = 1;
        else if ((c & 0xE0) == 0xC0) seq = 2;
        else if ((c & 0xF0) == 0xE0) seq = 3;
        else if ((c & 0xF8) == 0xF0) seq = 4;
        else return false;
        if (i + seq > len) return false;
        for (size_t j = 1; j < seq; j++) {
            if ((bytes[i + j] & 0xC0) != 0x80) return false;
        }
        i += seq;
    }
    return true;
}

std::string jstring_to_utf8(JNIEnv * env, jstring value) {
    if (value == nullptr) return {};
    const char * chars = env->GetStringUTFChars(value, nullptr);
    std::string result(chars == nullptr ? "" : chars);
    if (chars != nullptr) env->ReleaseStringUTFChars(value, chars);
    return result;
}

void free_completion_state(Session * session) {
    if (session->sampler != nullptr) {
        llama_sampler_free(session->sampler);
        session->sampler = nullptr;
    }
    session->pending_utf8.clear();
    session->n_generated = 0;
    session->n_predict = 0;
}

std::vector<llama_token> tokenize_prompt(const llama_vocab * vocab, const std::string & prompt) {
    const int n_max = -llama_tokenize(vocab, prompt.c_str(), (int32_t) prompt.size(), nullptr, 0, true, true);
    std::vector<llama_token> tokens(std::max(n_max, 0));
    if (n_max > 0) {
        llama_tokenize(vocab, prompt.c_str(), (int32_t) prompt.size(), tokens.data(), n_max, true, true);
    }
    return tokens;
}

} // namespace

extern "C" JNIEXPORT void JNICALL
Java_com_example_chatapp_data_engine_LlamaBridge_nativeBackendInit(JNIEnv *, jobject) {
    llama_backend_init();
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_chatapp_data_engine_LlamaBridge_nativeLoadModel(
        JNIEnv * env, jobject, jstring model_path, jint n_ctx, jint n_threads) {
    const std::string path = jstring_to_utf8(env, model_path);

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0; // CPU-only build

    llama_model * model = llama_model_load_from_file(path.c_str(), model_params);
    if (model == nullptr) {
        LOGE("Failed to load model from %s", path.c_str());
        return 0;
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = (uint32_t) n_ctx;
    ctx_params.n_batch = 512;
    ctx_params.n_ubatch = 512;
    ctx_params.n_threads = n_threads;
    ctx_params.n_threads_batch = n_threads;

    llama_context * ctx = llama_init_from_model(model, ctx_params);
    if (ctx == nullptr) {
        LOGE("Failed to create llama context");
        llama_model_free(model);
        return 0;
    }

    auto * session = new Session();
    session->model = model;
    session->ctx = ctx;
    session->vocab = llama_model_get_vocab(model);
    LOGI("Model loaded: n_ctx=%d n_threads=%d", n_ctx, n_threads);
    return reinterpret_cast<jlong>(session);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_chatapp_data_engine_LlamaBridge_nativeApplyChatTemplate(
        JNIEnv * env, jobject, jlong handle, jobjectArray roles, jobjectArray contents) {
    Session * session = from_handle(handle);
    if (session == nullptr) return nullptr;

    const jsize count = env->GetArrayLength(roles);
    std::vector<std::string> role_strings(count);
    std::vector<std::string> content_strings(count);
    std::vector<llama_chat_message> messages(count);
    for (jsize i = 0; i < count; i++) {
        auto role = (jstring) env->GetObjectArrayElement(roles, i);
        auto content = (jstring) env->GetObjectArrayElement(contents, i);
        role_strings[i] = jstring_to_utf8(env, role);
        content_strings[i] = jstring_to_utf8(env, content);
        env->DeleteLocalRef(role);
        env->DeleteLocalRef(content);
        messages[i] = { role_strings[i].c_str(), content_strings[i].c_str() };
    }

    const char * tmpl = llama_model_chat_template(session->model, nullptr);
    std::vector<char> buf(std::max<size_t>(4096, content_strings.size() * 1024 + 4096));
    int32_t written = -1;
    if (tmpl != nullptr) {
        written = llama_chat_apply_template(tmpl, messages.data(), messages.size(), true, buf.data(), (int32_t) buf.size());
        if (written > (int32_t) buf.size()) {
            buf.resize(written + 1);
            written = llama_chat_apply_template(tmpl, messages.data(), messages.size(), true, buf.data(), (int32_t) buf.size());
        }
    }

    std::string prompt;
    if (written >= 0) {
        prompt.assign(buf.data(), (size_t) written);
    } else {
        // Fallback: ChatML-style layout understood by most instruct models.
        LOGE("Chat template unavailable or failed (%d); using ChatML fallback", written);
        for (jsize i = 0; i < count; i++) {
            prompt += "<|im_start|>" + role_strings[i] + "\n" + content_strings[i] + "<|im_end|>\n";
        }
        prompt += "<|im_start|>assistant\n";
    }
    return env->NewStringUTF(prompt.c_str());
}

// Returns 0 on success, -1 invalid session, -2 prompt too long for the context,
// -3 tokenization failure, -4 decode failure.
extern "C" JNIEXPORT jint JNICALL
Java_com_example_chatapp_data_engine_LlamaBridge_nativeCompletionInit(
        JNIEnv * env, jobject, jlong handle, jstring prompt_j, jint n_predict,
        jfloat temperature, jint top_k, jfloat top_p, jint seed) {
    Session * session = from_handle(handle);
    if (session == nullptr || session->ctx == nullptr) return -1;

    free_completion_state(session);
    session->abort_requested.store(false);

    const std::string prompt = jstring_to_utf8(env, prompt_j);
    std::vector<llama_token> tokens = tokenize_prompt(session->vocab, prompt);
    if (tokens.empty()) {
        LOGE("Prompt tokenization produced no tokens");
        return -3;
    }

    const int n_ctx = (int) llama_n_ctx(session->ctx);
    if ((int) tokens.size() + 8 > n_ctx) {
        LOGE("Prompt too long: %zu tokens for n_ctx=%d", tokens.size(), n_ctx);
        return -2;
    }

    // Start every completion from a clean context; history is part of the prompt.
    llama_memory_clear(llama_get_memory(session->ctx), true);

    const int n_batch = 512;
    for (size_t start = 0; start < tokens.size(); start += n_batch) {
        if (session->abort_requested.load()) return -4;
        const int chunk = (int) std::min<size_t>(n_batch, tokens.size() - start);
        llama_batch batch = llama_batch_get_one(tokens.data() + start, chunk);
        if (llama_decode(session->ctx, batch) != 0) {
            LOGE("llama_decode failed during prefill");
            return -4;
        }
    }

    llama_sampler_chain_params chain_params = llama_sampler_chain_default_params();
    llama_sampler * sampler = llama_sampler_chain_init(chain_params);
    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(top_k));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(top_p, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(seed == 0 ? LLAMA_DEFAULT_SEED : (uint32_t) seed));

    session->sampler = sampler;
    session->n_generated = 0;
    session->n_predict = n_predict;

    const int remaining = n_ctx - (int) tokens.size() - 1;
    if (session->n_predict > remaining) session->n_predict = remaining;

    LOGI("Completion init: prompt_tokens=%zu n_predict=%d", tokens.size(), session->n_predict);
    return 0;
}

// Returns the next UTF-8 text piece, "" when a piece is still incomplete
// (multi-byte sequence pending), or null when generation has finished.
extern "C" JNIEXPORT jstring JNICALL
Java_com_example_chatapp_data_engine_LlamaBridge_nativeCompletionLoop(
        JNIEnv * env, jobject, jlong handle) {
    Session * session = from_handle(handle);
    if (session == nullptr || session->sampler == nullptr) return nullptr;
    if (session->abort_requested.load()) return nullptr;
    if (session->n_generated >= session->n_predict) return nullptr;

    const llama_token token = llama_sampler_sample(session->sampler, session->ctx, -1);
    if (llama_vocab_is_eog(session->vocab, token)) {
        return nullptr;
    }

    char piece[256];
    const int n_piece = llama_token_to_piece(session->vocab, token, piece, sizeof(piece), 0, true);
    if (n_piece > 0) {
        session->pending_utf8.append(piece, (size_t) n_piece);
    }

    session->current_token = token;
    llama_batch batch = llama_batch_get_one(&session->current_token, 1);
    if (llama_decode(session->ctx, batch) != 0) {
        LOGE("llama_decode failed during generation");
        return nullptr;
    }
    session->n_generated++;

    if (is_valid_utf8(session->pending_utf8)) {
        jstring result = env->NewStringUTF(session->pending_utf8.c_str());
        session->pending_utf8.clear();
        return result;
    }
    return env->NewStringUTF("");
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_chatapp_data_engine_LlamaBridge_nativeCompletionEnd(
        JNIEnv *, jobject, jlong handle) {
    Session * session = from_handle(handle);
    if (session != nullptr) {
        free_completion_state(session);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_chatapp_data_engine_LlamaBridge_nativeRequestAbort(
        JNIEnv *, jobject, jlong handle) {
    Session * session = from_handle(handle);
    if (session != nullptr) {
        session->abort_requested.store(true);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_chatapp_data_engine_LlamaBridge_nativeFree(
        JNIEnv *, jobject, jlong handle) {
    Session * session = from_handle(handle);
    if (session == nullptr) return;
    free_completion_state(session);
    if (session->ctx != nullptr) llama_free(session->ctx);
    if (session->model != nullptr) llama_model_free(session->model);
    delete session;
}

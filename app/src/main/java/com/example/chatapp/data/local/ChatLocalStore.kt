package com.example.chatapp.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.chatapp.data.model.ChatMessage
import com.example.chatapp.data.model.MessageRole
import com.example.chatapp.data.model.MessageType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ConversationSummary(
    val id: Long,
    val title: String,
    val preview: String,
    val updatedAt: Long,
    val messageCount: Int
)

data class ProfileStats(
    val conversationCount: Int,
    val messageCount: Int,
    val userMessages: Int,
    val aiMessages: Int
)

data class ConversationSearchResult(
    val conversationId: Long,
    val messageId: String,
    val role: MessageRole,
    val snippet: String,
    val timestamp: Long
)

@Singleton
class ChatLocalStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dbHelper = ChatDatabaseHelper(context)
    private val changeCounter = MutableStateFlow(0L)

    suspend fun ensureConversation(conversationId: Long?): Long = withContext(Dispatchers.IO) {
        when {
            conversationId != null && conversationExists(conversationId) -> conversationId
            else -> latestConversationId() ?: createConversation()
        }
    }

    fun observeConversationMessages(conversationId: Long): Flow<List<ChatMessage>> =
        changeCounter
            .map { loadConversationMessages(conversationId) }
            .onStart { emit(loadConversationMessages(conversationId)) }
            .flowOn(Dispatchers.IO)

    fun observeConversationSummaries(): Flow<List<ConversationSummary>> =
        changeCounter
            .map { loadConversationSummaries() }
            .onStart { emit(loadConversationSummaries()) }
            .flowOn(Dispatchers.IO)

    fun observeProfileStats(): Flow<ProfileStats> =
        changeCounter
            .map { loadProfileStats() }
            .onStart { emit(loadProfileStats()) }
            .flowOn(Dispatchers.IO)

    suspend fun insertMessage(conversationId: Long, message: ChatMessage) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.insertWithOnConflict(
            TABLE_MESSAGES,
            null,
            ContentValues().apply {
                put(COL_MESSAGE_ID, message.id)
                put(COL_MESSAGE_CONVERSATION_ID, conversationId)
                put(COL_MESSAGE_ROLE, message.role.name)
                put(COL_MESSAGE_CONTENT, message.content)
                put(COL_MESSAGE_TIMESTAMP, message.timestamp)
                put(COL_MESSAGE_IS_STREAMING, if (message.isStreaming) 1 else 0)
                put(COL_MESSAGE_TYPE, message.type.name)
                put(COL_MESSAGE_MEDIA_URI, message.mediaUri)
                put(COL_MESSAGE_MIME_TYPE, message.mimeType)
                put(COL_MESSAGE_DURATION_MILLIS, message.durationMillis)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
        upsertMessageSearchIndex(db, conversationId, message)
        touchConversation(db, conversationId, message)
        notifyChange()
    }

    suspend fun updateMessageContent(messageId: String, content: String, isStreaming: Boolean) =
        withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            db.update(
                TABLE_MESSAGES,
                ContentValues().apply {
                    put(COL_MESSAGE_CONTENT, content)
                    put(COL_MESSAGE_IS_STREAMING, if (isStreaming) 1 else 0)
                },
                "$COL_MESSAGE_ID = ?",
                arrayOf(messageId)
            )
            db.execSQL(
                "UPDATE $TABLE_MESSAGES_FTS SET $COL_MESSAGE_CONTENT = ? WHERE $COL_MESSAGE_ID = ?",
                arrayOf(content, messageId)
            )
            notifyChange()
        }

    suspend fun finishAbandonedStreamingMessages() = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.update(
            TABLE_MESSAGES,
            ContentValues().apply {
                put(COL_MESSAGE_IS_STREAMING, 0)
            },
            "$COL_MESSAGE_IS_STREAMING = ?",
            arrayOf("1")
        )
        notifyChange()
    }

    suspend fun deleteAllHistory() = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_MESSAGES, null, null)
            db.delete(TABLE_MESSAGES_FTS, null, null)
            db.delete(TABLE_CONVERSATIONS, null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        createConversation()
        notifyChange()
    }

    suspend fun latestConversationIdOrCreate(): Long = withContext(Dispatchers.IO) {
        latestConversationId() ?: createConversation()
    }

    suspend fun createNewConversation(): Long = withContext(Dispatchers.IO) {
        val id = createConversation("New Chat")
        notifyChange()
        id
    }

    suspend fun deleteConversation(conversationId: Long): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(
                TABLE_MESSAGES,
                "$COL_MESSAGE_CONVERSATION_ID = ?",
                arrayOf(conversationId.toString())
            )
            db.delete(
                TABLE_MESSAGES_FTS,
                "$COL_MESSAGE_CONVERSATION_ID = ?",
                arrayOf(conversationId.toString())
            )
            db.delete(
                TABLE_CONVERSATIONS,
                "$COL_CONVERSATION_ID = ?",
                arrayOf(conversationId.toString())
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        val nextConversationId = latestConversationId() ?: createConversation("New Chat")
        notifyChange()
        nextConversationId
    }

    suspend fun renameConversation(conversationId: Long, newTitle: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.update(
            TABLE_CONVERSATIONS,
            ContentValues().apply { put(COL_CONVERSATION_TITLE, newTitle.trim().take(80)) },
            "$COL_CONVERSATION_ID = ?",
            arrayOf(conversationId.toString())
        )
        notifyChange()
    }

    private fun loadConversationMessages(conversationId: Long): List<ChatMessage> {
        val db = dbHelper.readableDatabase
        return db.query(
            TABLE_MESSAGES,
            arrayOf(
                COL_MESSAGE_ID,
                COL_MESSAGE_CONTENT,
                COL_MESSAGE_ROLE,
                COL_MESSAGE_TIMESTAMP,
                COL_MESSAGE_IS_STREAMING,
                COL_MESSAGE_TYPE,
                COL_MESSAGE_MEDIA_URI,
                COL_MESSAGE_MIME_TYPE,
                COL_MESSAGE_DURATION_MILLIS
            ),
            "$COL_MESSAGE_CONVERSATION_ID = ?",
            arrayOf(conversationId.toString()),
            null,
            null,
            "$COL_MESSAGE_TIMESTAMP ASC"
        ).use { cursor ->
            val messages = mutableListOf<ChatMessage>()
            while (cursor.moveToNext()) {
                messages += ChatMessage(
                    id = cursor.getString(0),
                    content = cursor.getString(1),
                    role = MessageRole.valueOf(cursor.getString(2)),
                    timestamp = cursor.getLong(3),
                    isStreaming = cursor.getInt(4) == 1,
                    type = runCatching { MessageType.valueOf(cursor.getString(5)) }.getOrDefault(MessageType.TEXT),
                    mediaUri = cursor.getString(6),
                    mimeType = cursor.getString(7),
                    durationMillis = if (cursor.isNull(8)) null else cursor.getLong(8)
                )
            }
            messages
        }
    }

    suspend fun searchMessages(query: String): List<ConversationSearchResult> = withContext(Dispatchers.IO) {
        val normalized = query.trim()
        if (normalized.length < 2) return@withContext emptyList()
        val db = dbHelper.readableDatabase
        val ftsQuery = normalized
            .split(Regex("""\s+"""))
            .map { it.replace(Regex("""[^\p{L}\p{N}_-]"""), "") }
            .filter { it.isNotBlank() }
            .joinToString(" ") { "$it*" }
        if (ftsQuery.isBlank()) return@withContext emptyList()

        db.rawQuery(
            """
            SELECT $COL_MESSAGE_CONVERSATION_ID,
                   $COL_MESSAGE_ID,
                   $COL_MESSAGE_ROLE,
                   snippet($TABLE_MESSAGES_FTS, '', '', '...', -1, 12),
                   $COL_MESSAGE_TIMESTAMP
            FROM $TABLE_MESSAGES_FTS
            WHERE $TABLE_MESSAGES_FTS MATCH ?
            ORDER BY $COL_MESSAGE_TIMESTAMP DESC
            LIMIT 50
            """.trimIndent(),
            arrayOf(ftsQuery)
        ).use { cursor ->
            val results = mutableListOf<ConversationSearchResult>()
            while (cursor.moveToNext()) {
                results += ConversationSearchResult(
                    conversationId = cursor.getLong(0),
                    messageId = cursor.getString(1),
                    role = MessageRole.valueOf(cursor.getString(2)),
                    snippet = cursor.getString(3),
                    timestamp = cursor.getLong(4)
                )
            }
            results
        }
    }

    suspend fun exportAllConversationsAsText(): String = withContext(Dispatchers.IO) {
        val summaries = loadConversationSummaries().sortedBy { it.updatedAt }
        buildString {
            summaries.forEach { summary ->
                appendLine("# ${summary.title}")
                loadConversationMessages(summary.id).forEach { message ->
                    appendLine("${message.role.name.lowercase()}: ${message.content}")
                }
                appendLine()
            }
        }.trim()
    }

    suspend fun exportAllConversationsAsJson(): String = withContext(Dispatchers.IO) {
        val conversations = JSONArray()
        loadConversationSummaries().forEach { summary ->
            val messages = JSONArray()
            loadConversationMessages(summary.id).forEach { message ->
                messages.put(
                    JSONObject()
                        .put("id", message.id)
                        .put("role", message.role.name)
                        .put("content", message.content)
                        .put("timestamp", message.timestamp)
                        .put("type", message.type.name)
                        .put("mediaUri", message.mediaUri)
                        .put("mimeType", message.mimeType)
                        .put("durationMillis", message.durationMillis)
                )
            }
            conversations.put(
                JSONObject()
                    .put("id", summary.id)
                    .put("title", summary.title)
                    .put("updatedAt", summary.updatedAt)
                    .put("messages", messages)
            )
        }
        JSONObject().put("conversations", conversations).toString(2)
    }

    private fun loadConversationSummaries(): List<ConversationSummary> {
        val db = dbHelper.readableDatabase
        val query = """
            SELECT c.$COL_CONVERSATION_ID,
                   COALESCE(c.$COL_CONVERSATION_TITLE, 'New Chat'),
                   COALESCE(
                       (SELECT
                            CASE
                                WHEN m.$COL_MESSAGE_TYPE = '${MessageType.IMAGE.name}' THEN '[Image] ' || m.$COL_MESSAGE_CONTENT
                                WHEN m.$COL_MESSAGE_TYPE = '${MessageType.AUDIO.name}' THEN '[Audio] ' || m.$COL_MESSAGE_CONTENT
                                ELSE m.$COL_MESSAGE_CONTENT
                            END
                        FROM $TABLE_MESSAGES m
                        WHERE m.$COL_MESSAGE_CONVERSATION_ID = c.$COL_CONVERSATION_ID
                        ORDER BY m.$COL_MESSAGE_TIMESTAMP DESC LIMIT 1),
                       ''
                   ),
                   c.$COL_CONVERSATION_UPDATED_AT,
                   (SELECT COUNT(*) FROM $TABLE_MESSAGES m2
                    WHERE m2.$COL_MESSAGE_CONVERSATION_ID = c.$COL_CONVERSATION_ID)
            FROM $TABLE_CONVERSATIONS c
            ORDER BY c.$COL_CONVERSATION_UPDATED_AT DESC
        """.trimIndent()

        return db.rawQuery(query, null).use { cursor ->
            val items = mutableListOf<ConversationSummary>()
            while (cursor.moveToNext()) {
                items += ConversationSummary(
                    id = cursor.getLong(0),
                    title = cursor.getString(1),
                    preview = cursor.getString(2),
                    updatedAt = cursor.getLong(3),
                    messageCount = cursor.getInt(4)
                )
            }
            items
        }
    }

    private fun loadProfileStats(): ProfileStats {
        val db = dbHelper.readableDatabase
        val conversations = singleIntQuery(db, "SELECT COUNT(*) FROM $TABLE_CONVERSATIONS")
        val totalMessages = singleIntQuery(db, "SELECT COUNT(*) FROM $TABLE_MESSAGES")
        val userMessages = singleIntQuery(
            db,
            "SELECT COUNT(*) FROM $TABLE_MESSAGES WHERE $COL_MESSAGE_ROLE = ?",
            arrayOf(MessageRole.USER.name)
        )
        val aiMessages = singleIntQuery(
            db,
            "SELECT COUNT(*) FROM $TABLE_MESSAGES WHERE $COL_MESSAGE_ROLE = ?",
            arrayOf(MessageRole.AI.name)
        )
        return ProfileStats(
            conversationCount = conversations,
            messageCount = totalMessages,
            userMessages = userMessages,
            aiMessages = aiMessages
        )
    }

    private fun singleIntQuery(db: SQLiteDatabase, query: String, args: Array<String>? = null): Int {
        return db.rawQuery(query, args).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    private fun latestConversationId(): Long? {
        val db = dbHelper.readableDatabase
        return db.query(
            TABLE_CONVERSATIONS,
            arrayOf(COL_CONVERSATION_ID),
            null,
            null,
            null,
            null,
            "$COL_CONVERSATION_UPDATED_AT DESC",
            "1"
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
    }

    private fun conversationExists(conversationId: Long): Boolean {
        val db = dbHelper.readableDatabase
        return db.query(
            TABLE_CONVERSATIONS,
            arrayOf(COL_CONVERSATION_ID),
            "$COL_CONVERSATION_ID = ?",
            arrayOf(conversationId.toString()),
            null,
            null,
            null,
            "1"
        ).use { it.moveToFirst() }
    }

    private fun createConversation(title: String? = null): Long {
        val now = System.currentTimeMillis()
        val db = dbHelper.writableDatabase
        return db.insert(
            TABLE_CONVERSATIONS,
            null,
            ContentValues().apply {
                put(COL_CONVERSATION_TITLE, title)
                put(COL_CONVERSATION_CREATED_AT, now)
                put(COL_CONVERSATION_UPDATED_AT, now)
            }
        )
    }

    private fun touchConversation(db: SQLiteDatabase, conversationId: Long, message: ChatMessage) {
        val values = ContentValues().apply {
            put(COL_CONVERSATION_UPDATED_AT, message.timestamp)
            if (message.role == MessageRole.USER) {
                val title = message.content.trim().take(60)
                if (title.isNotEmpty()) {
                    put(COL_CONVERSATION_TITLE, title)
                }
            }
        }
        db.update(
            TABLE_CONVERSATIONS,
            values,
            "$COL_CONVERSATION_ID = ?",
            arrayOf(conversationId.toString())
        )
    }

    private fun notifyChange() {
        changeCounter.value += 1
    }

    private class ChatDatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE $TABLE_CONVERSATIONS (
                    $COL_CONVERSATION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_CONVERSATION_TITLE TEXT,
                    $COL_CONVERSATION_CREATED_AT INTEGER NOT NULL,
                    $COL_CONVERSATION_UPDATED_AT INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE $TABLE_MESSAGES (
                    $COL_MESSAGE_ID TEXT PRIMARY KEY,
                    $COL_MESSAGE_CONVERSATION_ID INTEGER NOT NULL,
                    $COL_MESSAGE_ROLE TEXT NOT NULL,
                    $COL_MESSAGE_CONTENT TEXT NOT NULL,
                    $COL_MESSAGE_TIMESTAMP INTEGER NOT NULL,
                    $COL_MESSAGE_IS_STREAMING INTEGER NOT NULL DEFAULT 0,
                    $COL_MESSAGE_TYPE TEXT NOT NULL DEFAULT '${MessageType.TEXT.name}',
                    $COL_MESSAGE_MEDIA_URI TEXT,
                    $COL_MESSAGE_MIME_TYPE TEXT,
                    $COL_MESSAGE_DURATION_MILLIS INTEGER,
                    FOREIGN KEY($COL_MESSAGE_CONVERSATION_ID) REFERENCES $TABLE_CONVERSATIONS($COL_CONVERSATION_ID) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX idx_messages_conversation_time ON $TABLE_MESSAGES($COL_MESSAGE_CONVERSATION_ID, $COL_MESSAGE_TIMESTAMP)"
            )
            createFtsTable(db)

            val now = System.currentTimeMillis()
            db.insert(
                TABLE_CONVERSATIONS,
                null,
                ContentValues().apply {
                    put(COL_CONVERSATION_TITLE, "New Chat")
                    put(COL_CONVERSATION_CREATED_AT, now)
                    put(COL_CONVERSATION_UPDATED_AT, now)
                }
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE $TABLE_MESSAGES ADD COLUMN $COL_MESSAGE_TYPE TEXT NOT NULL DEFAULT '${MessageType.TEXT.name}'")
                db.execSQL("ALTER TABLE $TABLE_MESSAGES ADD COLUMN $COL_MESSAGE_MEDIA_URI TEXT")
                db.execSQL("ALTER TABLE $TABLE_MESSAGES ADD COLUMN $COL_MESSAGE_MIME_TYPE TEXT")
                db.execSQL("ALTER TABLE $TABLE_MESSAGES ADD COLUMN $COL_MESSAGE_DURATION_MILLIS INTEGER")
            }
            if (oldVersion < 3) {
                createFtsTable(db)
                db.execSQL(
                    """
                    INSERT INTO $TABLE_MESSAGES_FTS(
                        $COL_MESSAGE_ID,
                        $COL_MESSAGE_CONVERSATION_ID,
                        $COL_MESSAGE_ROLE,
                        $COL_MESSAGE_CONTENT,
                        $COL_MESSAGE_TIMESTAMP
                    )
                    SELECT $COL_MESSAGE_ID,
                           $COL_MESSAGE_CONVERSATION_ID,
                           $COL_MESSAGE_ROLE,
                           $COL_MESSAGE_CONTENT,
                           $COL_MESSAGE_TIMESTAMP
                    FROM $TABLE_MESSAGES
                    """.trimIndent()
                )
            }
        }

        private fun createFtsTable(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS $TABLE_MESSAGES_FTS USING fts4(
                    $COL_MESSAGE_ID,
                    $COL_MESSAGE_CONVERSATION_ID,
                    $COL_MESSAGE_ROLE,
                    $COL_MESSAGE_CONTENT,
                    $COL_MESSAGE_TIMESTAMP,
                    notindexed=$COL_MESSAGE_ID,
                    notindexed=$COL_MESSAGE_CONVERSATION_ID,
                    notindexed=$COL_MESSAGE_ROLE,
                    notindexed=$COL_MESSAGE_TIMESTAMP
                )
                """.trimIndent()
            )
        }
    }

    private fun upsertMessageSearchIndex(db: SQLiteDatabase, conversationId: Long, message: ChatMessage) {
        db.delete(TABLE_MESSAGES_FTS, "$COL_MESSAGE_ID = ?", arrayOf(message.id))
        db.insert(
            TABLE_MESSAGES_FTS,
            null,
            ContentValues().apply {
                put(COL_MESSAGE_ID, message.id)
                put(COL_MESSAGE_CONVERSATION_ID, conversationId)
                put(COL_MESSAGE_ROLE, message.role.name)
                put(COL_MESSAGE_CONTENT, message.content)
                put(COL_MESSAGE_TIMESTAMP, message.timestamp)
            }
        )
    }

    private companion object {
        const val DATABASE_NAME = "chat_local.db"
        const val DATABASE_VERSION = 3

        const val TABLE_CONVERSATIONS = "conversations"
        const val TABLE_MESSAGES = "messages"
        const val TABLE_MESSAGES_FTS = "messages_fts"

        const val COL_CONVERSATION_ID = "id"
        const val COL_CONVERSATION_TITLE = "title"
        const val COL_CONVERSATION_CREATED_AT = "created_at"
        const val COL_CONVERSATION_UPDATED_AT = "updated_at"

        const val COL_MESSAGE_ID = "id"
        const val COL_MESSAGE_CONVERSATION_ID = "conversation_id"
        const val COL_MESSAGE_ROLE = "role"
        const val COL_MESSAGE_CONTENT = "content"
        const val COL_MESSAGE_TIMESTAMP = "timestamp"
        const val COL_MESSAGE_IS_STREAMING = "is_streaming"
        const val COL_MESSAGE_TYPE = "message_type"
        const val COL_MESSAGE_MEDIA_URI = "media_uri"
        const val COL_MESSAGE_MIME_TYPE = "mime_type"
        const val COL_MESSAGE_DURATION_MILLIS = "duration_millis"
    }
}

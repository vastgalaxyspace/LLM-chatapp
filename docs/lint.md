# Lint policy

Android lint runs in CI for every change. Production-code warnings are fixed rather than baselined. Version-availability notices are reviewed separately because the vendored llama.cpp runtime is intentionally pinned (see app/src/main/cpp/LLAMA_CPP_VERSION.txt) for model/runtime compatibility and unrelated dependency upgrades are outside this stabilization pass.

Known Gradle migration notices (`android.builtInKotlin=false`, legacy Android DSL, and Kapt K1 mode) are retained temporarily because AGP 9 built-in Kotlin migration must be coordinated with Hilt annotation processing. They do not affect the current JDK 21 build and should be removed in a dedicated toolchain upgrade.

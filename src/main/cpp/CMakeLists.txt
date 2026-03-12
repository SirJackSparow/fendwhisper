cmake_minimum_required(VERSION 3.22.1)
project("whisper_android")

# ===== WHISPER DIR =====
set(WHISPER_DIR "../../../whisper.cpp")
get_filename_component(WHISPER_DIR ${WHISPER_DIR} ABSOLUTE)

# ===== SOURCES =====
set(SOURCE_FILES
        ${WHISPER_DIR}/whisper.cpp
        ${WHISPER_DIR}/ggml/src/ggml.c
        ${WHISPER_DIR}/ggml/src/ggml-alloc.c
        ${WHISPER_DIR}/ggml/src/ggml-backend.cpp
        ${WHISPER_DIR}/ggml/src/ggml-backend-reg.cpp
        ${WHISPER_DIR}/ggml/src/ggml-quants.c
        ${WHISPER_DIR}/ggml/src/ggml-threading.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu.c
        ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu-aarch64.cpp
)

# ===== BUILD WHISPER STATIC LIBRARY =====
add_library(whisper STATIC ${SOURCE_FILES})

target_include_directories(whisper PUBLIC
        ${WHISPER_DIR}
        ${WHISPER_DIR}/ggml/include
        ${WHISPER_DIR}/ggml/src
)

# ===== ABI OPTIMIZATIONS =====
if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    message(STATUS "Building for arm64-v8a with FP16 + dotprod")
    target_compile_options(whisper PRIVATE
            -march=armv8.2-a+fp16+dotprod
            -O3
            -ffast-math
    )
elseif(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    message(STATUS "Building for armeabi-v7a with NEON")
    target_compile_options(whisper PRIVATE
            -mfpu=neon
            -O3
    )
else()
    message(STATUS "Building for ${ANDROID_ABI}")
    target_compile_options(whisper PRIVATE -O3)
endif()

# ===== C/C++ STANDARD =====
set_source_files_properties(
        ${WHISPER_DIR}/ggml/src/ggml.c
        ${WHISPER_DIR}/ggml/src/ggml-alloc.c
        ${WHISPER_DIR}/ggml/src/ggml-quants.c
        ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu.c
        PROPERTIES COMPILE_FLAGS "-std=c11"
)

set_source_files_properties(
        ${WHISPER_DIR}/whisper.cpp
        ${WHISPER_DIR}/ggml/src/ggml-backend.cpp
        ${WHISPER_DIR}/ggml/src/ggml-backend-reg.cpp
        ${WHISPER_DIR}/ggml/src/ggml-threading.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu-aarch64.cpp
        PROPERTIES COMPILE_FLAGS "-std=c++17"
)

# ===== JNI BRIDGE =====
add_library(whisper_android SHARED whisper_jni.cpp)

target_include_directories(whisper_android PRIVATE
        ${WHISPER_DIR}
        ${WHISPER_DIR}/ggml/include
)

target_compile_options(whisper_android PRIVATE
        -std=c++17
        -O3
)

target_link_libraries(whisper_android
        whisper
        android
        log
)
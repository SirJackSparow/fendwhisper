cmake_minimum_required(VERSION 3.22.1)
project("whisper_android")

# ===== WHISPER DIR =====
set(WHISPER_DIR "../../../whisper.cpp")
get_filename_component(WHISPER_DIR ${WHISPER_DIR} ABSOLUTE)

# ===== SOURCES =====
set(SOURCE_FILES
        ${WHISPER_DIR}/src/whisper.cpp
        ${WHISPER_DIR}/ggml/src/ggml.c
        ${WHISPER_DIR}/ggml/src/ggml.cpp
        ${WHISPER_DIR}/ggml/src/ggml-alloc.c
        ${WHISPER_DIR}/ggml/src/ggml-backend.cpp
        ${WHISPER_DIR}/ggml/src/ggml-backend-dl.cpp
        ${WHISPER_DIR}/ggml/src/ggml-backend-reg.cpp
        ${WHISPER_DIR}/ggml/src/ggml-opt.cpp
        ${WHISPER_DIR}/ggml/src/ggml-quants.c
        ${WHISPER_DIR}/ggml/src/gguf.cpp
        ${WHISPER_DIR}/ggml/src/ggml-threading.cpp

        # New decoupled GGML CPU backend
        ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu.c
        ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/quants.c
        ${WHISPER_DIR}/ggml/src/ggml-cpu/repack.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/traits.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/binary-ops.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/unary-ops.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/vec.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/ops.cpp

        whisper_wrapper.cpp
)

if(${ANDROID_ABI} MATCHES "arm")
    list(APPEND SOURCE_FILES
            ${WHISPER_DIR}/ggml/src/ggml-cpu/arch/arm/quants.c
            ${WHISPER_DIR}/ggml/src/ggml-cpu/arch/arm/repack.cpp
            ${WHISPER_DIR}/ggml/src/ggml-cpu/arch/arm/cpu-feats.cpp
    )
elseif(${ANDROID_ABI} MATCHES "x86")
    list(APPEND SOURCE_FILES
            ${WHISPER_DIR}/ggml/src/ggml-cpu/arch/x86/quants.c
            ${WHISPER_DIR}/ggml/src/ggml-cpu/arch/x86/repack.cpp
            ${WHISPER_DIR}/ggml/src/ggml-cpu/arch/x86/cpu-feats.cpp
    )
endif()

# ===== BUILD WHISPER STATIC LIBRARY =====
add_library(whisper STATIC ${SOURCE_FILES})

target_compile_definitions(whisper PRIVATE
    GGML_COMMIT="unknown"
    GGML_VERSION="0.9.7"
    WHISPER_VERSION="1.8.3"
    GGML_USE_CPU
)

target_include_directories(whisper PUBLIC
        ${WHISPER_DIR}/include
        ${WHISPER_DIR}/src
        ${WHISPER_DIR}/ggml/include
        ${WHISPER_DIR}/ggml/src
        ${WHISPER_DIR}/ggml/src/ggml-cpu
)

# ===== ABI OPTIMIZATIONS =====
if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    message(STATUS "Building for arm64-v8a with FP16 + dotprod")
    target_compile_options(whisper PRIVATE
            -march=armv8.2-a+fp16+dotprod
            -O3
            -ffast-math
            -fno-finite-math-only
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
        ${WHISPER_DIR}/ggml/src/ggml-cpu/quants.c
        ${WHISPER_DIR}/ggml/src/ggml-cpu/arch/arm/quants.c
        ${WHISPER_DIR}/ggml/src/ggml-cpu/arch/x86/quants.c
        PROPERTIES COMPILE_FLAGS "-std=c11"
)

set_source_files_properties(
        ${WHISPER_DIR}/src/whisper.cpp
        ${WHISPER_DIR}/ggml/src/ggml.cpp
        ${WHISPER_DIR}/ggml/src/ggml-backend.cpp
        ${WHISPER_DIR}/ggml/src/ggml-backend-dl.cpp
        ${WHISPER_DIR}/ggml/src/ggml-backend-reg.cpp
        ${WHISPER_DIR}/ggml/src/ggml-opt.cpp
        ${WHISPER_DIR}/ggml/src/gguf.cpp
        ${WHISPER_DIR}/ggml/src/ggml-threading.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/ggml-cpu.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/traits.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/ops.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/vec.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/binary-ops.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/unary-ops.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/repack.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/arch/arm/repack.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/arch/arm/cpu-feats.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/arch/x86/repack.cpp
        ${WHISPER_DIR}/ggml/src/ggml-cpu/arch/x86/cpu-feats.cpp
        whisper_wrapper.cpp
        PROPERTIES COMPILE_FLAGS "-std=c++17"
)

# ===== JNI BRIDGE =====
add_library(whisper_android SHARED whisper_jni.cpp)

target_link_options(whisper_android PRIVATE
        "-Wl,-z,max-page-size=16384"
        "-Wl,-z,common-page-size=16384"
)

target_include_directories(whisper_android PRIVATE
        ${WHISPER_DIR}/include
        ${WHISPER_DIR}/src
        ${WHISPER_DIR}/ggml/include
)

target_compile_options(whisper_android PRIVATE
        -std=c++17
        -O3
        -DGGML_USE_CPU
)

target_link_libraries(whisper_android
        whisper
        android
        log
        dl
        m
        atomic
)

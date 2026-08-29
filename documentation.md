# WhisperFend Technical Documentation

## Overview
WhisperFend is an Android library providing a high-level Kotlin API for `whisper.cpp`. It handles JNI interactions, native memory management, and model loading via a state-driven approach.

## Architecture
The library follows a facade pattern via the `WhisperFend` object.

### Components
1. **WhisperFend**: Public entry point. Exposes a `StateFlow<WhisperState>` for lifecycle monitoring.
2. **WhisperState**: A sealed class representing the engine status:
   - `Idle`: Engine is uninitialized.
   - `Loading`: Copying or loading the model file.
   - `Ready`: Model is resident in memory and ready for transcription.
   - `Transcribing`: Native `full_transcribe` is running.
   - `Error`: Holds failure details.
3. **WhisperManager (Internal)**: Manages the `WhisperEngine` and file validation.
4. **WhisperEngine (Internal)**: Low-level JNI bridge.

## JNI Details
The native library `whisper_android` is loaded statically. It wraps `whisper.cpp` to provide:
- Context initialization (`initContext`).
- Transcription (`fullTranscribe`).
- Text segment retrieval (`getTextSegment`).

## Resource Management
Native resources are managed through `nativePtr`. The `release()` method must be called to free native memory (GGML contexts and Whisper states) when the library is no longer needed.

## Performance Tuning
- **Threads**: The `transcribe` method accepts `numThreads`. Recommended value is `4` for most modern Android devices.
- **Model Size**: Use GGUF format models. `tiny` or `base` models are recommended for mobile to maintain low latency and memory footprint.

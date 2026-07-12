# WhisperFend Android Integration Guide

This guide explains how to integrate and use the `whisperfend` module in your Android application for high-performance, on-device speech-to-text.

## 1. Setup

### Add Dependency
In your app's `build.gradle.kts`, add the `whisperfend` module:

```kotlin
dependencies {
    implementation(project(":whisperfend"))
}
```

## 2. Usage

### Upload and Load Model
`MainWhisperFend` handles copying the model from a URI (e.g., from a file picker) to internal storage and initializing the engine.

```kotlin
// Example using a file picker URI
MainWhisperFend.uploadModelFile(uri, context, 
    onSuccess = { message ->
        Log.d("Whisper", "Model loaded: $message")
    },
    onError = { isRetryable, error ->
        Log.e("Whisper", "Load failed: $error")
    }
)
```

### Transcribe Audio
Whisper requires **16kHz Mono 16-bit PCM** audio data. The API expects a `FloatArray` where each value is between `-1.0f` and `1.0f`.

```kotlin
val audioData: FloatArray = // ... your 16kHz audio data
val result = MainWhisperFend.transcribe(audioData, numThreads = 4)

if (result != null) {
    Log.d("Whisper", "Transcription: $result")
}
```

### Resource Management
Always release the native resources when the engine is no longer needed (e.g., in `onCleared()` of a ViewModel).

```kotlin
MainWhisperFend.release()
```

## 3. Implementation Details

- **Architecture**: Follows the same pattern as the `fendai` module.
- **Native Stack**: Uses a C++ wrapper (`WhisperWrapper`) for memory safety and efficient context management.
- **ABIs Supported**: `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`.
- **Optimization**: Includes `-fno-finite-math-only` for full compatibility with modern `ggml` backends on Android.

## 4. Tips for Success
1. **Audio Format**: Ensure your audio is exactly **16,000Hz**. Whisper will produce garbage text or fail if the sample rate is incorrect.
2. **Model Choice**: Use `base` or `small` models for the best balance of speed and accuracy on mobile devices.
3. **Threading**: Setting `numThreads` to match the number of high-performance CPU cores (usually 4 on modern phones) provides the best results.

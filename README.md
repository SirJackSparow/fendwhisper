# 🎙️ WhisperFend

A professional, high-level Kotlin wrapper for **whisper.cpp** on Android. It provides a state-driven API, handles JNI lifecycle management, and simplifies model loading for high-performance on-device Speech-to-Text.

---

## 📦 Setup

Add the following to your app's `build.gradle.kts`:

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/SirJackSparow/fendwhisper")
        credentials {
            username = project.findProperty("gpr.user") as String?
            password = project.findProperty("gpr.key") as String?
        }
    }
}

dependencies {
    implementation("com.dg.whisperfend:whisperfend:1.0.x")
}
```

---

## 🚀 Quick Start

### 1. Load a Model
Use a GGUF/BIN model file. The library validates the file signature, handles background copying to internal storage, and initializes the native engine.

```kotlin
import com.dg.whisperfend.WhisperFend

WhisperFend.instance.loadModel(
    uri = modelUri,
    context = context,
    onSuccess = { message -> Log.d("Whisper", "✅ $message") },
    onError = { isValidation, error -> Log.e("Whisper", "❌ $error") }
)
```

### 2. Monitor Engine State
Observe the `state` flow to react to engine status changes (Idle, Loading, Ready, Transcribing, Error).

```kotlin
lifecycleScope.launch {
    WhisperFend.instance.state.collect { state ->
        when (state) {
            is WhisperState.Loading -> showLoadingSpinner()
            is WhisperState.Ready -> enableTranscribeButton()
            is WhisperState.Transcribing -> showProcessingWaveform()
            is WhisperState.Error -> showError(state.message)
            else -> Unit
        }
    }
}
```

### 3. Transcribe Audio
Transcription is a suspending function that accepts a `FloatArray` (PCM 16kHz mono, normalized -1.0 to 1.0) and returns a `Result<String>`.

```kotlin
val audioData: FloatArray = // PCM 16kHz mono normalized data
viewModelScope.launch {
    val result = WhisperFend.instance.transcribe(audioData, numThreads = 4)
    result.onSuccess { text ->
        println("Transcription: $text")
    }.onFailure { e ->
        println("Failed: ${e.message}")
    }
}
```

---

## 🛠️ Audio Format Requirements

Audio must be **16kHz mono PCM** as a `FloatArray` normalized to **[-1.0, 1.0]**.

### Conversion Examples:

```kotlin
// ShortArray (16-bit PCM) → FloatArray
val floats = FloatArray(shorts.size) { i -> shorts[i] / 32768.0f }

// ByteArray (16-bit PCM, little-endian) → FloatArray
val floats = FloatArray(bytes.size / 2) { i ->
    (bytes[i * 2].toInt() or (bytes[i * 2 + 1].toInt() shl 8)) / 32768.0f
}
```

---

## 🧹 Cleanup
Always release native resources when the engine is no longer needed (e.g., in `onCleared()` of a ViewModel).

```kotlin
WhisperFend.instance.release()
```

---

## 📖 Detailed Documentation
For more technical details, refer to:
- [Implementation Guide](implementation.md): How to build and integrate.
- [Technical Documentation](documentation.md): JNI bridge and native resource details.

## 📄 License
This library is provided under the **MIT License**.
Based on [whisper.cpp](https://github.com/ggerganov/whisper.cpp) by Georgi Gerganov.

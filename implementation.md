# WhisperFend Implementation Guide

## 1. Setup
Add the `:whisperfend` module as a dependency in your `build.gradle`:

```gradle
dependencies {
    implementation project(':whisperfend')
}
```

## 2. Initialize and Load Model
You must load a `.gguf` or `.bin` model file before transcribing. Access the engine via `WhisperFend.instance`.

```kotlin
WhisperFend.instance.loadModel(
    uri = modelUri,
    context = context,
    onSuccess = { msg -> Log.d("Whisper", msg) },
    onError = { isValidation, error -> Log.e("Whisper", error) }
)
```

## 3. Observe State
It is highly recommended to observe the `state` to update your UI.

```kotlin
lifecycleScope.launch {
    WhisperFend.instance.state.collect { state ->
        when (state) {
            is WhisperState.Loading -> showProgress()
            is WhisperState.Ready -> enableTranscribeButton()
            is WhisperState.Error -> showError(state.message)
            // ...
        }
    }
}
```

## 4. Transcribe
Transcription is a suspending function that returns a `Result<String>`.

```kotlin
val audioData: FloatArray = // PCM 16kHz mono audio
viewModelScope.launch {
    val result = WhisperFend.instance.transcribe(audioData, numThreads = 4)
    result.onSuccess { text ->
        println("Transcribed: $text")
    }.onFailure { e ->
        println("Error: ${e.message}")
    }
}
```

## 5. Cleanup
Always release resources when finished, typically in `onCleared()` of a ViewModel.

```kotlin
WhisperFend.instance.release()
```

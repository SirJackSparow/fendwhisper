# WhisperFend

## Setup

Add to your app's `build.gradle.kts`:

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

## How to use

### 1. Upload a model

After downloading a GGUF model, use `uploadModelFile()` to copy it into the app and load it:

```kotlin
import com.dg.whisperfend.implement.MainWhisperFend

MainWhisperFend.uploadModelFile(modelUri, context,
    onSuccess = { message ->
        Log.d("Whisper", message)
    },
    onError = { isValidationError, errorMessage ->
        Log.e("Whisper", "Error (validation=$isValidationError): $errorMessage")
    }
)
```

This checks the file header (GGUF/GGML magic bytes), copies it to internal storage, and initializes the engine. If the same file already exists, it skips the copy.

### 2. Transcribe audio

Pass raw PCM audio as `FloatArray` (16kHz mono, normalized -1.0 to 1.0):

```kotlin
val text = MainWhisperFend.transcribe(audioFloats)
if (text != null) {
    // transcription
} else {
    // no speech detected or model not loaded
}
```

Thread note: whisper.cpp is not thread-safe, so run one transcription at a time.

### 3. Release

```kotlin
MainWhisperFend.release()
```

Frees native memory. Call when leaving the screen or closing the app.

## Audio format

Audio needs to be **16kHz mono PCM** as `FloatArray` in **[-1.0, 1.0]**.

### Recording from microphone

```kotlin
// Record: 16kHz, mono, 16-bit PCM
val recorder = AudioRecord(
    MediaRecorder.AudioSource.MIC,
    16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
    bufferSize
)
recorder.startRecording()

// Collect chunks
val shortBuffer = mutableListOf<Short>()
val chunk = ShortArray(1024)
while (isRecording) {
    val read = recorder.read(chunk, 0, chunk.size)
    if (read > 0) for (i in 0 until read) shortBuffer.add(chunk[i])
}
recorder.stop()
recorder.release()

// Convert to normalized FloatArray
val audio = FloatArray(shortBuffer.size) { i -> shortBuffer[i] / 32768.0f }
val text = MainWhisperFend.transcribe(audio)
```

### Converting existing audio

```kotlin
// ShortArray → FloatArray
val floats = FloatArray(shorts.size) { i -> shorts[i] / 32768.0f }

// ByteArray (16-bit PCM, little-endian) → FloatArray
val floats = FloatArray(bytes.size / 2) { i ->
    (bytes[i * 2].toInt() or (bytes[i * 2 + 1].toInt() shl 8)) / 32768.0f
}
```

## Example

```kotlin
class MyViewModel(private val context: Context) : ViewModel() {

    private val _isModelLoaded = MutableStateFlow(false)
    private val _isTranscribing = MutableStateFlow(false)

    fun loadModel(uri: Uri) {
        MainWhisperFend.uploadModelFile(uri, context,
            onSuccess = { _isModelLoaded.value = true },
            onError = { _, msg -> Log.e("Whisper", msg) }
        )
    }

    suspend fun transcribe(audio: FloatArray): String? {
        if (!_isModelLoaded.value || _isTranscribing.value) return null
        _isTranscribing.value = true
        return try {
            MainWhisperFend.transcribe(audio)
        } finally {
            _isTranscribing.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        MainWhisperFend.release()
    }
}
```

## Model files

Works with **GGUF** and **GGML** models from [whisper.cpp](https://huggingface.co/ggerganov/whisper.cpp).

`ggml-tiny.gguf` (~75 MB) is a good starting point.

```kotlin
val request = DownloadManager.Request(
    "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.gguf".toUri()
).apply {
    setTitle("Downloading Whisper model")
    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "ggml-tiny.gguf")
}
downloadManager.enqueue(request)
```
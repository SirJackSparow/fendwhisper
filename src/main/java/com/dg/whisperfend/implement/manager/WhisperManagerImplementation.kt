package com.dg.whisperfend.implement.manager

import android.content.Context
import android.net.Uri
import com.dg.whisperfend.core.WhisperEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class WhisperManagerImplementation : WhisperManager {

    private val engine = WhisperEngine()
    private var modelInitJob: Job? = null
    private var isInstanceLoaded = false

    override suspend fun transcribe(audioData: FloatArray, numThreads: Int): String? {
        return engine.transcribe(audioData, numThreads)
    }

    override fun loadModel(
        modelPath: String,
        onError: (Exception) -> Unit,
        onSuccess: (Boolean) -> Unit
    ) {
        modelInitJob?.cancel()
        modelInitJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isInstanceLoaded) {
                    engine.release()
                    isInstanceLoaded = false
                }

                val success = engine.loadModel(modelPath)

                withContext(Dispatchers.Main) {
                    isInstanceLoaded = success
                    onSuccess(success)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    override fun release() {
        engine.release()
        isInstanceLoaded = false
        modelInitJob?.cancel()
    }

    override fun checkGGUFFile(uri: Uri, context: Context): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val header = ByteArray(16)
                val read = inputStream.read(header)
                if (read < 4) {
                    android.util.Log.e("WHISPER_ERROR", "❌ FILE TOO SMALL: Only $read bytes read")
                    return false
                }

                // GGML Magic Number: 0x67676d6c
                // On disk (Little Endian): 6c 6d 67 67 ('lmgg')
                // On disk (Big Endian):    67 67 6d 6c ('ggml')
                
                val isGGML_LE = header[0] == 0x6c.toByte() && header[1] == 0x6d.toByte() &&
                                header[2] == 0x67.toByte() && header[3] == 0x67.toByte()
                                
                val isGGML_BE = header[0] == 0x67.toByte() && header[1] == 0x67.toByte() &&
                                header[2] == 0x6d.toByte() && header[3] == 0x6c.toByte()

                val isGGUF = header[0] == 0x47.toByte() && header[1] == 0x47.toByte() &&
                             header[2] == 0x55.toByte() && header[3] == 0x46.toByte()

                val isValid = isGGML_LE || isGGML_BE || isGGUF

                if (!isValid) {
                    val hexDump = header.sliceArray(0 until read).joinToString(" ") { "%02x".format(it) }
                    android.util.Log.e("WHISPER_ERROR", "❌ INVALID MODEL SIGNATURE: $hexDump")
                } else {
                    android.util.Log.d("WHISPER_SUCCESS", "✅ Valid Whisper model detected")
                }

                isValid
            } ?: false
        } catch (e: Exception) {
            android.util.Log.e("WHISPER_ERROR", "❌ EXCEPTION DURING CHECK: ${e.message}")
            false
        }
    }
}

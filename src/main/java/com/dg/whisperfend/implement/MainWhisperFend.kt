package com.dg.whisperfend.implement

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.dg.whisperfend.implement.manager.WhisperManager
import com.dg.whisperfend.implement.manager.WhisperManagerImplementation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

object MainWhisperFend {

    private val whisperManager: WhisperManager = WhisperManagerImplementation()

    fun uploadModelFile(
        uri: Uri,
        context: Context,
        onSuccess: (String) -> Unit,
        onError: (Boolean, String) -> Unit
    ) {
        if (!whisperManager.checkGGUFFile(uri, context)) {
            onError(true, "File is not a valid Whisper model (GGUF/BIN)")
            return
        }

        var fileName = ""
        if (uri.scheme == "file") {
            fileName = uri.path?.let { File(it).name } ?: ""
        } else {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }

        if (fileName.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val destFile = File(context.filesDir, fileName)

                // Only copy if the file doesn't exist or is different in size
                var shouldCopy = true
                if (destFile.exists()) {
                    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                        if (afd.length == destFile.length()) {
                            shouldCopy = false
                        }
                    }
                }

                if (shouldCopy) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            FileOutputStream(destFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    } catch (e: Exception) {
                        launch(Dispatchers.Main) {
                            onError(true, "Failed to copy file: ${e.message}")
                        }
                        return@launch
                    }
                }

                whisperManager.loadModel(
                    modelPath = destFile.absolutePath,
                    onSuccess = { success ->
                        if (success) {
                            onSuccess("Model loaded successfully!")
                        } else {
                            onError(true, "Failed to load model into engine")
                        }
                    },
                    onError = { e ->
                        onError(true, e.message ?: "Unknown error loading model")
                    }
                )
            }
        } else {
            onError(true, "Could not determine file name")
        }
    }

    suspend fun transcribe(audioData: FloatArray, numThreads: Int = 4): String? {
        return whisperManager.transcribe(audioData, numThreads)
    }

    fun release() {
        whisperManager.release()
    }
}

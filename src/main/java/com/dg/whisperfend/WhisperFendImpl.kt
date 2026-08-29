package com.dg.whisperfend

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.dg.whisperfend.implement.manager.WhisperManager
import com.dg.whisperfend.implement.manager.WhisperManagerImplementation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

internal class WhisperFendImpl : WhisperFend {

    private val whisperManager: WhisperManager = WhisperManagerImplementation()
    
    private val _state = MutableStateFlow<WhisperState>(WhisperState.Idle)
    
    override val state: StateFlow<WhisperState> = _state.asStateFlow()

    override fun loadModel(
        uri: Uri,
        context: Context,
        onSuccess: (String) -> Unit,
        onError: (Boolean, String) -> Unit
    ) {
        _state.value = WhisperState.Loading
        
        if (!whisperManager.checkGGUFFile(uri, context)) {
            val errorMsg = "File is not a valid Whisper model (GGUF/BIN)"
            _state.value = WhisperState.Error(errorMsg)
            onError(true, errorMsg)
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
                        withContext(Dispatchers.Main) {
                            val errorMsg = "Failed to copy file: ${e.message}"
                            _state.value = WhisperState.Error(errorMsg)
                            onError(true, errorMsg)
                        }
                        return@launch
                    }
                }

                whisperManager.loadModel(
                    modelPath = destFile.absolutePath,
                    onSuccess = { success ->
                        CoroutineScope(Dispatchers.Main).launch {
                            if (success) {
                                _state.value = WhisperState.Ready
                                onSuccess("Model loaded successfully!")
                            } else {
                                val errorMsg = "Failed to load model into engine"
                                _state.value = WhisperState.Error(errorMsg)
                                onError(true, errorMsg)
                            }
                        }
                    },
                    onError = { e ->
                        CoroutineScope(Dispatchers.Main).launch {
                            val errorMsg = e.message ?: "Unknown error loading model"
                            _state.value = WhisperState.Error(errorMsg)
                            onError(true, errorMsg)
                        }
                    }
                )
            }
        } else {
            val errorMsg = "Could not determine file name"
            _state.value = WhisperState.Error(errorMsg)
            onError(true, errorMsg)
        }
    }

    override suspend fun transcribe(audioData: FloatArray, numThreads: Int): Result<String> {
        if (_state.value !is WhisperState.Ready) {
            return Result.failure(IllegalStateException("Engine is not ready. Current state: ${_state.value}"))
        }

        _state.value = WhisperState.Transcribing
        return try {
            val result = whisperManager.transcribe(audioData, numThreads)
            _state.value = WhisperState.Ready
            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("Transcription failed or returned no result"))
            }
        } catch (e: Exception) {
            _state.value = WhisperState.Ready
            Result.failure(e)
        }
    }

    override fun release() {
        whisperManager.release()
        _state.value = WhisperState.Idle
    }
}

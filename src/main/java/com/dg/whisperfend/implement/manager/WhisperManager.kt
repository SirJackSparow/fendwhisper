package com.dg.whisperfend.implement.manager

import android.content.Context
import android.net.Uri

interface WhisperManager {
    suspend fun transcribe(audioData: FloatArray, numThreads: Int = 4): String?
    fun loadModel(
        modelPath: String,
        onError: (Exception) -> Unit,
        onSuccess: (Boolean) -> Unit
    )
    fun release()
    fun checkGGUFFile(uri: Uri, context: Context): Boolean
}

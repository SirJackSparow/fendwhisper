package com.dg.whisperfend.core

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WhisperEngine {

    companion object {
        private const val TAG = "WhisperEngine"

        init {
            try {
                System.loadLibrary("whisper_android")
                Log.d(TAG, "Loaded whisper_android library")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load whisper_android library", e)
            }
        }
    }

    private var nativePtr: Long = 0

    suspend fun loadModel(modelPath: String): Boolean = withContext(Dispatchers.IO) {
        if (nativePtr != 0L) {
            freeContext(nativePtr)
        }
        nativePtr = initContext(modelPath)
        nativePtr != 0L
    }

    suspend fun transcribe(audioData: FloatArray, numThreads: Int = 4): String? = withContext(Dispatchers.IO) {
        if (nativePtr == 0L) return@withContext null

        val result = fullTranscribe(nativePtr, numThreads, audioData)
        if (result != 0) {
            Log.e(TAG, "Transcription failed with error code $result")
            return@withContext null
        }

        val segmentCount = getTextSegmentCount(nativePtr)
        val stringBuilder = StringBuilder()
        for (i in 0 until segmentCount) {
            stringBuilder.append(getTextSegment(nativePtr, i))
        }
        stringBuilder.toString()
    }

    fun release() {
        if (nativePtr != 0L) {
            freeContext(nativePtr)
            nativePtr = 0L
        }
    }

    fun getSystemInfo(): String {
        return getSystemInfoNative()
    }

    private external fun initContext(modelPath: String): Long
    private external fun freeContext(contextPtr: Long)
    private external fun fullTranscribe(contextPtr: Long, numThreads: Int, audioData: FloatArray): Int
    private external fun getTextSegmentCount(contextPtr: Long): Int
    private external fun getTextSegment(contextPtr: Long, index: Int): String
    private external fun getSystemInfoNative(): String
}

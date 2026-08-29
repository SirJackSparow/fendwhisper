package com.dg.whisperfend

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.StateFlow

/**
 * Main entry point for the WhisperFend library.
 * This interface manages the lifecycle of the Whisper engine and provides methods for model loading and transcription.
 * 
 * Access the singleton instance via [WhisperFend.instance].
 */
interface WhisperFend {

    /**
     * The current state of the Whisper engine.
     */
    val state: StateFlow<WhisperState>

    /**
     * Loads a Whisper model file from the provided [uri].
     * 
     * @param uri The URI of the model file (GGUF/BIN).
     * @param context Android context for content resolution and file storage.
     * @param onSuccess Callback triggered when the model is successfully loaded.
     * @param onError Callback triggered on error, providing a boolean flag if it was a validation error and a message.
     */
    fun loadModel(
        uri: Uri,
        context: Context,
        onSuccess: (String) -> Unit = {},
        onError: (Boolean, String) -> Unit = { _, _ -> }
    )

    /**
     * Transcribes audio data using the loaded Whisper model.
     * 
     * @param audioData FloatArray containing the PCM audio data (16kHz mono recommended).
     * @param numThreads Number of threads to use for transcription.
     * @return Result containing the transcribed text or an error.
     */
    suspend fun transcribe(audioData: FloatArray, numThreads: Int = 4): Result<String>

    /**
     * Releases native resources and resets the engine state.
     */
    fun release()

    companion object {
        /**
         * Returns the default implementation of [WhisperFend].
         */
        val instance: WhisperFend by lazy { WhisperFendImpl() }
    }
}

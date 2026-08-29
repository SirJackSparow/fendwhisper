package com.dg.whisperfend

sealed class WhisperState {
    object Idle : WhisperState()
    object Loading : WhisperState()
    object Ready : WhisperState()
    object Transcribing : WhisperState()
    data class Error(val message: String) : WhisperState()
}

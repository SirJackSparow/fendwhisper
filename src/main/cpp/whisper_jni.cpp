#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>
#include "whisper_wrapper.h"

#define TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_dg_whisperfend_core_WhisperEngine_initContext(
        JNIEnv *env, jobject thiz, jstring model_path_str) {
    const char *model_path_chars = env->GetStringUTFChars(model_path_str, nullptr);

    WhisperWrapper* wrapper = new WhisperWrapper();
    if (!wrapper->load_model(model_path_chars)) {
        delete wrapper;
        wrapper = nullptr;
    }

    env->ReleaseStringUTFChars(model_path_str, model_path_chars);
    return reinterpret_cast<jlong>(wrapper);
}

JNIEXPORT void JNICALL
Java_com_dg_whisperfend_core_WhisperEngine_freeContext(
        JNIEnv *env, jobject thiz, jlong context_ptr) {
    WhisperWrapper* wrapper = reinterpret_cast<WhisperWrapper*>(context_ptr);
    if (wrapper) {
        delete wrapper;
    }
}

JNIEXPORT jint JNICALL
Java_com_dg_whisperfend_core_WhisperEngine_fullTranscribe(
        JNIEnv *env, jobject thiz, jlong context_ptr, jint num_threads, jfloatArray audio_data) {
    WhisperWrapper* wrapper = reinterpret_cast<WhisperWrapper*>(context_ptr);
    if (!wrapper) return -1;

    jfloat *audio_data_arr = env->GetFloatArrayElements(audio_data, nullptr);
    const jsize audio_data_length = env->GetArrayLength(audio_data);

    int result = wrapper->full_transcribe(audio_data_arr, audio_data_length, num_threads);

    env->ReleaseFloatArrayElements(audio_data, audio_data_arr, JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_dg_whisperfend_core_WhisperEngine_getTextSegmentCount(
        JNIEnv *env, jobject thiz, jlong context_ptr) {
    WhisperWrapper* wrapper = reinterpret_cast<WhisperWrapper*>(context_ptr);
    return wrapper ? wrapper->get_segment_count() : 0;
}

JNIEXPORT jstring JNICALL
Java_com_dg_whisperfend_core_WhisperEngine_getTextSegment(
        JNIEnv *env, jobject thiz, jlong context_ptr, jint index) {
    WhisperWrapper* wrapper = reinterpret_cast<WhisperWrapper*>(context_ptr);
    if (!wrapper) return env->NewStringUTF("");

    const char *text = wrapper->get_segment_text(index);
    return env->NewStringUTF(text);
}

JNIEXPORT jstring JNICALL
Java_com_dg_whisperfend_core_WhisperEngine_getSystemInfoNative(
        JNIEnv *env, jobject thiz
) {
    WhisperWrapper wrapper; // Temporary for sys info
    return env->NewStringUTF(wrapper.get_system_info());
}

}

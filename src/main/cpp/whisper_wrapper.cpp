#include "whisper_wrapper.h"
#include <cstring>

WhisperWrapper::WhisperWrapper() {
}

WhisperWrapper::~WhisperWrapper() {
    if (ctx) {
        whisper_free(ctx);
        ctx = nullptr;
    }
}

bool WhisperWrapper::load_model(const char* model_path) {
    if (ctx) {
        whisper_free(ctx);
    }

    ctx = whisper_init_from_file_with_params(model_path, whisper_context_default_params());
    return ctx != nullptr;
}

int WhisperWrapper::full_transcribe(const float* audio_data, int n_samples, int n_threads) {
    if (!ctx) {
        return -1;
    }

    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.n_threads = n_threads;
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = true;
    params.print_special = false;
    params.translate = false;
    params.language = "auto";
    params.no_context = true;
    params.single_segment = false;

    return whisper_full(ctx, params, audio_data, n_samples);
}

int WhisperWrapper::get_segment_count() const {
    if (!ctx) return 0;
    return whisper_full_n_segments(ctx);
}

const char* WhisperWrapper::get_segment_text(int index) const {
    if (!ctx) return "";
    return whisper_full_get_segment_text(ctx, index);
}

int64_t WhisperWrapper::get_segment_t0(int index) const {
    if (!ctx) return 0;
    return whisper_full_get_segment_t0(ctx, index);
}

int64_t WhisperWrapper::get_segment_t1(int index) const {
    if (!ctx) return 0;
    return whisper_full_get_segment_t1(ctx, index);
}

const char* WhisperWrapper::get_system_info() const {
    return whisper_print_system_info();
}

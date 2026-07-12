#pragma once

#include "whisper.h"
#include <string>
#include <vector>

struct WhisperSegment {
    std::string text;
    int64_t t0;
    int64_t t1;
};

class WhisperWrapper {
public:
    WhisperWrapper();
    ~WhisperWrapper();

    bool load_model(const char* model_path);
    int full_transcribe(const float* audio_data, int n_samples, int n_threads);

    int get_segment_count() const;
    const char* get_segment_text(int index) const;
    int64_t get_segment_t0(int index) const;
    int64_t get_segment_t1(int index) const;

    const char* get_system_info() const;

private:
    struct whisper_context* ctx = nullptr;
};

# Protect the library's public API
-keep class com.dg.whisperfend.WhisperFend { *; }
-keep class com.dg.whisperfend.WhisperFend$Companion { *; }
-keep class com.dg.whisperfend.WhisperState { *; }
-keep class com.dg.whisperfend.WhisperState$** { *; }

# Protect JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep the implementation
-keep class com.dg.whisperfend.WhisperFendImpl { *; }

# Ignore missing platform classes
-dontwarn java.lang.invoke.StringConcatFactory

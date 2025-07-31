# Базовые оптимизации
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# Сохраняем только необходимое для MovieTorr
-keep class com.movietorr.MainActivity$KinopoiskInterface {
    public *;
}

# OkHttp (минимальные правила)
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Gson (минимальные правила)
-keepattributes Signature
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Основные классы приложения
-keep class com.movietorr.** {
    public protected *;
}

# Удаляем логирование в release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

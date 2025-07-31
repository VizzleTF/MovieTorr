# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
-keepclassmembers class com.movietorr.MainActivity$KinopoiskInterface {
   public *;
}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# MovieTorr specific rules
-keep class com.movietorr.** { *; }
-keepclassmembers class com.movietorr.** { *; }

# WebView JavaScript interface
-keepclassmembers class com.movietorr.MainActivity$KinopoiskInterface {
   public *;
}

# BottomSheet fragments
-keep class com.movietorr.SearchBottomSheet { *; }
-keep class com.movietorr.SettingsBottomSheet { *; }
-keep class com.movietorr.SiteBottomSheet { *; }

# Data classes
-keep class com.movietorr.SiteConfig { *; }
-keep class com.movietorr.TorrentService { *; }
-keep class com.movietorr.TorApiService { *; }

# RecyclerView adapters
-keep class com.movietorr.SitesAdapter { *; } 
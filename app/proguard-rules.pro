# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep OkHttp classes
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Keep Gson classes
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep AndroidX classes
-keep class androidx.** { *; }
-dontwarn androidx.**

# Keep your app's main classes
-keep class com.movietorr.** { *; }

# Keep all Activities
-keep public class * extends android.app.Activity
-keep public class * extends androidx.fragment.app.FragmentActivity
-keep public class * extends android.app.Application

# Keep AndroidManifest
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions

# Keep MainActivity specifically
-keep class com.movietorr.MainActivity { *; }

# Keep all View classes
-keep public class * extends android.view.View
-keep public class * extends android.view.ViewGroup

# Keep all Adapter classes
-keep public class * extends android.widget.BaseAdapter
-keep public class * extends androidx.recyclerview.widget.RecyclerView$Adapter

# Keep all Fragment classes
-keep public class * extends androidx.fragment.app.Fragment

# Keep all Service classes
-keep public class * extends android.app.Service

# Keep all BroadcastReceiver classes
-keep public class * extends android.content.BroadcastReceiver

# Keep all ContentProvider classes
-keep public class * extends android.content.ContentProvider

# Remove logging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

# Optimize
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification 
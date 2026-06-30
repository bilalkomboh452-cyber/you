
# WAGLO ProGuard rules

# Keep application class
-keep class com.waglo.app.WagloApp { *; }

# Keep all model classes (needed for Gson serialization)
-keep class com.waglo.app.model.** { *; }
-keepclassmembers class com.waglo.app.model.** { *; }

# Keep engine classes
-keep class com.waglo.app.engine.** { *; }

# Keep repository
-keep class com.waglo.app.repository.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Metadata { public <methods>; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**

# ViewBinding
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** inflate(android.view.LayoutInflater);
    public static *** inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
    public static *** bind(android.view.View);
}

# Keep accessibility service
-keep class com.waglo.app.service.WAAccessibilityService { *; }

# Database
-keep class com.waglo.app.database.** { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

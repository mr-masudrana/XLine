# Keep UpdateChecker and its data classes for Gson
-keep class com.ipdial.util.UpdateChecker** { *; }
-keep class com.ipdial.util.UpdateChecker$GitHubRelease { *; }

# Keep PJSIP JNI bridge
-keep class org.pjsip.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep our SIP service
-keep class com.ipdial.service.** { *; }
-keep class com.ipdial.data.model.** { *; }
-keep class com.ipdial.data.repository.** { *; }
-keep class com.ipdial.ui.** { *; }

# Firebase & Firestore
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.tasks.** { *; }
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.common.** { *; }
-keep class com.google.firebase.components.** { *; }
-keep class com.google.firebase.inject.** { *; }
-keep class com.google.firebase.installations.** { *; }
-keep class com.google.firebase.platforminfo.** { *; }
-keep class com.google.firebase.heartbeatinfo.** { *; }
-keep class com.google.firebase.tracing.** { *; }
-keep class com.google.firebase.analytics.** { *; }
-keep class com.google.firebase.crashlytics.** { *; }

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep class * implements androidx.room.RoomOpenHelper$Delegate

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.material.ripple.** { *; }
-keep class androidx.compose.material3.ripple.** { *; }
-keep interface androidx.compose.foundation.IndicationNodeFactory { *; }

# More aggressive shrinking - Disabled assumed side effects for logs to keep some debugging info if needed
#-assumenosideeffects class android.util.Log {
#    public static *** d(...);
#    public static *** v(...);
#    public static *** i(...);
#}

# Start.io Ads
-keep class com.startapp.** { *; }
-dontwarn com.startapp.**
-keep class com.startapp.sdk.** { *; }

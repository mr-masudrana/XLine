# Add project specific ProGuard rules here.

# PJSIP / pjsua2 JNI bindings — keep everything so JNI method names and
# native-facing classes aren't stripped or renamed (this WILL crash at
# runtime in release builds if minifyEnabled is turned on without these).
-keep class org.pjsip.pjsua2.** { *; }
-keepclassmembers class org.pjsip.pjsua2.** { *; }
-dontwarn org.pjsip.pjsua2.**

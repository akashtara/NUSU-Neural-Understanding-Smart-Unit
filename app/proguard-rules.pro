# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# -----------------
# Core Tree-Shaking & Optimization Settings
# -----------------

# Optimize the byte code down to the bare essentials
-optimizationpasses 5
-allowaccessmodification

# -----------------
# App-Specific Models (Tree-Shaking Safety First)
# -----------------
# Do not strip or obfuscate fields in our JSON models since Moshi requires serialization fields.
-keep class com.example.api.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Preserve the generated Moshi JSON adapters
-keep class *JsonAdapter { *; }
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# -----------------
# Network & Serialization Keeps (Retrofit & OkHttp)
# -----------------
# Keep annotations and signatures necessary for type-safety and Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod, AnnotationDefault, *Annotation*, MethodParameters

# Keep Retrofit interface methods intact
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# -----------------
# Jetpack Compose Optimization Rules
# -----------------
# Ensure state classes are not stripped or decoupled during tree-shaking
-keepclassmembers class * extends androidx.compose.runtime.snapshots.SnapshotStateList { *; }
-keepclassmembers class * extends androidx.compose.runtime.snapshots.SnapshotStateMap { *; }
-dontwarn androidx.compose.**

# -----------------
# Kotlin Coroutines
# -----------------
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

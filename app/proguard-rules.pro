# ProGuard rules for MoneyLog

# --- Kotlin ---
-keepattributes *Annotation*
-keepclassmembers class ** {
    @org.jetbrains.annotations.NotNull *;
}

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface *

# --- Hilt ---
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.* *;
}

# --- Google API Client (Drive) ---
-keep class com.google.api.** { *; }
-keep class com.google.apis.** { *; }
-dontwarn com.google.api.**
-dontwarn com.google.common.collect.**

# --- Google AI Edge (Gemini Nano) ---
-keep class com.google.ai.edge.** { *; }
-dontwarn com.google.ai.edge.**

# --- Gson (Google API Client 내부 사용) ---
-keepattributes Signature
-keepattributes EnclosingMethod
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }

# --- 디버그 정보 유지 (크래시 리포트용) ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

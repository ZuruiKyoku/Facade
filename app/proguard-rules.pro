# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Room
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# Hilt / Dagger
-dontwarn com.google.errorprone.annotations.**

# Shizuku
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }

# Media3
-dontwarn com.google.android.exoplayer2.**

# Keep model classes used for reflection / Room entities
-keep class com.slygames.facade.data.model.** { *; }
-keep class com.slygames.facade.data.local.db.** { *; }

# Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep the wallpaper / accessibility service classes referenced only from XML/manifest
-keep class com.slygames.facade.services.** { *; }

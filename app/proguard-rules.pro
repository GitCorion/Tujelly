# Reglas de ProGuard/R8 para Tujelly

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# Kotlinx Serialization
-keepattributes *Annotation*, ElementValueAttribute
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep class kotlinx.serialization.** { *; }
-keep class com.example.tujelly.data.remote.** { *; }

# Room Database Entities, DAOs and Generated Impl Classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.example.tujelly.data.local.db.** { *; }
-keep class **_Impl { *; }

# Media3 / ExoPlayer
-dontwarn androidx.media3.**

# Coil Image Loader
-dontwarn coil.**

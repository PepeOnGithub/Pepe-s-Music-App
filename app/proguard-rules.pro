# Moshi codegen adapters
-keep class **JsonAdapter { <init>(...); }
-keep @com.squareup.moshi.JsonClass class * { *; }
# Retrofit service interfaces
-keep,allowobfuscation interface xyz.pepe.glacierclient.musicapp.data.remote.** { *; }
-keepattributes Signature, *Annotation*, Exceptions
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
# Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

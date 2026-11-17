# ---- Sigha release rules ----
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# Kotlin metadata / reflection used by Compose and serialization
-keep class kotlin.Metadata { *; }

# kotlinx.serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.sigha.converter.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# FFmpegKit (JNI callbacks)
-keep class com.arthenica.ffmpegkit.** { *; }
-dontwarn com.arthenica.ffmpegkit.**

# Tesseract4Android (JNI)
-keep class cz.adaptech.tesseract4android.** { *; }
-keep class com.googlecode.tesseract.android.** { *; }

# PDFBox optionally uses the Gemalto JPEG-2000 decoder for JPX images; that dependency is not
# bundled. PDFs whose images are JPEG-2000 will fail that filter, everything else works.
-dontwarn com.gemalto.jp2.**

# PDFBox: reflection + bundled resources
-keep class org.apache.pdfbox.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn org.apache.pdfbox.**
-dontwarn org.bouncycastle.**

# Commons Compress uses optional codecs by reflection
-dontwarn org.apache.commons.compress.**
-keep class org.apache.commons.compress.** { *; }

# Coil / OkHttp optional TLS providers
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Keep our own model classes readable in crash reports
-keepnames class com.sigha.converter.** { *; }

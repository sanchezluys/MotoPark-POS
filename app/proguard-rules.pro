# ProGuard and R8 Obfuscation & Optimization Rules

# Reglas de optimización avanzada y ofuscación agresiva
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''
-dontusemixedcaseclassnames

# Ofuscación de atributos y nombres de archivos de código fuente
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable,InnerClasses,EnclosingMethod,Signature,*Annotation*

# Eliminar logs informativos y de depuración en la compilación de producción
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# Room Database & Modelos de datos
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep class com.example.data.model.** { *; }
-keep class com.example.data.dao.** { *; }
-dontwarn androidx.room.paging.**

# ML Kit Text Recognition (OCR)
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Jetpack Compose
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**


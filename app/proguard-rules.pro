# === Firebase Auth ===
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# === Firebase Firestore ===
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**

# === Modelos de datos — Firestore necesita leer campos por nombre ===
-keep class com.example.habittrackerapp.data.Habit { *; }
-keep class com.example.habittrackerapp.data.Reflexion { *; }
-keep class com.example.habittrackerapp.data.ResumenSemanal { *; }
-keep class com.example.habittrackerapp.data.EstadisticasUsuario { *; }
-keep class com.example.habittrackerapp.data.HabitCategory { *; }

# === Kotlin Coroutines ===
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# === Navigation Component ===
-keepnames class androidx.navigation.fragment.NavHostFragment
-keepnames class * extends androidx.fragment.app.Fragment

# === WorkManager ===
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# === Mantener nombres de clases de excepciones ===
-keepnames class * extends java.lang.Exception

# === Evitar warnings de librerías internas ===
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
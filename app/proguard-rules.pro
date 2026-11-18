# Canopy Proguard rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep class app.canopy.launcher.data.** { *; }
-keep class app.canopy.launcher.layout.** { *; }
-keep class kotlinx.serialization.** { *; }

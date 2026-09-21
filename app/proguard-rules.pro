# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Kotlin
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
	public static void check*(...);
	public static void throw*(...);
}
-assumenosideeffects class java.util.Objects {
    public static ** requireNonNull(...);
}

# Strip debug log
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# Activity and Fragment names
-keep class com.sysui.batt.ui.activities.**
-keep class com.sysui.batt.ui.fragments.**

# LSPosed API 102 entry (loaded via META-INF/xposed/java_init.list)
-keep class com.sysui.batt.xposed.ModernInitHook
-keepnames class com.sysui.batt.xposed.**
-keepnames class com.sysui.batt.xposed.utils.XPrefs
-keep class com.sysui.batt.xposed.** {
    <init>(android.content.Context);
}

# Weather
-keepnames class com.sysui.batt.utils.weather.**
-keep class com.sysui.batt.utils.weather.** { *; }

# EventBus
-keepattributes *Annotation*
-keepclassmembers,allowoptimization,allowobfuscation class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep,allowoptimization,allowobfuscation enum org.greenrobot.eventbus.ThreadMode { *; }

# If using AsyncExecutord, keep required constructor of default event used.
# Adjust the class name if a custom failure event type is used.
-keepclassmembers,allowoptimization,allowobfuscation class org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

# Accessed via reflection, avoid renaming or removal
-keep,allowoptimization,allowobfuscation class org.greenrobot.eventbus.android.AndroidComponentsImpl

# Keep the ConstraintLayout Motion class
-keep,allowoptimization,allowobfuscation class androidx.constraintlayout.motion.widget.** { *; }

# Keep Recycler View Stuff
-keep,allowoptimization,allowobfuscation class androidx.recyclerview.widget.** { *; }

# Keep Parcelable Creators
-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Obfuscation
-repackageclasses
-allowaccessmodification

# Root Service
-keep class com.sysui.batt.services.RootProviderProxy { *; }
-keep class com.sysui.batt.IRootProviderProxy { *; }

# AIDL Classes
-keep interface **.I* { *; }
-keep class **.I*$Stub { *; }
-keep class **.I*$Stub$Proxy { *; }

# Circle Battery: LSPosed loads ModernInitHook by name from META-INF/xposed/java_init.list,
# invisible to R8. Keep the whole xposed package plus app entry points.
-keep class com.sysui.batt.xposed.** { *; }
-keep class com.sysui.batt.BattApp { *; }
-keep class com.sysui.batt.MainActivity { *; }
-keep class com.sysui.batt.data.provider.** { *; }

# R8 full-mode refs javax.lang.model via error-prone annotations; Android
# runtime never needs it.
-dontwarn javax.lang.model.**

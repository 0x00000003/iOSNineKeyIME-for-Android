# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# disable obfuscation
-dontobfuscate

# Project fork: keep exact JNI names/constructors, not unused legacy RimeApi/dispatcher roots.
-keep,allowoptimization class com.osfans.trime.core.Rime {
    native <methods>;
    public static void handleRimeMessage(int, java.lang.Object[]);
}
-keep class com.osfans.trime.core.*Proto { *; }
-keep class com.osfans.trime.core.RimeResponse { *; }
-keep interface com.osfans.trime.core.Candidates { *; }
-keep class com.osfans.trime.core.Candidates$* { *; }
-keep class com.osfans.trime.core.SchemaItem { *; }
-keep class com.osfans.trime.core.RimeKeyEvent { *; }

# remove kotlin null checks
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkNotNull(...);
    static void checkExpressionValueIsNotNull(...);
    static void checkNotNullExpressionValue(...);
    static void checkReturnedValueIsNotNull(...);
    static void checkFieldIsNotNull(...);
    static void checkParameterIsNotNull(...);
    static void checkNotNullParameter(...);
}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Stable ABI for the separate local optimized-engine instrumentation APK.
# The same rules apply to unsigned release; no legacy dispatcher/service roots.
-keep,allowoptimization class com.osfans.trime.core.Rime$Companion { *; }
-keepclassmembers class com.osfans.trime.core.Rime { public static ** Companion; }
-keep,allowoptimization class dev.local.iosninekeyime.probe.ProbeRunner { *; }
-keep,allowoptimization class dev.local.iosninekeyime.probe.ProbeResources { *; }
-keep,allowoptimization class dev.local.iosninekeyime.probe.ProbeModelResources { *; }
-keep,allowoptimization class dev.local.iosninekeyime.probe.ProbeFileSnapshot { *; }
-keep,allowoptimization class dev.local.iosninekeyime.probe.ProbeFailureCode { *; }
-keep,allowoptimization class dev.local.iosninekeyime.engine.NativeEngineLease { *; }
-keep,allowoptimization class dev.local.iosninekeyime.privacy.LearningStore { *; }
-keep,allowoptimization class dev.local.iosninekeyime.state.KeyboardReducer { *; }

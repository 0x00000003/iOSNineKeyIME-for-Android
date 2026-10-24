// SPDX-License-Identifier: GPL-3.0-or-later
// Standalone synthetic host only. No dependency on the IME, native code or runtime data.
plugins { alias(libs.plugins.android.application) }
android {
    namespace = "dev.local.iosninekeyime.testhost"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "dev.local.iosninekeyime.testhost"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "0.1-synthetic-only"
        testInstrumentationRunner = "dev.local.iosninekeyime.testhost.HostInstrumentation"
    }
    signingConfigs.getByName("debug").storeFile = rootProject.file("../../.private/baseline-debug.keystore")
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { buildConfig = false }
    buildTypes.all {
        @Suppress("UnstableApiUsage")
        vcsInfo.include = false
    }
}
// Only a debug test companion; never an additional IME release variant.
androidComponents.beforeVariants(androidComponents.selector().withBuildType("release")) { it.enable = false }
// Keep AGP's built-in Kotlin runtime at the same reviewed, verified version as the main app.
dependencies { implementation("org.jetbrains.kotlin:kotlin-stdlib:2.3.21") }

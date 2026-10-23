// SPDX-License-Identifier: GPL-3.0-or-later
plugins { alias(libs.plugins.kotlin.jvm) }
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) }
dependencies {
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}
tasks.test { useJUnitPlatform() }
dependencyLocking { lockAllConfigurations() }

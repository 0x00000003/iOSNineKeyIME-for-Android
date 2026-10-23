/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

@Suppress("unused")
class NativeAppConventionPlugin : NativeBaseConventionPlugin() {
    private fun Project.sourceVersion(key: String): String {
        val versions = java.util.Properties()
        rootProject.file("source-versions.properties").inputStream().use { versions.load(it) }
        return requireNotNull(versions.getProperty(key))
    }
    private val Project.librimeVersion: String
        get() = sourceVersion("librime")
    private val Project.openccVersion: String
        get() = sourceVersion("opencc")

    override fun apply(target: Project) {
        super.apply(target)

        target.pluginManager.apply("com.android.application")

        target.extensions.configure<ApplicationExtension> {
            packaging {
                jniLibs {
                    useLegacyPackaging = true
                }
            }
            defaultConfig {
                buildConfigField("String", "LIBRIME_VERSION", "\"${target.librimeVersion}\"")
                buildConfigField("String", "OPENCC_VERSION", "\"${target.openccVersion}\"")
            }
        }
    }
}

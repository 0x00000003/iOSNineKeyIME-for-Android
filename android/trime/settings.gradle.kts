// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "trime"
include(":app")
include(":codegen")

// Pure reducer validation is independent of the WIP runtime resource gate.
include(":state")

// Separate cross-UID synthetic host; never an IME runtime dependency.
include(":testhost")

// Lock every used project and plugin classpath, including the included convention build.
// Missing or changed lock state fails, including a new configuration not yet reviewed.
gradle.beforeProject {
    dependencyLocking {
        lockAllConfigurations()
        lockMode.set(org.gradle.api.artifacts.dsl.LockMode.STRICT)
    }
    buildscript.configurations.configureEach {
        if (name == "classpath") resolutionStrategy.activateDependencyLocking()
    }
}

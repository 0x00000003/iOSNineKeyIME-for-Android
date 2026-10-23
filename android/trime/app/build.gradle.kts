/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
@file:Suppress("UnstableApiUsage")

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.security.MessageDigest
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import javax.xml.parsers.DocumentBuilderFactory
import java.io.File

plugins {
    id("com.osfans.trime.app-convention")
    id("com.osfans.trime.native-app-convention")
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
}

val runtimeLocks = ((JsonSlurper().parse(rootProject.file("../../upstream-lock.json")) as Map<*, *>)["runtime_assets"] as List<*>)
    .filterIsInstance<Map<*, *>>()
val probeResourceLock = runtimeLocks.single { it["id"] == "m1-engine-probe" }
val compactModelLock = runtimeLocks.singleOrNull { it["id"] == "m1-octagram-compact-model" }
fun pinnedSha(row: Map<*, *>): String = (row["sha256"] as String).also { require(it.matches(Regex("[a-f0-9]{64}"))) }

val noticeManifest = JsonSlurper().parse(rootProject.file("../../third_party/offline-notice-sources.json")) as Map<*, *>
val noticeSha = pinnedSha(noticeManifest)
// Explicit local validation only. Default release remains unsigned; no external key lookup.
val localReleaseProbe = providers.gradleProperty("localReleaseProbe").orNull == "true"
val releaseEngineTests = layout.buildDirectory.dir("generated/releaseEngineTests")
val prepareReleaseEngineTests = tasks.register<Sync>("prepareReleaseEngineTests") {
    from("src/androidTest/java") {
        include("**/QualityBoundaryProbe.kt", "**/LearningBoundaryProbe.kt", "**/ContextBoundaryProbe.kt", "**/ResourceBoundaryProbe.kt")
    }
    into(releaseEngineTests)
}
val runtimeInventoryDir = layout.buildDirectory.dir("generated/runtimeInventory")
val offlineNoticeDir = layout.buildDirectory.dir("generated/offlineNotices")
val prepareRuntimeAssets = tasks.register<Exec>("prepareRuntimeAssets") {
    inputs.files(rootProject.file("../../upstream-lock.json"), rootProject.file("../../third_party/android-probe-resources.json"), rootProject.file("../../third_party/prebuilt-tables.json"))
    inputs.files(rootProject.fileTree("../../scripts") { include("*.py") }, rootProject.file("../../tests/native/ime_probe.lua"))
    outputs.dir(layout.buildDirectory.dir("generated/imeAssets"))
    outputs.upToDateWhen { false } // Regenerate and verify before packaging, including a clean checkout.
    commandLine("python3", rootProject.file("../../scripts/ensure_runtime_assets.py"))
}

val prepareOfflineNotices = tasks.register<Exec>("prepareOfflineNotices") {
    inputs.file(rootProject.file("../../third_party/offline-notice-sources.json"))
    inputs.file(rootProject.file("../../scripts/prepare_offline_notices.py"))
    inputs.files((noticeManifest["files"] as List<*>).map { rootProject.file("../../" + (it as Map<*, *>)["path"]) })
    outputs.dir(offlineNoticeDir)
    commandLine("python3", rootProject.file("../../scripts/prepare_offline_notices.py"), "--output", offlineNoticeDir.get().asFile)
}

android {
    namespace = "com.osfans.trime"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "dev.local.iosninekeyime.probe"
        minSdk = 21
        targetSdk = 36
        versionCode = 20261023
        versionName = "0.1.0-m1-wip"

        testInstrumentationRunner = if (localReleaseProbe) "dev.local.iosninekeyime.probe.ReleaseEngineInstrumentation" else "dev.local.iosninekeyime.probe.ProbeInstrumentation"
        multiDexEnabled = true
        buildConfigField("String", "OFFLINE_NOTICES_SHA256", "\"$noticeSha\"")
        buildConfigField("String", "PROBE_RESOURCE_SHA256", "\"${pinnedSha(probeResourceLock)}\"")
        buildConfigField("boolean", "PROBE_MODEL_ENABLED", (compactModelLock != null).toString())
        buildConfigField("String", "PROBE_MODEL_SHA256", "\"${compactModelLock?.let(::pinnedSha).orEmpty()}\"")
        buildConfigField("long", "PROBE_MODEL_BYTES", "${(compactModelLock?.get("bytes") as? Number)?.toLong() ?: 0}L")
        buildConfigField("String", "BUILDER", "\"${project.builder}\"")
        buildConfigField("long", "BUILD_TIMESTAMP", project.buildTimestamp)
        buildConfigField("String", "BUILD_COMMIT_HASH", "\"${project.buildCommitHash}\"")
        buildConfigField("String", "BUILD_GIT_REPO", "\"${project.buildGitRepo}\"")
        buildConfigField("String", "BUILD_VERSION_NAME", "\"${project.buildVersionName}\"")
    }

    sourceSets.getByName("androidTest").assets.srcDir(rootProject.file("../../tests/fixtures"))
    testBuildType = if (localReleaseProbe) "release" else "debug"
    if (localReleaseProbe) sourceSets.getByName("androidTest").apply {
        java.setSrcDirs(listOf("src/releaseEngineTest/java", releaseEngineTests))
        kotlin.setSrcDirs(listOf("src/releaseEngineTest/java", releaseEngineTests))
    }
    sourceSets.getByName("main").assets.setSrcDirs(listOf(layout.buildDirectory.dir("generated/imeAssets"), offlineNoticeDir, runtimeInventoryDir))
    signingConfigs.getByName("debug").storeFile = rootProject.file("../../.private/baseline-debug.keystore")

    base {
        // https://www.norio.be/blog/archivesBaseName-removed-from-gradle9.html
        archivesName = "${android.defaultConfig.applicationId}-$buildVersionName"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        resValues = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "probe-runtime-abi.pro",
            )
            signingConfig = if (localReleaseProbe) signingConfigs.getByName("debug") else null

            resValue("string", "trime_app_name", "九键 M1 探针")
        }
        debug {
            applicationIdSuffix = ".debug"

            resValue("string", "trime_app_name", "九键 M1 探针")
        }
        all {
            // remove META-INF/version-control-info.textproto
            @Suppress("UnstableApiUsage")
            vcsInfo.include = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // hack workaround lint gradle 8.0.2
    lint {
        checkReleaseBuilds = true
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    packaging {
        resources {
            excludes +=
                setOf(
                    "/META-INF/*.version",
                    "/META-INF/*.kotlin_module", // cannot be excluded actually
                    "/META-INF/androidx/**",
                    "/DebugProbesKt.bin",
                    "/kotlin-tooling-metadata.json",
                )
        }
    }
}

aboutLibraries {
    collect {
        configPath.set(file("licenses").takeIf { it.exists() })
        fetchRemoteLicense.set(false)
        fetchRemoteFunding.set(false)
        includePlatform.set(false)
    }
    export {
        excludeFields.set(
            setOf("generated", "developers", "organization", "scm", "funding", "content"),
        )
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":state"))
    ksp(project(":codegen"))
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.autofill)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.recyclerview)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.flexbox)
    implementation(libs.bravh)
    implementation(libs.timber)
    implementation(libs.xxpermissions)
    implementation(libs.kodein.di)
    implementation(libs.snakeyaml)
    implementation(libs.splitties.bitflags)
    implementation(libs.splitties.systemservices)
    implementation(libs.splitties.views.dsl)
    implementation(libs.splitties.views.dsl.constraintlayout)
    implementation(libs.splitties.views.dsl.coordinatorlayout)
    implementation(libs.splitties.views.dsl.recyclerview)
    implementation(libs.splitties.views.recyclerview)
    implementation(libs.aboutlibraries.core)
    implementation(libs.iconics.core)
    implementation(libs.community.material.typeface) {
        artifact { type = "aar" }
    }

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    androidTestImplementation(libs.junit)
}

configurations {
    all {
        // remove Baseline Profile Installer or whatever it is...
        exclude(group = "androidx.profileinstaller", module = "profileinstaller")
    }
}

// All resolved configurations are locked; missing/changed locks fail in strict mode after generation.
dependencyLocking { lockAllConfigurations() }


tasks.named("preBuild") { dependsOn(prepareRuntimeAssets, prepareOfflineNotices, "writeReleaseRuntimeInventory") }
if (localReleaseProbe) tasks.configureEach {
    if (name.contains("ReleaseAndroidTest") && (name.startsWith("compile") || name.startsWith("ksp"))) dependsOn(prepareReleaseEngineTests)
}

// Exact resolved artifact bytes, no cache paths/account identity in the report.
tasks.register("writeReleaseRuntimeInventory") {
    dependsOn(configurations.named("releaseRuntimeClasspath"))
    val output = runtimeInventoryDir.map { it.file("runtime-artifacts-release.json") }
    outputs.file(output)
    // Refresh hashes from resolved bytes every build, including local project jars.
    outputs.upToDateWhen { false }
    doLast {
        fun sha(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(65536)
                while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
        val artifacts = configurations.getByName("releaseRuntimeClasspath").incoming.artifacts.artifacts
        val modules = artifacts.map { it.id.componentIdentifier }.filterIsInstance<ModuleComponentIdentifier>().distinct()
        val poms = dependencies.createArtifactResolutionQuery().forComponents(modules)
            .withArtifacts(MavenModule::class.java, MavenPomArtifact::class.java).execute().resolvedComponents
            .associate { component ->
                val pom = component.getArtifacts(MavenPomArtifact::class.java).filterIsInstance<ResolvedArtifactResult>().single().file
                component.id.displayName to pom
            }
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            isExpandEntityReferences = false
        }
        fun declaredLicenses(pom: File, chain: MutableList<Map<String, String>>, depth: Int = 0): List<Map<String, String>> {
            require(depth < 8) { "E_POM_PARENT_DEPTH" }
            chain += mapOf("file" to pom.name, "sha256" to sha(pom))
            val doc = factory.newDocumentBuilder().parse(pom)
            val nodes = doc.getElementsByTagNameNS("*", "license")
            val licenses = mutableListOf<Map<String, String>>()
            for (i in 0 until nodes.length) {
                val item = nodes.item(i) as org.w3c.dom.Element
                fun field(name: String) = item.getElementsByTagNameNS("*", name).item(0)?.textContent?.trim().orEmpty()
                licenses += mapOf("name" to field("name"), "url" to field("url"))
            }
            if (licenses.isNotEmpty()) return licenses
            val parent = doc.getElementsByTagNameNS("*", "parent").item(0) as? org.w3c.dom.Element
                ?: error("E_POM_LICENSE_MISSING")
            fun parentField(name: String) = parent.getElementsByTagNameNS("*", name).item(0)?.textContent?.trim().orEmpty()
            val coordinate = listOf("groupId", "artifactId", "version").map { parentField(it).also { value -> require(value.isNotEmpty() && !value.contains('$')) { "E_POM_PARENT_COORDINATE" } } }.joinToString(":")
            val parts = coordinate.split(":")
            val parentPom = dependencies.createArtifactResolutionQuery().forModule(parts[0], parts[1], parts[2])
                .withArtifacts(MavenModule::class.java, MavenPomArtifact::class.java).execute().resolvedComponents.single()
                .getArtifacts(MavenPomArtifact::class.java).filterIsInstance<ResolvedArtifactResult>().single().file
            return declaredLicenses(parentPom, chain, depth + 1)
        }
        val records = artifacts.map { artifact ->
            val component = artifact.id.componentIdentifier.displayName
            val pom = poms[component]
            val chain = mutableListOf<Map<String, String>>()
            val licenses = if (pom != null) declaredLicenses(pom, chain) else emptyList()
            mapOf("component" to component, "file" to artifact.file.name,
                "bytes" to artifact.file.length(), "sha256" to sha(artifact.file),
                "pom_file" to pom?.name, "pom_sha256" to pom?.let { sha(it) },
                "pom_license_sources" to chain, "pom_licenses" to licenses,
                "scope" to if(pom == null) "local project code" else "exact selected Maven artifact/POM with explicit parent inheritance; publisher declaration, not independent legal approval")
        }.sortedBy { it["component"].toString() + "/" + it["file"] }
        val file = output.get().asFile
        file.parentFile.mkdirs()
        file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(records)) + "\n")
    }
}

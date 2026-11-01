import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val releaseStorePath = providers.environmentVariable("HICREATE_RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("HICREATE_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("HICREATE_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("HICREATE_RELEASE_KEY_PASSWORD").orNull
val releaseSigningValues = listOf(
    releaseStorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
)
val hasAnyReleaseSigning = releaseSigningValues.any { !it.isNullOrBlank() }
val hasReleaseSigning = releaseSigningValues.all { !it.isNullOrBlank() }

if (hasAnyReleaseSigning && !hasReleaseSigning) {
    throw GradleException(
        "Incomplete release signing configuration. Set HICREATE_RELEASE_STORE_FILE, " +
            "HICREATE_RELEASE_STORE_PASSWORD, HICREATE_RELEASE_KEY_ALIAS and " +
            "HICREATE_RELEASE_KEY_PASSWORD.",
    )
}

android {
    namespace = "com.orynnx.hicreatehook"
    // libxposed API 102 AARs are built against android-37; targetSdk stays 36.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.orynnx.hicreatehook"
        minSdk = 31
        targetSdk = 36
        versionCode = 10000
        versionName = "1.0.0-alpha-release"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
    }

    // The proprietary Huawei runtime payload is never part of the source tree.
    // Supply it with -PassetsDir=... or place it in local-assets/ (git-ignored).
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/moduleAssets"))

    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }
}

dependencies {
    compileOnly(libs.libxposed.api)
    implementation(libs.miuix)
    implementation(libs.glasmio.ui)
    implementation(libs.activity.ktx)
    testImplementation(libs.junit)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

val configuredAssetsDir = providers.gradleProperty("assetsDir").orNull
val allowDebugSigning = providers.gradleProperty("allowDebugSigning").orNull == "true"
val moduleAssetsDir = if (configuredAssetsDir != null) {
    rootProject.file(configuredAssetsDir)
} else {
    rootProject.file("local-assets")
}
val requiredModuleAssets = listOf(
    "featurelayer-widget.dex",
    "hwemui.dex",
    "hwext-res.apk",
    "hwframework-full.jar",
    "hwframework.dex",
    "xposed_init",
)

val syncModuleAssets by tasks.registering(Copy::class) {
    description = "Copy the locally supplied Huawei runtime payload into the APK."
    from(moduleAssetsDir)
    into(layout.buildDirectory.dir("generated/moduleAssets"))
    includeEmptyDirs = false
}

tasks.named("preBuild").configure {
    dependsOn(syncModuleAssets)
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(syncModuleAssets)
    doLast {
        if (!hasReleaseSigning && !allowDebugSigning) {
            throw GradleException(
                "Release signing is not configured. Set all HICREATE_RELEASE_* variables, " +
                    "or pass -PallowDebugSigning=true for a local debug-signed test build.",
            )
        }
        val missing = requiredModuleAssets.filterNot { File(moduleAssetsDir, it).isFile }
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Missing Huawei runtime payload: ${missing.joinToString()}. " +
                    "Pass -PassetsDir=<directory> or place the files in local-assets/.",
            )
        }
    }
}




plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    filter {
        exclude("**/build/**")
        exclude("**/*.gradle.kts")
        exclude("**/buildSrc/**")
    }
    // Enable automatic formatting
    verbose.set(true)
    outputToConsole.set(true)
    coloredOutput.set(true)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/detekt-config.yml")
    autoCorrect = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        md.required.set(true)
        sarif.required.set(true)
        txt.required.set(true)
    }
}

android {
    namespace = "com.github.radupana.featherweight"
    compileSdk = 36

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "featherweight-release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: "featherweight"
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    defaultConfig {
        applicationId = "com.github.radupana.featherweight"
        minSdk = 26
        targetSdk = 36

        // Read version from gradle.properties
        val appVersion = project.findProperty("appVersion") as String? ?: "1.0.0"
        versionName = appVersion

        // Calculate versionCode from semantic version
        // Format: MAJOR * 10000 + MINOR * 100 + PATCH
        // Example: 1.2.3 becomes 10203
        val versionParts = appVersion.split(".")
        versionCode =
            if (versionParts.size == 3) {
                val major = versionParts[0].toIntOrNull() ?: 0
                val minor = versionParts[1].toIntOrNull() ?: 0
                val patch = versionParts[2].toIntOrNull() ?: 0
                major * 10000 + minor * 100 + patch
            } else {
                1
            }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            // Ensure consistent class generation for coverage
            isMinifyEnabled = false
            isShrinkResources = false
        }
        create("alpha") {
            initWith(getByName("debug"))
            isDebuggable = true
            versionNameSuffix = "-alpha"
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.ignoreFailures = false
            }
        }
    }

    packaging {
        resources {
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Reorderable library for drag-and-drop in LazyColumn
    implementation(libs.reorderable)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material)
    implementation(libs.androidx.material.icons.extended)

    // Splash Screen
    implementation(libs.androidx.core.splashscreen)

    // Coroutines
    implementation(libs.kotlinx.coroutines.play.services)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Mocking
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.android)

    // Assertion Libraries
    testImplementation(libs.truth)
    testImplementation(libs.turbine) // For Flow testing

    // AndroidX Test
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.core.ktx)

    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.androidx.room.testing)

    // SQLCipher for database encryption
    // SQLCipher removed - using Android's built-in encryption instead
    // Saves ~13 MB of APK size with no security impact
    // See: https://github.com/radupana/featherweight/issues/126
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Gson for JSON parsing
    implementation(libs.gson)

    // HTTP Client
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Compose Foundation (required for Calendar)
    implementation(libs.androidx.compose.foundation)

    // Calendar - using version 2.6.1 which is compatible with latest Compose
    implementation(libs.compose)

    // Lottie for professional animations
    implementation(libs.lottie.compose)

    // UI Testing with UI Automator
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestUtil(libs.androidx.test.orchestrator)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.functions)
    implementation(libs.google.auth)
    implementation(libs.firebase.crashlytics.ndk)
    implementation(libs.google.firebase.analytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.performance)

    // App Check for security
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)

    // Firebase App Distribution - API only for all builds
    implementation(libs.firebase.appdistribution.api)
    // Full SDK for debug and alpha builds
    debugImplementation(libs.firebase.appdistribution)
    "alphaImplementation"(libs.firebase.appdistribution)
}

tasks.named("preBuild") {
    dependsOn("ktlintFormat")
}

tasks.named("check") {
    dependsOn("detekt", "ktlintCheck")
}

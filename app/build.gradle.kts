plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    id("jacoco")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    filter {
        exclude("**/build/**")
        exclude("**/*.gradle.kts")
        exclude("**/buildSrc/**")
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/detekt-config.yml")
    autoCorrect = true
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

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true

        unitTests {
            isIncludeAndroidResources = true
            all {
                it.ignoreFailures = true
            }
        }
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

    // Robolectric for Context-dependent tests
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.androidx.room.testing)
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
    implementation(libs.firebase.crashlytics.ndk)
    implementation(libs.google.firebase.analytics)
    implementation(libs.firebase.config)
}

// Make Detekt part of the build process
tasks.named("check") {
    dependsOn("detekt")
}

// Detekt runs on check task
// Enable for preBuild once all issues are resolved

// JaCoCo configuration for code coverage
tasks.withType<Test> {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter =
        listOf(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*",
            "**/databinding/**/*.*",
            "**/generated/**/*.*",
        )

    val debugTree =
        fileTree(layout.buildDirectory.dir("intermediates/javac/debug")) {
            exclude(fileFilter)
        }

    val kotlinDebugTree =
        fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
            exclude(fileFilter)
        }

    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    classDirectories.setFrom(files(debugTree, kotlinDebugTree))
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("**/*.exec", "**/*.ec")
        },
    )
}

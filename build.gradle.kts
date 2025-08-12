// Top-level build file where you can add configuration options common to all sub-projects/modules.

apply(from = "gradle/code-quality.gradle.kts")
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktlint) apply true
    alias(libs.plugins.detekt) apply true
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    filter {
        exclude("**/build/**")
        exclude("**/*.gradle.kts")
        exclude("**/buildSrc/**")
    }
}

configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/detekt-config.yml")
    // NO BASELINE - WE FIX EVERYTHING
    
    source.setFrom(
        "app/src/main/java",
        "app/src/test/java",
        "app/src/androidTest/java"
    )
    
    parallel = true
    autoCorrect = true // AUTO-FIX EVERYTHING POSSIBLE
}

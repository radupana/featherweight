// Top-level build file where you can add configuration options common to all sub-projects/modules.

apply(from = "gradle/code-quality.gradle.kts")
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktlint) apply true
    alias(libs.plugins.detekt) apply true
    id("org.sonarqube") version "6.3.1.5724"
    id("com.google.gms.google-services") version "4.4.3" apply false
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
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

    source.setFrom(
        "app/src/main/java",
        "app/src/test/java",
        "app/src/androidTest/java",
    )

    parallel = true
    autoCorrect = true
}

sonarqube {
    properties {
        property("sonar.projectKey", "featherweight")
        property("sonar.projectName", "Featherweight")
        property("sonar.host.url", "http://localhost:9000")
        property("sonar.token", System.getenv("SONAR_TOKEN") ?: "")
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.coverage.jacoco.xmlReportPaths", "${project.projectDir}/app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
        property("sonar.junit.reportPaths", "${project.projectDir}/app/build/test-results/testDebugUnitTest")
    }
}

// Code Quality Tasks

tasks.register("codeQuality") {
    group = "verification"
    description = "Run all code quality checks"
    dependsOn("ktlintCheck", "detekt")
}

tasks.register("codeQualityFix") {
    group = "verification"
    description = "Auto-fix code quality issues where possible"
    dependsOn("ktlintFormat")
    doLast {
        println("KtLint formatting complete.")
        println("For Detekt auto-fixes, manually set autoCorrect = true in build.gradle.kts")
    }
}

tasks.register("detektReport") {
    group = "verification"
    description = "Generate detailed Detekt HTML report"
    dependsOn("detekt")
    doLast {
        val reportFile = file("${layout.buildDirectory.get()}/reports/detekt/detekt.html")
        if (reportFile.exists()) {
            println("Detekt report generated at: file://${reportFile.absolutePath}")
        } else {
            println("No Detekt report found. Run './gradlew detekt' first.")
        }
    }
}

tasks.register("codeSmells") {
    group = "verification"
    description = "Find and report code smells"
    doLast {
        println("\n=== Code Quality Commands ===")
        println("• ./gradlew detekt           - Run Detekt analysis")
        println("• ./gradlew ktlintCheck      - Check code formatting")
        println("• ./gradlew ktlintFormat     - Auto-fix formatting issues")
        println("• ./gradlew codeQuality      - Run all quality checks")
        println("• ./gradlew detektReport     - Generate HTML report")
        println("\nReports location:")
        println("• build/reports/detekt/detekt.html")
        println("• build/reports/detekt/detekt.txt")
        println("==============================\n")
    }
}
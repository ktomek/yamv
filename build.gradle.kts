plugins {
//    `kotlin-dsl` version "2.0.21"
    kotlin("jvm") version "2.0.21" apply false
    alias(libs.plugins.application) apply false
    alias(libs.plugins.library) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.detekt) apply true
    alias(libs.plugins.compose.compiler) apply false
    `maven-publish`
}

allprojects {
    group = "com.ktomek.yamv" // Common group ID for all modules
    version = "0.0.1" // Default version (can be overridden in submodules)
}

detekt {
    // Version of detekt that will be used. When unspecified the latest detekt
    // version found will be used. Override to stay on the same version.
    toolVersion = "1.23.7"

    // The directories where detekt looks for source files.
    // Defaults to `files("src/main/java", "src/test/java", "src/main/kotlin", "src/test/kotlin")`.
    source.setFrom("src/main/kotlin")

    // Builds the AST in parallel. Rules are always executed in parallel.
    // Can lead to speedups in larger projects. `false` by default.
    parallel = true

    // Define the detekt configuration(s) you want to use.
    // Defaults to the default detekt configuration.
    config.setFrom("config/quality/detekt/detekt-ruleset.yml")

    // Applies the config files on top of detekt's default config file. `false` by default.
    buildUponDefaultConfig = false

    // Turns on all the rules. `false` by default.
    allRules = false

    // Specifying a baseline file. All findings stored in this file in subsequent runs of detekt.
    baseline = file("config/quality/detekt/baseline.xml")

    // Disables all default detekt rulesets and will only run detekt with custom rules
    // defined in plugins passed in with `detektPlugins` configuration. `false` by default.
    disableDefaultRuleSets = false

    // Adds debug output during task execution. `false` by default.
    debug = false

    // If set to `true` the build does not fail when the
    // maxIssues count was reached. Defaults to `false`.
    ignoreFailures = false

    autoCorrect = true

    // Android: Don't create tasks for the specified build types (e.g. "release")
//    ignoredBuildTypes = listOf("release")

    // Android: Don't create tasks for the specified build flavor (e.g. "production")
//    ignoredFlavors = listOf("production")

    // Android: Don't create tasks for the specified build variants (e.g. "productionRelease")
//    ignoredVariants = listOf("productionRelease")

    // Specify the base path for file paths in the formatted reports.
    // If not set, all file paths reported will be absolute file path.
    basePath = projectDir.absolutePath
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    include("**/*.kt")
    include("**/*.kts")
    exclude("resources/")
    exclude("build/")
}

val projectSource = file(projectDir)
val configFile = files("$rootDir/config/quality/detekt/detekt-ruleset.yml")
val baselineFile = file("$rootDir/config/quality/detekt/baseline.xml")
val kotlinFiles = "**/*.kt"
val resourceFiles = "**/resources/**"
val buildFiles = "**/build/**"

tasks.register<io.gitlab.arturbosch.detekt.Detekt>("detektAll") {
    description = "Custom DETEKT build for all modules"
    parallel = true
    ignoreFailures = false
    autoCorrect = true
    buildUponDefaultConfig = true

    // Assuming projectSource, baselineFile, configFile, kotlinFiles, resourceFiles, and buildFiles are defined earlier
    setSource(projectSource)
    baseline.set(baselineFile)
    config.setFrom(configFile)

    include(kotlinFiles)
    exclude(resourceFiles, buildFiles)

    reports {
        html.enabled = true
        xml.enabled = false
        txt.enabled = false
    }
}

dependencies {
    detektPlugins(libs.detekt.formatting)
    detektPlugins(libs.detekt.compose)
}

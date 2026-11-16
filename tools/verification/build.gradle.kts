plugins {
    application
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(libs.jackson.databind)
    implementation(project(":compatibility:contexts"))
    implementation(project(":compatibility:interop"))
    implementation(project(":compatibility:translation"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass = "io.github.luasgraphicss.minemod.verification.cli.VerificationCli"
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.named<JavaExec>("run") {
    workingDir = rootDir
}

tasks.test {
    useJUnitPlatform()
    systemProperty("minemod.repositoryRoot", rootDir.absolutePath)
}

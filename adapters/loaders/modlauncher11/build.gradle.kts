plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    api(project(":adapters:loaders:launch"))
    api(project(":adapters:presentation:glfw"))
    compileOnly(libs.asm)
    compileOnly(libs.asm.tree)
    compileOnly(libs.modlauncher11) { isTransitive = false }

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(testFixtures(project(":adapters:loaders:launch")))
    testImplementation(testFixtures(project(":adapters:presentation:core")))
    testImplementation(libs.asm)
    testImplementation(libs.asm.tree)
    testImplementation(libs.modlauncher11) { isTransitive = false }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.test {
    useJUnitPlatform()
}

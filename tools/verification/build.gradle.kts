plugins {
    application
    id("minemod.provider-tests")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(libs.asm)
    implementation(libs.jackson.databind)
    implementation(project(":adapters:configuration"))
    implementation(project(":adapters:events"))
    implementation(project(":adapters:frame:display"))
    implementation(project(":adapters:frame:glfw"))
    implementation(project(":adapters:loaders:launch"))
    implementation(project(":adapters:renderer-ownership"))
    implementation(project(":adapters:shader-packs"))
    implementation(project(":adapters:transformers"))
    implementation(project(":compatibility:bridges"))
    implementation(project(":compatibility:contexts"))
    implementation(project(":compatibility:interop"))
    implementation(project(":compatibility:runtime"))
    implementation(testFixtures(project(":compatibility:runtime")))
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

plugins {
    id("minemod.runtime-library")
    id("minemod.window-tests")
}

dependencies {
    api(project(":adapters:presentation:core"))
    api(project(":adapters:presentation:renderer"))
    compileOnly(libs.lwjgl2.core) { isTransitive = false }

    testImplementation(testFixtures(project(":adapters:presentation:core")))
    testImplementation(testFixtures(project(":adapters:presentation:renderer")))
    testImplementation(project(":adapters:transformers"))
    testImplementation(libs.asm)
    testImplementation(libs.lwjgl2.core) { isTransitive = false }
}

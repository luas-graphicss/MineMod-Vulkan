plugins {
    id("minemod.runtime-library")
}

dependencies {
    api(project(":adapters:presentation:core"))
    compileOnly(libs.lwjgl2.core) { isTransitive = false }

    testImplementation(testFixtures(project(":adapters:presentation:core")))
    testImplementation(project(":adapters:transformers"))
    testImplementation(libs.asm)
    testImplementation(libs.lwjgl2.core) { isTransitive = false }
}

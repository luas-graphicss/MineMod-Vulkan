plugins {
    id("minemod.runtime-library")
}

dependencies {
    api(project(":adapters:presentation:core"))
    compileOnly(libs.lwjgl3.core)
    compileOnly(libs.lwjgl3.glfw)
    compileOnly(libs.lwjgl3.opengl)

    testImplementation(testFixtures(project(":adapters:presentation:core")))
    testImplementation(project(":adapters:transformers"))
    testImplementation(libs.asm)
    testImplementation(libs.lwjgl3.core)
    testImplementation(libs.lwjgl3.glfw)
    testImplementation(libs.lwjgl3.opengl)
}

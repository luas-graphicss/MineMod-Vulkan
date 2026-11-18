plugins {
    id("minemod.runtime-library")
    id("minemod.window-tests")
}

val lwjglNatives = "natives-linux"

dependencies {
    api(project(":adapters:presentation:core"))
    api(project(":adapters:presentation:renderer"))
    compileOnly(libs.lwjgl3.core)
    compileOnly(libs.lwjgl3.glfw)
    compileOnly(libs.lwjgl3.opengl)

    testImplementation(testFixtures(project(":adapters:presentation:core")))
    testImplementation(testFixtures(project(":adapters:presentation:renderer")))
    testImplementation(project(":adapters:transformers"))
    testImplementation(libs.asm)
    testImplementation(libs.lwjgl3.core)
    testImplementation(libs.lwjgl3.glfw)
    testImplementation(libs.lwjgl3.opengl)
    testRuntimeOnly(variantOf(libs.lwjgl3.core) { classifier(lwjglNatives) })
    testRuntimeOnly(variantOf(libs.lwjgl3.glfw) { classifier(lwjglNatives) })
}

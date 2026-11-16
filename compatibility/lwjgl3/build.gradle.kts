plugins {
    id("minemod.runtime-library")
    id("minemod.provider-tests")
}

val lwjglNatives = "natives-linux"

dependencies {
    api(project(":compatibility:dispatch"))
    compileOnly(libs.lwjgl3.core)
    compileOnly(libs.lwjgl3.opengl)

    testImplementation(project(":compatibility:runtime"))
    testImplementation(testFixtures(project(":compatibility:runtime")))
    testImplementation(libs.lwjgl3.core)
    testImplementation(libs.lwjgl3.opengl)
    testRuntimeOnly(variantOf(libs.lwjgl3.core) { classifier(lwjglNatives) })
    testRuntimeOnly(variantOf(libs.lwjgl3.opengl) { classifier(lwjglNatives) })
}

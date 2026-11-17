plugins {
    id("minemod.runtime-library")
    id("minemod.provider-tests")
}

val lwjglNatives = "natives-linux"

dependencies {
    api(project(":jvm:command-codec"))
    api(project(":jvm:native-bridge"))
    api(project(":compatibility:runtime"))

    testImplementation(testFixtures(project(":compatibility:runtime")))
    testImplementation(project(":compatibility:lwjgl3"))
    testImplementation(libs.lwjgl3.core)
    testImplementation(libs.lwjgl3.opengl)
    testRuntimeOnly(variantOf(libs.lwjgl3.core) { classifier(lwjglNatives) })
    testRuntimeOnly(variantOf(libs.lwjgl3.opengl) { classifier(lwjglNatives) })
}

plugins {
    id("minemod.runtime-library")
}

dependencies {
    api(project(":adapters:frame:core"))
    api(project(":adapters:presentation:glfw"))
    compileOnly(libs.lwjgl3.core)
    compileOnly(libs.lwjgl3.opengl)

    testImplementation(testFixtures(project(":adapters:presentation:core")))
    testImplementation(libs.lwjgl3.core)
    testImplementation(libs.lwjgl3.opengl)
}

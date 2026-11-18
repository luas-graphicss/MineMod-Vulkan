plugins {
    id("minemod.runtime-library")
}

dependencies {
    api(project(":adapters:frame:core"))
    api(project(":adapters:presentation:display"))
    compileOnly(libs.lwjgl2.core) { isTransitive = false }

    testImplementation(testFixtures(project(":adapters:presentation:core")))
    testImplementation(libs.lwjgl2.core) { isTransitive = false }
}

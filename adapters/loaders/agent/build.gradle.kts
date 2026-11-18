plugins {
    id("minemod.runtime-library")
}

dependencies {
    api(project(":adapters:loaders:launch"))
    api(project(":adapters:presentation:glfw"))
    compileOnly(libs.asm)

    testImplementation(testFixtures(project(":adapters:loaders:launch")))
    testImplementation(testFixtures(project(":adapters:presentation:core")))
    testImplementation(libs.asm)
}

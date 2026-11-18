plugins {
    id("minemod.runtime-library")
}

dependencies {
    api(project(":adapters:loaders:launch"))
    api(project(":adapters:presentation:glfw"))
    compileOnly(libs.asm)
    compileOnly(libs.asm.tree)
    compileOnly(libs.modlauncher) { isTransitive = false }

    testImplementation(testFixtures(project(":adapters:loaders:launch")))
    testImplementation(testFixtures(project(":adapters:presentation:core")))
    testImplementation(libs.asm)
    testImplementation(libs.asm.tree)
    testImplementation(libs.modlauncher) { isTransitive = false }
}

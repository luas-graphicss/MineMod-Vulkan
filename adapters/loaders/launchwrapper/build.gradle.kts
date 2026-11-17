plugins {
    id("minemod.runtime-library")
}

dependencies {
    api(project(":adapters:loaders:launch"))
    api(project(":adapters:presentation:display"))
    compileOnly(libs.asm)
    compileOnly(libs.launchwrapper) { isTransitive = false }

    testImplementation(testFixtures(project(":adapters:loaders:launch")))
    testImplementation(libs.asm)
    testImplementation(libs.launchwrapper) { isTransitive = false }
}

plugins {
    id("minemod.runtime-library")
}

dependencies {
    api(project(":adapters:loaders:launch"))
    api(project(":adapters:frame:display"))
    compileOnly(libs.asm)
    compileOnly(libs.launchwrapper) { isTransitive = false }

    testImplementation(testFixtures(project(":adapters:loaders:launch")))
    testImplementation(testFixtures(project(":adapters:presentation:core")))
    testImplementation(libs.asm)
    testImplementation(libs.launchwrapper) { isTransitive = false }
}

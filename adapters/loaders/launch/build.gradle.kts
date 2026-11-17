plugins {
    id("minemod.runtime-library")
    `java-test-fixtures`
}

dependencies {
    api(project(":adapters:transformers"))
    compileOnly(libs.asm)
    compileOnly(libs.asm.tree)

    testFixturesImplementation(libs.asm)

    testImplementation(libs.asm)
    testImplementation(libs.asm.tree)
}

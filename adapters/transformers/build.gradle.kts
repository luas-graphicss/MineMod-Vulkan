plugins {
    id("minemod.runtime-library")
}

dependencies {
    compileOnly(libs.asm)

    testImplementation(libs.asm)
}

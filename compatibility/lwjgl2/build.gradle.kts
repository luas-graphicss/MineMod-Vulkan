plugins {
    id("minemod.runtime-library")
    id("minemod.provider-tests")
}

val lwjglNatives = configurations.dependencyScope("lwjglNatives")
val lwjglNativesArchive = configurations.resolvable("lwjglNativesArchive") {
    extendsFrom(lwjglNatives.get())
}

dependencies {
    api(project(":compatibility:dispatch"))
    implementation(project(":compatibility:natives"))
    compileOnly(libs.lwjgl2.core) { isTransitive = false }

    testImplementation(project(":compatibility:runtime"))
    testImplementation(testFixtures(project(":compatibility:runtime")))
    testImplementation(libs.lwjgl2.core) { isTransitive = false }
    lwjglNatives(variantOf(libs.lwjgl2.platform) { classifier("natives-linux") })
}

val extractLwjglNatives = tasks.register<Sync>("extractLwjglNatives") {
    description = "Unpacks the native libraries of LWJGL 2 for the tests that load them."
    from(lwjglNativesArchive.map { archives -> archives.map { zipTree(it) } }) {
        include("*.so")
    }
    into(layout.buildDirectory.dir("lwjgl-natives"))
}

tasks.withType<Test>().configureEach {
    inputs.files(extractLwjglNatives).withPropertyName("lwjglNatives")
    systemProperty("org.lwjgl.librarypath", extractLwjglNatives.get().destinationDir.absolutePath)
}

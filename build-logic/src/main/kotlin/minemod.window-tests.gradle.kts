plugins {
    java
}

val rendererLibrary = providers.gradleProperty("minemod.sceneLibrary")

tasks.withType<Test>().configureEach {
    if (rendererLibrary.isPresent) {
        inputs.property("rendererLibrary", rendererLibrary)
        systemProperty("minemod.sceneLibrary", rendererLibrary.get())
    }
}

plugins {
    java
}

val rendererLibrary = providers.gradleProperty("minemod.sceneLibrary")
val windowSystem = providers.environmentVariable("DISPLAY")

tasks.withType<Test>().configureEach {
    if (rendererLibrary.isPresent) {
        inputs.property("rendererLibrary", rendererLibrary)
        inputs.property("windowSystem", windowSystem.isPresent)
        systemProperty("minemod.sceneLibrary", rendererLibrary.get())
    }
}

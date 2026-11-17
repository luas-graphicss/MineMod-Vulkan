plugins {
    java
}

val providerNatives = rootProject.layout.projectDirectory.file("build/linux-jni/jni/libminemod_gl_provider_jni.so")
val systemProvider = providers.gradleProperty("minemod.systemProvider")
val sceneHost = providers.gradleProperty("minemod.sceneHost")
val sceneLibrary = providers.gradleProperty("minemod.sceneLibrary")
val runsOnLinux = System.getProperty("os.name") == "Linux"

tasks.withType<Test>().configureEach {
    systemProperty("java.awt.headless", "true")
    if (runsOnLinux) {
        dependsOn(":jvm:native-bridge:buildNativeLibrary")
        systemProperty("minemod.providerNatives", providerNatives.asFile.absolutePath)
    }
    if (runsOnLinux && systemProvider.isPresent) {
        inputs.property("systemProvider", systemProvider)
        systemProperty("minemod.systemProvider", systemProvider.get())
    }
    if (runsOnLinux && sceneHost.isPresent) {
        inputs.property("sceneHost", sceneHost)
        systemProperty("minemod.sceneHost", sceneHost.get())
    }
    if (runsOnLinux && sceneLibrary.isPresent) {
        inputs.property("sceneLibrary", sceneLibrary)
        systemProperty("minemod.sceneLibrary", sceneLibrary.get())
    }
}

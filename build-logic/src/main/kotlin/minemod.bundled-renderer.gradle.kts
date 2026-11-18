import java.security.MessageDigest

plugins {
    java
}

val rendererLibrary = providers.gradleProperty("minemod.sceneLibrary")

if (rendererLibrary.isPresent) {
    val hostArchitecture = when (val reported = System.getProperty("os.arch")) {
        "amd64", "x86_64" -> "x86_64"
        "arm64", "aarch64" -> "aarch64"
        else -> reported
    }
    val hostSystem = if (System.getProperty("os.name").startsWith("Windows")) "windows" else "linux"
    val platformDirectory = "natives/$hostSystem-$hostArchitecture"

    val stageRenderer = tasks.register("stageRenderer") {
        group = LifecycleBasePlugin.BUILD_GROUP
        description = "Stages the renderer library next to its SHA-256."
        val builtLibrary = rootDir.resolve(rendererLibrary.get())
        val stagedDirectory = layout.buildDirectory.dir("renderer-library")
        inputs.file(builtLibrary).withPropertyName("builtLibrary")
        inputs.property("platformDirectory", platformDirectory)
        outputs.dir(stagedDirectory)
        doLast {
            val target = stagedDirectory.get().dir(platformDirectory).asFile
            target.deleteRecursively()
            target.mkdirs()
            val contents = builtLibrary.readBytes()
            val digest = MessageDigest.getInstance("SHA-256").digest(contents)
            target.resolve(builtLibrary.name).writeBytes(contents)
            target.resolve("${builtLibrary.name}.sha256").writeText(digest.joinToString("") { "%02x".format(it) })
        }
    }

    tasks.named<Jar>("jar") {
        from(stageRenderer)
    }
}

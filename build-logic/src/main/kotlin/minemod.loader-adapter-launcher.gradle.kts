import groovy.json.JsonOutput
import io.github.luasgraphicss.minemod.buildlogic.adapters.LoaderAdapter
import java.security.MessageDigest

plugins {
    `java-library`
}

val mavenCentral = "https://repo.maven.apache.org/maven2/"

val adapter = the<LoaderAdapter>()
val libs = the<VersionCatalogsExtension>().named("libs")
val profileId = "${adapter.target.release}-minemod-vulkan"
val agentDirectory = "io/github/luasgraphicss/${adapter.artifactName}/${adapter.version}"
val agentPath = "$agentDirectory/${adapter.artifactName}-${adapter.version}.jar"

val launchLibraries = configurations.create("launchLibraries") {
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    launchLibraries(libs.findLibrary("asm").get())
}

val launcherProfile = tasks.register("launcherProfile") {
    description = "Writes the launcher profile that starts the unmodified client with the agent."
    val profile = layout.buildDirectory.file("launcher/$profileId.json")
    val repository = mavenCentral
    val sha1Of = { file: File ->
        MessageDigest.getInstance("SHA-1").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
    }
    val libraries = launchLibraries.incoming.artifacts.resolvedArtifacts.map { artifacts ->
        artifacts.map { artifact ->
            val module = artifact.id.componentIdentifier as ModuleComponentIdentifier
            val path = "${module.group.replace('.', '/')}/${module.module}/${module.version}/${artifact.file.name}"
            mapOf(
                "name" to "${module.group}:${module.module}:${module.version}",
                "downloads" to mapOf(
                    "artifact" to mapOf(
                        "path" to path,
                        "url" to repository + path,
                        "sha1" to sha1Of(artifact.file),
                        "size" to artifact.file.length(),
                    ),
                ),
            )
        }
    }
    val launcher = mapOf(
        "id" to profileId,
        "inheritsFrom" to adapter.target.release,
        "type" to "release",
        "releaseTime" to adapter.target.releaseTime,
        "time" to adapter.target.releaseTime,
        "mainClass" to adapter.target.mainClass,
        "arguments" to mapOf(
            "game" to emptyList<String>(),
            "jvm" to listOf("-javaagent:\${library_directory}/$agentPath"),
        ),
    )
    inputs.property("launcher", launcher)
    inputs.files(launchLibraries)
    outputs.file(profile)
    doLast {
        val document = launcher + mapOf("libraries" to libraries.get())
        profile.get().asFile.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(document)) + "\n")
    }
}

val launcherInstall = tasks.register<Sync>("launcherInstall") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Lays the profile and the agent out as the launcher keeps them in its directory."
    into(layout.buildDirectory.dir("install"))
    from(launcherProfile) {
        into("versions/$profileId")
    }
    from(tasks.jar) {
        into("libraries/$agentDirectory")
    }
}

tasks.assemble {
    dependsOn(launcherInstall)
}

import java.security.MessageDigest

plugins {
    id("fabric-loom")
}

val modVersion: String by project
val gameAdapter = project(":minecraft-adapter")
evaluationDependsOn(gameAdapter.path)
val repositoryRoot = rootDir.resolve("../../..")

val isWindows = System.getProperty("os.name").startsWith("Windows")
val hostArchitecture = when (val reported = System.getProperty("os.arch")) {
    "amd64", "x86_64" -> "x86_64"
    "arm64", "aarch64" -> "aarch64"
    else -> reported
}
val nativePlatform = (if (isWindows) "windows" else "linux") + "-" + hostArchitecture
val nativePreset = if (isWindows) "windows-client" else "linux-client"
val nativeLibraryName = if (isWindows) "minemod_jni.dll" else "libminemod_jni.so"

version = modVersion

base {
    archivesName = rootProject.name
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val bundledRuntime: Configuration by configurations.creating {
    isCanBeConsumed = false
}

dependencies {
    minecraft(targetLibs.minecraft)
    mappings(loom.officialMojangMappings())
    modImplementation(targetLibs.fabric.loader)
    implementation(project(path = gameAdapter.path, configuration = "namedElements"))
    implementation("minemod-vulkan.jvm:client-runtime")
    bundledRuntime("minemod-vulkan.jvm:client-runtime")
}

val stageNativeLibrary by tasks.registering {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stages the JNI library built by the $nativePreset preset next to its SHA-256."
    val builtLibrary = repositoryRoot.resolve("build/$nativePreset/jni/$nativeLibraryName")
    val stagedDirectory = layout.buildDirectory.dir("native-library")
    val platformDirectory = "natives/$nativePlatform"
    val libraryName = nativeLibraryName
    inputs.files(builtLibrary).withPropertyName("builtLibrary").optional()
    inputs.property("platformDirectory", platformDirectory)
    outputs.dir(stagedDirectory)
    doLast {
        val target = stagedDirectory.get().dir(platformDirectory).asFile
        target.deleteRecursively()
        if (!builtLibrary.isFile) {
            throw GradleException("The mod bundles the JNI library and found none at $builtLibrary")
        }
        target.mkdirs()
        val contents = builtLibrary.readBytes()
        val digest = MessageDigest.getInstance("SHA-256").digest(contents)
        target.resolve(libraryName).writeBytes(contents)
        target.resolve("$libraryName.sha256").writeText(digest.joinToString("") { "%02x".format(it) })
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing", "-Werror"))
}

tasks.processResources {
    from(stageNativeLibrary)
    val modProperties = mapOf(
        "version" to project.version.toString(),
        "loaderVersion" to targetLibs.fabric.loader.get().version,
        "minecraftVersion" to targetLibs.minecraft.get().version,
    )
    inputs.properties(modProperties)
    filesMatching("fabric.mod.json") {
        expand(modProperties)
    }
}

tasks.jar {
    dependsOn(gameAdapter.tasks.named("classes"))
    from(provider { gameAdapter.the<SourceSetContainer>()["main"].output })
    from(bundledRuntime.elements.map { jars -> jars.map { zipTree(it) } }) {
        exclude("META-INF/MANIFEST.MF")
    }
    from(repositoryRoot.resolve("LICENSE"))
}

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
val nativeLibraryNames =
    listOf("minemod_jni", "minemod_gl_provider_jni").map { if (isWindows) "$it.dll" else "lib$it.so" }

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
    implementation("minemod-vulkan.compatibility:lwjgl3")
    implementation("minemod-vulkan.adapters:renderer-ownership")
    bundledRuntime("minemod-vulkan.jvm:client-runtime")
    bundledRuntime("minemod-vulkan.compatibility:lwjgl3")
    bundledRuntime("minemod-vulkan.adapters:renderer-ownership")
}

val builtNativeLibraries = nativeLibraryNames.map { repositoryRoot.resolve("build/$nativePreset/jni/$it") }
val jniJavaHome = javaToolchains.compilerFor(java.toolchain).map { it.metadata.installationPath.asFile.absolutePath }

val configureNativeLibrary by tasks.registering(Exec::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Configures the CMake build of the JNI library with the renderer in it."
    workingDir = repositoryRoot
    executable = "cmake"
    args("--preset", nativePreset)
    argumentProviders.add(CommandLineArgumentProvider { listOf("-DMINEMOD_JAVA_HOME=${jniJavaHome.get()}") })
}

val buildNativeLibrary by tasks.registering(Exec::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Builds the JNI libraries that the mod bundles."
    dependsOn(configureNativeLibrary)
    workingDir = repositoryRoot
    commandLine("cmake", "--build", "--preset", nativePreset)
    outputs.files(builtNativeLibraries)
    outputs.upToDateWhen { false }
}

val stageNativeLibrary by tasks.registering {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stages each JNI library next to its SHA-256."
    val builtLibraries = builtNativeLibraries
    val stagedDirectory = layout.buildDirectory.dir("native-library")
    val platformDirectory = "natives/$nativePlatform"
    inputs.files(buildNativeLibrary).withPropertyName("builtLibraries")
    inputs.property("platformDirectory", platformDirectory)
    outputs.dir(stagedDirectory)
    doLast {
        val target = stagedDirectory.get().dir(platformDirectory).asFile
        target.deleteRecursively()
        target.mkdirs()
        builtLibraries.forEach { builtLibrary ->
            val contents = builtLibrary.readBytes()
            val digest = MessageDigest.getInstance("SHA-256").digest(contents)
            target.resolve(builtLibrary.name).writeBytes(contents)
            target.resolve("${builtLibrary.name}.sha256").writeText(digest.joinToString("") { "%02x".format(it) })
        }
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

import io.github.luasgraphicss.minemod.buildlogic.nativeframe.MixinConfig
import io.github.luasgraphicss.minemod.buildlogic.nativeframe.ReleaseTargets
import io.github.luasgraphicss.minemod.buildlogic.nativeframe.names.GameNames
import java.security.MessageDigest
import java.util.Properties

plugins {
    id("fabric-loom")
}

val buildJdk = 21

val release = projectDir.name.removePrefix("mc-")
val repositoryRoot = projectDir.resolve("../../..")
val targets = ReleaseTargets.read(repositoryRoot.resolve("manifests/minecraft-targets.json"))
val target = targets.of(release)
val sharedSources = projectDir.resolve("../native-frame/src")
val gameAdapter = project(":minecraft-adapter")
evaluationDependsOn(gameAdapter.path)
val libs = the<VersionCatalogsExtension>().named("libs")

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

version = Properties()
    .apply { repositoryRoot.resolve("gradle.properties").inputStream().use(::load) }
    .getProperty("modVersion")

base {
    archivesName = rootProject.name
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(buildJdk)
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf(sharedSources.resolve("main/java")))
        resources.setSrcDirs(listOf(sharedSources.resolve("main/resources")))
    }
    test {
        java.setSrcDirs(listOf(sharedSources.resolve("test/java")))
        resources.setSrcDirs(listOf(sharedSources.resolve("test/resources")))
    }
}

val bundledRuntime = configurations.create("bundledRuntime") {
    isCanBeConsumed = false
}

dependencies {
    minecraft("com.mojang:minecraft:$release")
    mappings(GameNames(loom, targets).of(target))
    modImplementation(target.fabricLoader)
    implementation(project(path = gameAdapter.path, configuration = "namedElements"))
    implementation("minemod-vulkan.jvm:client-runtime")
    implementation("minemod-vulkan.compatibility:lwjgl3")
    implementation("minemod-vulkan.adapters:renderer-ownership")
    bundledRuntime("minemod-vulkan.jvm:client-runtime")
    bundledRuntime("minemod-vulkan.compatibility:lwjgl3")
    bundledRuntime("minemod-vulkan.adapters:renderer-ownership")
    testImplementation("net.fabricmc:fabric-loader-junit:${target.fabricLoaderVersion}")
    testImplementation(platform(libs.findLibrary("junit-bom").get()))
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
}

val builtNativeLibraries = nativeLibraryNames.map { repositoryRoot.resolve("build/$nativePreset/jni/$it") }
val jniJavaHome = javaToolchains.compilerFor(java.toolchain).map { it.metadata.installationPath.asFile.absolutePath }

val configureNativeLibrary = tasks.register<Exec>("configureNativeLibrary") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Configures the CMake build of the JNI library with the renderer in it."
    workingDir = repositoryRoot
    executable = "cmake"
    args("--preset", nativePreset)
    argumentProviders.add(CommandLineArgumentProvider { listOf("-DMINEMOD_JAVA_HOME=${jniJavaHome.get()}") })
}

val buildNativeLibrary = tasks.register<Exec>("buildNativeLibrary") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Builds the JNI libraries that the mod bundles."
    dependsOn(configureNativeLibrary)
    workingDir = repositoryRoot
    commandLine("cmake", "--build", "--preset", nativePreset)
    outputs.files(builtNativeLibraries)
    outputs.upToDateWhen { false }
}

val stageNativeLibrary = tasks.register("stageNativeLibrary") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stages each JNI library next to its SHA-256."
    val builtLibraries = builtNativeLibraries
    val stagedDirectory = layout.buildDirectory.dir("native-library")
    val platformDirectory = "natives/$nativePlatform"
    inputs.files(buildNativeLibrary).withPropertyName("builtLibraries")
    inputs.property("platformDirectory", platformDirectory)
    outputs.dir(stagedDirectory)
    doLast {
        val staged = stagedDirectory.get().dir(platformDirectory).asFile
        staged.deleteRecursively()
        staged.mkdirs()
        builtLibraries.forEach { builtLibrary ->
            val contents = builtLibrary.readBytes()
            val digest = MessageDigest.getInstance("SHA-256").digest(contents)
            staged.resolve(builtLibrary.name).writeBytes(contents)
            staged.resolve("${builtLibrary.name}.sha256").writeText(digest.joinToString("") { "%02x".format(it) })
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = target.javaRuntime
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing,-options", "-Werror"))
}

tasks.compileTestJava {
    options.release = buildJdk
}

tasks.processResources {
    from(stageNativeLibrary)
    val modProperties = mapOf(
        "version" to project.version.toString(),
        "loaderVersion" to target.fabricLoaderVersion,
        "minecraftVersion" to release,
        "javaVersion" to target.javaRuntime.toString(),
        "mixinConfig" to MixinConfig.fileNameOf(release),
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

tasks.test {
    description = "Applies every mixin of the game adapter to the game of this release."
    useJUnitPlatform()
    val gameDirectory = layout.buildDirectory.dir("mixin-application")
    workingDir = gameDirectory.get().asFile
    systemProperty("minemod.mixinConfig", MixinConfig.fileNameOf(release))
    doFirst {
        gameDirectory.get().asFile.mkdirs()
    }
}

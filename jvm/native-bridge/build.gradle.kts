plugins {
    id("minemod.runtime-library")
}

dependencies {
    api(project(":jvm:render-contract"))
    testImplementation(project(":jvm:command-codec"))
    testImplementation(testFixtures(project(":jvm:command-codec")))
}

val isWindows = System.getProperty("os.name").startsWith("Windows")
val nativePreset = if (isWindows) "windows-jni" else "linux-jni"
val nativeLibraryName = if (isWindows) "minemod_jni.dll" else "libminemod_jni.so"
val nativeLibrary = rootProject.layout.projectDirectory.file("build/$nativePreset/jni/$nativeLibraryName")
val jniJavaHome = javaToolchains.compilerFor(java.toolchain).map { it.metadata.installationPath.asFile.absolutePath }

val configureNativeLibrary = tasks.register<Exec>("configureNativeLibrary") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Configures the CMake build of the JNI library."
    workingDir = rootDir
    executable = "cmake"
    args("--preset", nativePreset)
    argumentProviders.add(CommandLineArgumentProvider { listOf("-DMINEMOD_JAVA_HOME=${jniJavaHome.get()}") })
}

val buildNativeLibrary = tasks.register<Exec>("buildNativeLibrary") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Builds the JNI library that the bridge tests load."
    dependsOn(configureNativeLibrary)
    workingDir = rootDir
    commandLine("cmake", "--build", "--preset", nativePreset)
    outputs.file(nativeLibrary)
    outputs.upToDateWhen { false }
}

tasks.withType<Test>().configureEach {
    val corpus = rootProject.layout.projectDirectory.dir("tests/fixtures/command-streams")
    inputs.dir(corpus).withPropertyName("commandStreamCorpus").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(buildNativeLibrary).withPropertyName("nativeLibrary")
    systemProperty("minemod.commandStreamCorpus", corpus.asFile.absolutePath)
    systemProperty("minemod.nativeLibrary", nativeLibrary.asFile.absolutePath)
}

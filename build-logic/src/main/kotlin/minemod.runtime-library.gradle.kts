plugins {
    `java-library`
}

val buildJdk = 21
val bytecodeRelease = 8
val additionalRuntimes = listOf(16, 17, 21)
val forbiddenRuntimeGroups = listOf(
    "org.lwjgl",
    "com.mojang",
    "net.minecraft",
    "net.minecraftforge",
    "net.fabricmc",
    "net.neoforged",
)

val libs = the<VersionCatalogsExtension>().named("libs")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(buildJdk)
    }
}

dependencies {
    "testImplementation"(platform(libs.findLibrary("junit-bom").get()))
    "testImplementation"(libs.findLibrary("junit-jupiter").get())
    "testRuntimeOnly"(libs.findLibrary("junit-platform-launcher").get())
}

tasks.withType<JavaCompile>().configureEach {
    options.release = bytecodeRelease
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all,-options", "-Werror"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.test {
    description = "Runs the tests on a Java $bytecodeRelease runtime."
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(bytecodeRelease) }
}

val runtimeTests = additionalRuntimes.map { runtime ->
    tasks.register<Test>("testOnJava$runtime") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Runs the tests on a Java $runtime runtime."
        testClassesDirs = sourceSets.test.get().output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
        javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(runtime) }
    }
}

val runtimeComponents = configurations.runtimeClasspath.flatMap { classpath ->
    classpath.incoming.artifacts.resolvedArtifacts.map { artifacts ->
        artifacts.map { it.id.componentIdentifier.displayName }
    }
}

val verifyRuntimeIsolation = tasks.register("verifyRuntimeIsolation") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Fails when the runtime classpath reaches Minecraft, a mod loader or LWJGL."
    inputs.property("runtimeComponents", runtimeComponents)
    inputs.property("forbiddenRuntimeGroups", forbiddenRuntimeGroups)
    doLast {
        val leaked = runtimeComponents.get().filter { component ->
            forbiddenRuntimeGroups.any { group -> component.startsWith("$group:") || component.startsWith("$group.") }
        }
        if (leaked.isNotEmpty()) {
            throw GradleException("The runtime classpath must not reach game, loader or LWJGL libraries: $leaked")
        }
    }
}

tasks.check {
    dependsOn(runtimeTests, verifyRuntimeIsolation)
}

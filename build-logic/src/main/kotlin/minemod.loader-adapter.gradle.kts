import io.github.luasgraphicss.minemod.buildlogic.adapters.AdapterResources
import io.github.luasgraphicss.minemod.buildlogic.adapters.FramePlanStandIns
import io.github.luasgraphicss.minemod.buildlogic.adapters.LaunchIntegrations
import io.github.luasgraphicss.minemod.buildlogic.adapters.LoaderAdapter
import io.github.luasgraphicss.minemod.buildlogic.adapters.LoaderTarget
import java.util.zip.ZipFile

plugins {
    `java-library`
    id("minemod.bundled-renderer")
}

val buildJdk = 21
val bytecodeRelease = 8

val family = project.parent!!.name
val release = name.removePrefix("mc-")
val target = LoaderTarget.read(rootDir.resolve("manifests/minecraft-targets.json"), family, release)
val drawsFrame = layout.projectDirectory.file("src/main/resources/${AdapterResources.FRAME_PLAN}").asFile.isFile
val launch = LaunchIntegrations.of(target, drawsFrame)
val adapter = LoaderAdapter(
    target = target,
    launch = launch,
    artifactName = "minemod-vulkan-$family-$release",
    version = providers.gradleProperty("modVersion").get(),
)

extensions.add("loaderAdapter", adapter)

version = adapter.version

base {
    archivesName = adapter.artifactName
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(buildJdk)
    }
}

val bundledLaunch = configurations.create("bundledLaunch") {
    isCanBeConsumed = false
}

dependencies {
    bundledLaunch(project(":adapters:loaders:${launch.module}"))
}

tasks.withType<JavaCompile>().configureEach {
    options.release = bytecodeRelease
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all,-options", "-Werror"))
}

tasks.jar {
    from(bundledLaunch.elements.map { jars -> jars.map { zipTree(it) } }) {
        exclude("META-INF/MANIFEST.MF")
    }
    from(rootDir.resolve("LICENSE"))
    manifest {
        attributes(launch.manifestAttributes)
    }
}

val verifyLaunchEntries = tasks.register("verifyLaunchEntries") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Fails when the jar lacks a plan, the classes its calls go to or what its loader starts it from."
    val adapterJar = tasks.jar.flatMap { it.archiveFile }
    val requiredEntries = launch.requiredEntries
    inputs.file(adapterJar)
    inputs.property("requiredEntries", requiredEntries)
    doLast {
        val missing = ZipFile(adapterJar.get().asFile).use { jar ->
            val framePlan = jar.getEntry(AdapterResources.FRAME_PLAN)
            val standIns = framePlan?.let { jar.getInputStream(it).use(FramePlanStandIns::classEntriesOf) }.orEmpty()
            (requiredEntries + standIns).filter { jar.getEntry(it) == null }
        }
        if (missing.isNotEmpty()) {
            throw GradleException("${adapterJar.get().asFile.name} lacks $missing")
        }
    }
}

tasks.check {
    dependsOn(verifyLaunchEntries)
}

launch.packagingPlugin?.let { pluginManager.apply(it) }

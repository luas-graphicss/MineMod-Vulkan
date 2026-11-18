import net.fabricmc.loom.task.prod.ClientProductionRunTask

plugins {
    id("fabric-loom")
}

val modVersion: String by project

version = modVersion

base {
    archivesName = rootProject.name
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    minecraft(targetLibs.minecraft)
    mappings(loom.officialMojangMappings())
    modImplementation(targetLibs.fabric.loader)
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing", "-Werror"))
}

tasks.processResources {
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

val journeyPlan = providers.gradleProperty("journey.plan")
val journeyOutput = providers.gradleProperty("journey.output")
val candidateMod = providers.gradleProperty("journey.mod")

fun ClientProductionRunTask.runsTheJourney() {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    runDir = layout.dir(journeyOutput.map { file(it).resolve("game") })
    jvmArgs.add(journeyPlan.map { "-Dminemod.journey.plan=$it" })
    jvmArgs.add(journeyOutput.map { "-Dminemod.journey.output=$it" })
}

tasks.register<ClientProductionRunTask>("runReference") {
    description = "Runs a journey on the game with the OpenGL renderer it ships."
    runsTheJourney()
}

tasks.register<ClientProductionRunTask>("runCandidate") {
    description = "Runs a journey on the game with the MineMod Vulkan mod."
    runsTheJourney()
    mods.from(candidateMod)
}

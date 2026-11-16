plugins {
    id("minemod.runtime-library")
    `java-test-fixtures`
}

dependencies {
    api(project(":jvm:render-contract"))
}

tasks.withType<Test>().configureEach {
    val corpus = rootProject.layout.projectDirectory.dir("tests/fixtures/command-streams")
    inputs.dir(corpus).withPropertyName("commandStreamCorpus").withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("minemod.commandStreamCorpus", corpus.asFile.absolutePath)
}

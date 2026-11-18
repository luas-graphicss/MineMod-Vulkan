plugins {
    id("minemod.runtime-library")
    `java-test-fixtures`
}

dependencies {
    api(project(":adapters:presentation:core"))
    api(project(":jvm:client-runtime"))
}

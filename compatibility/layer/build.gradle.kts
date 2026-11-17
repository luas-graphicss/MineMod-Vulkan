plugins {
    id("minemod.runtime-library")
    `java-test-fixtures`
}

dependencies {
    api(project(":compatibility:translation"))
    api(project(":compatibility:interop"))
}

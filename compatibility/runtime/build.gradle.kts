plugins {
    id("minemod.runtime-library")
    id("minemod.provider-tests")
    `java-test-fixtures`
}

dependencies {
    api(project(":compatibility:natives"))
}

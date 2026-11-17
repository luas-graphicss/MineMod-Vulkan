plugins {
    id("minemod.runtime-library")
}

dependencies {
    api(project(":compatibility:contexts"))
    api(project(":compatibility:layer"))
}

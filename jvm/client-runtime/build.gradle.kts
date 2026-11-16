plugins {
    id("minemod.runtime-library")
}

dependencies {
    api(project(":jvm:command-codec"))
    api(project(":jvm:native-bridge"))
}

plugins {
    id("minemod.runtime-library")
}

dependencies {
    api(project(":compatibility:translation"))
    api(project(":compatibility:interop"))
}

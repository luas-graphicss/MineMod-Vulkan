plugins {
    id("minemod.runtime-library")
}

dependencies {
    api(project(":compatibility:contracts"))
    api(project(":compatibility:provider"))
}

plugins {
    id("minemod.runtime-library")
}

dependencies {
    api(project(":compatibility:dispatch"))
    api(project(":compatibility:resources"))
    api(project(":compatibility:state"))
}

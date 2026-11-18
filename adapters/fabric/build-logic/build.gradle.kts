plugins {
    `kotlin-dsl`
}

val loomVersion: String by project
val foojayResolverVersion: String by project
val mappingIoVersion: String by project

dependencies {
    implementation("net.fabricmc:fabric-loom:$loomVersion")
    implementation("org.gradle.toolchains:foojay-resolver:$foojayResolverVersion")
    implementation("net.fabricmc:mapping-io:$mappingIoVersion")
}

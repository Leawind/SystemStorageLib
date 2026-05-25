plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.3.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://libraries.minecraft.net")
}


val bundleSlf4j = false

val shadowBundle: Configuration by configurations.creating

fun DependencyHandlerScope.shadow(dependencyNotation: String) {
    implementation(dependencyNotation)
    add("shadowBundle", dependencyNotation)
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.slf4j:slf4j-simple:2.0.17")


    shadow("com.github.Leawind:inventory-java:0.1.1")
    shadow("dev.dirs:directories:26")

    compileOnly("org.jspecify:jspecify:1.0.0")

    implementation("com.mojang:datafixerupper:4.0.26")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("com.google.jimfs:jimfs:1.3.0")
}

tasks.withType<AbstractArchiveTask> {
    archiveBaseName = "${project.property("mod.id")}"
    archiveVersion = "${project.property("mod.version")}+mc${project.property("mod.minecraftVersion")}"
}

tasks.shadowJar {
    configurations = listOf(shadowBundle)
    archiveClassifier = ""

    minimize()

    relocate("io.github.leawind.inventory", "io.github.leawind.systemstoragelib.lib.inventory")

    // directories
    relocate("dev.dirs", "io.github.leawind.systemstoragelib.lib.dirs")
    exclude("META-INF/native-image/**")
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    val props = project.properties
        .filterKeys { it.startsWith("mod.") }
        .mapKeys { it.key.removePrefix("mod.") }

    inputs.properties(props)
    filesMatching(
        listOf(
            "fabric.mod.json",
            "META-INF/mods.toml",
            "META-INF/neoforge.mods.toml"
        )
    ) {
        expand(props)
    }
}

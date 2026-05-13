plugins {
    id("java-library")
    id("com.gradleup.shadow") version "8.3.10"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("org.jspecify:jspecify:1.0.0")

    compileOnly("org.slf4j:slf4j-api:2.0.17")
    testImplementation("org.slf4j:slf4j-simple:2.0.17")

    implementation("com.github.Leawind:inventory-java:5b9aca4746")
    implementation("dev.dirs:directories:26")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    configurations = listOf(project.configurations.runtimeClasspath.get())

    minimize()

    relocate("io.github.leawind.inventory", "io.github.leawind.systemstoragelib.lib.inventory")
    relocate("dev.dirs", "io.github.leawind.systemstoragelib.lib.dirs")

    // Exclude license files and metadata from bundled libraries to avoid confusion
    exclude("META-INF/native-image/**") // dev.dirs:directories

    archiveClassifier.set("all")
}

val shadowJarOutput: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = true
    outgoing {
        artifact(tasks.shadowJar)
    }
}

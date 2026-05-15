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
    maven("https://libraries.minecraft.net")
}

dependencies {
    compileOnly("org.jspecify:jspecify:1.0.0")

    compileOnly("org.slf4j:slf4j-api:2.0.17")
    testImplementation("org.slf4j:slf4j-simple:2.0.17")

    implementation("com.github.Leawind:inventory-java:498a483d63")
    implementation("dev.dirs:directories:26")
    implementation("com.mojang:datafixerupper:9.0.19") // gson, guava, fastutil

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveClassifier.set("all")
    configurations = listOf(project.configurations.runtimeClasspath.get())

    minimize()

    relocate("io.github.leawind.inventory", "io.github.leawind.systemstoragelib.lib.inventory")

    relocate("dev", "io.github.leawind.systemstoragelib.lib.dev") // directories
    relocate("com", "io.github.leawind.systemstoragelib.lib.com") // gson, datafixerupper
    relocate("javax", "io.github.leawind.systemstoragelib.lib.javax") // datafixerupper
    relocate("org", "io.github.leawind.systemstoragelib.lib.org") // datafixerupper
    relocate("it", "io.github.leawind.systemstoragelib.lib.it") // datafixerupper

    exclude("META-INF/native-image/**") // directories
    exclude("META-INF/maven/**") // gson
    exclude("META-INF/proguard/**") // gson
    exclude("com/google/errorprone/**") // gson
    exclude("META-INF/LICENSE*") // datafixerupper
}

val shadowJarOutput: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = true
    outgoing {
        artifact(tasks.shadowJar)
    }
}

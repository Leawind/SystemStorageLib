import gg.meza.stonecraft.mod
import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("com.gradleup.shadow") version "8.3.10"
    id("gg.meza.stonecraft")
}

modSettings {
    clientOptions {
        // https://minecraft.wiki/w/Options.txt
        fov = 90
        guiScale = 2
        narrator = false
        musicVolume = 0.0
        guiScale = 3

        additionalLines = mapOf(
            "maxFps" to "60",
            "renderDistance" to "8",
            "simulationDistance" to "5",
            "mouseSensitivity" to "0.22"
        )
    }
}


val shadowBundle: Configuration by configurations.creating

fun DependencyHandlerScope.shadow(dependencyNotation: String) {
    implementation(dependencyNotation)
    add("shadowBundle", dependencyNotation)
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    shadow("com.github.Leawind:inventory-java:5b9aca4746")
    shadow("dev.dirs:directories:26")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    compileOnly("org.jspecify:jspecify:1.0.0")
}

tasks.shadowJar {
    dependsOn(tasks.processResources)
    tasks.findByName("generatePackMCMetaJson")?.let { dependsOn(it) }

    configurations = listOf(shadowBundle)

    if (tasks.findByName("remapJar") == null) {
        println("remapJar: null, project.name=${project.name}, project.version=${project.version}")
        archiveClassifier.set("")
    } else {
        println("remapJar: exists, project.name=${project.name}, project.version=${project.version}")
        archiveClassifier.set("shadow")
    }

    relocate("io.github.leawind.inventory", "io.github.leawind.systemstoragelib.lib.inventory")
    relocate("dev.dirs", "io.github.leawind.systemstoragelib.lib.dirs") // dev.dirs:directories

    // Exclude license files and metadata from bundled libraries to avoid confusion
    exclude("META-INF/native-image/**") // dev.dirs:directories

    minimize()
}

tasks.withType<RemapJarTask>().matching { it.name == "remapJar" }.configureEach {
    dependsOn(tasks.shadowJar)
    inputFile.set(tasks.shadowJar.flatMap { it.archiveFile })
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
}

if (mod.isForge) {
    tasks.compileTestJava {
        dependsOn("generatePackMCMetaJson")
    }
}

publishMods {
    modrinth {
        if (mod.isFabric) {
            requires("fabric-api")
            optional("modmenu")
        }
    }

    curseforge {
        clientRequired = false
        serverRequired = false
        if (mod.isFabric) requires("fabric-api")
    }
}

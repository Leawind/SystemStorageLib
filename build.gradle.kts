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

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(project(":core"))

    add("shadowBundle", project(":core", configuration = "shadowJarOutput"))
}

tasks.shadowJar {
    dependsOn(tasks.processResources)
    tasks.findByName("generatePackMCMetaJson")?.let { dependsOn(it) }

    configurations = listOf(shadowBundle)

    println("remapJar: ${tasks.findByName("remapJar")}, project.name=${project.name}, project.version=${project.version}")
    if (tasks.findByName("remapJar") == null) {
        archiveClassifier.set("")
    } else {
        archiveClassifier.set("shadow")
    }

}

tasks.withType<RemapJarTask>().matching { it.name == "remapJar" }.configureEach {
    dependsOn(tasks.shadowJar)
    inputFile.set(tasks.shadowJar.flatMap { it.archiveFile })
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
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

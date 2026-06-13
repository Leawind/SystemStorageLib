import java.io.FilterReader
import java.io.Reader
import java.io.StringReader

plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.3.1"
    `maven-publish`
}

group = "${project.property("mod.group")}"
version = "${project.property("mod.version")}"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://libraries.minecraft.net")
}

val shadowBundle: Configuration by configurations.creating
fun DependencyHandlerScope.shadowBundle(dependencyNotation: String) {
    implementation(dependencyNotation)
    add("shadowBundle", dependencyNotation)
}

dependencies {
    shadowBundle("com.github.Leawind:inventory-java:0.4.0")
    shadowBundle("dev.dirs:directories:26")

    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("org.jetbrains:annotations:24.0.1")

    compileOnly("org.slf4j:slf4j-api:2.0.17")
    testImplementation("org.slf4j:slf4j-simple:2.0.17")

    implementation("com.mojang:datafixerupper:4.0.26")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.3")

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


class EmbedMarkdownFilter(reader: Reader) : FilterReader(reader) {
    var sourceFile: File? = null
    private var resolvedReader: Reader? = null

    private fun getResolvedReader(): Reader {
        if (resolvedReader == null) {
            val file = sourceFile ?: error("sourceFile is not set for EmbedMarkdownFilter")
            resolvedReader = StringReader(resolveEmbeds(file))
        }
        return resolvedReader!!
    }

    override fun read(cbuf: CharArray, off: Int, len: Int): Int = getResolvedReader().read(cbuf, off, len)
    override fun read(): Int = getResolvedReader().read()

    private fun resolveEmbeds(file: File): String {
        return file.readText(Charsets.UTF_8).replace(Regex("""\{\[embed\]\((.*?)\)\}""")) { match ->
            val relativePath = match.groupValues[1].trim()
            val targetFile = File(file.parentFile, relativePath)

            if (targetFile.exists() && targetFile.isFile) {
                resolveEmbeds(targetFile)
            } else {
                match.value
            }
        }
    }
}

tasks.named<Copy>("processResources") {
    filesMatching("**/*.md") {
        filter(mapOf("sourceFile" to file), EmbedMarkdownFilter::class.java)
    }
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

    from(rootProject.file("LICENSE")) { into("META-INF") }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("${project.property("mod.name")}")
                description.set("${project.property("mod.description")}")
                url.set("${project.property("mod.sourceUrl")}")
            }
        }
    }
    repositories {
        mavenLocal()
    }
}

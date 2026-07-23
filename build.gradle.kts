import me.modmuss50.mpp.ReleaseType
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.tasks.SourceSetContainer

plugins {
    // plugin versions are defined in stonecutter.gradle.kts
    id("net.fabricmc.fabric-loom") apply false
    id("net.fabricmc.fabric-loom-remap") apply false
    id("me.modmuss50.mod-publish-plugin")
}

val legacyMappedMinecraft = sc.current.project == "1.20.1"
pluginManager.apply(if (legacyMappedMinecraft) "net.fabricmc.fabric-loom-remap" else "net.fabricmc.fabric-loom")

version = "${project.property("mod_version")}+${sc.current.project}"
group = project.property("maven_group") as String

base {
    archivesName = project.property("archives_base_name") as String
}

repositories {
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1") { name = "DevAuth" }
    maven("https://maven.terraformersmc.com/releases/") { name = "Terraformers" }
}

dependencies {
    add("minecraft", "com.mojang:minecraft:${sc.current.version}")

    if (legacyMappedMinecraft) {
        add("modImplementation", "net.fabricmc:fabric-loader:${project.property("loader_version")}")
        add("mappings", "net.fabricmc:yarn:1.20.1+build.10:v2")
        add("modImplementation", "net.fabricmc.fabric-api:fabric-api:${property("dependencies.fabric")}")
        add("compileOnly", "com.google.code.findbugs:jsr305:3.0.2")
    } else {
        add("implementation", "net.fabricmc:fabric-loader:${project.property("loader_version")}")
        add("implementation", platform("net.fabricmc.fabric-api:fabric-api-bom:${property("dependencies.fabric")}"))
        add("implementation", "net.fabricmc.fabric-api:fabric-networking-api-v1")
        add("implementation", "net.fabricmc.fabric-api:fabric-key-mapping-api-v1")
        add("implementation", "net.fabricmc.fabric-api:fabric-lifecycle-events-v1")
        add("implementation", "net.fabricmc.fabric-api:fabric-command-api-v2")
        add("implementation", "net.fabricmc.fabric-api:fabric-rendering-v1")
        add("implementation", "net.fabricmc.fabric-api:fabric-resource-loader-v1")
        add("runtimeOnly", "net.fabricmc.fabric-api:fabric-registry-sync-v0")
    }

    // Allow logging into an actual Minecraft account in a dev env
    // See https://github.com/DJtheRedstoner/DevAuth
    add("localRuntime", "me.djtheredstoner:DevAuth-fabric:1.2.2")

    val modmenu: String = sc.properties["dependencies.modmenu"]
    add("compileOnly", "com.terraformersmc:modmenu:${modmenu}")
    if(sc.properties["debug.load_modmenu"]) {
        add("localRuntime", "com.terraformersmc:modmenu:${modmenu}")
        add("localRuntime", "net.fabricmc.fabric-api:fabric-screen-api-v1")
    }
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version as String,
        "minecraft_dependency" to sc.properties["dependencies.minecraft"],
        "java_dependency" to if (legacyMappedMinecraft) ">=17" else ">=25",
    )

    inputs.properties(props)
    filesMatching("fabric.mod.json") {
        expand(props)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = if (legacyMappedMinecraft) 17 else 25
}

if (legacyMappedMinecraft) {
    extensions.configure<SourceSetContainer> {
        named("main") {
            java.setSrcDirs(listOf(rootProject.file("src/legacy1201/java")))
            resources.setSrcDirs(listOf(rootProject.file("src/legacy1201/resources")))
        }
    }
}

java {
    val targetVersion = if (legacyMappedMinecraft) JavaVersion.VERSION_17 else JavaVersion.VERSION_25
    sourceCompatibility = targetVersion
    targetCompatibility = targetVersion
}

extensions.configure<LoomGradleExtensionAPI>("loom") {
    decompilers {
        named("vineflower") {
            options.put("mark-corresponding-synthetics", "1")
        }
    }

    runConfigs.configureEach {
        ideConfigGenerated(stonecutter.current.isActive)
        // by default loom will use versions/*/run for the run dir, so instead tell it to use the
        // run dir in the project root directory
        runDir = "../../run"
    }

    if (!legacyMappedMinecraft) {
        accessWidenerPath = sc.process(rootProject.file("src/main/resources/wildfire_gender.accesswidener"), "build/dev.aw")
    }
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.property("archives_base_name")}" }
    }
}

publishMods {
    val modVer: String = sc.properties["mod_version"]
    val minVer: String = sc.properties["publish.min_version"]
    val maxVer: String = sc.properties["publish.max_version"]
    val verTitle: String = sc.properties["publish.version_title"]

    file = tasks.jar.get().archiveFile
    displayName = "$modVer for $verTitle"
    version = project.version as String
    changelog = providers.fileContents(rootProject.layout.projectDirectory.file("CHANGELOG.md")).asText
    type = ReleaseType.of(sc.properties["publish.type"])
    modLoaders.add("fabric")

    dryRun = providers.environmentVariable("MODRINTH_TOKEN").getOrNull() == null

    modrinth {
        projectId = property("publish.modrinth").toString()
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersionRange {
            start = minVer
            end = maxVer
        }
        requires("fabric-api")
    }
}

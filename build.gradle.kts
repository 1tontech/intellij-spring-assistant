import org.gradle.api.JavaVersion.VERSION_11
import org.jetbrains.changelog.date
import org.jetbrains.changelog.markdownToHTML

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    java
    id("org.jetbrains.intellij") version "1.3.1"
    id("org.jetbrains.changelog") version "1.3.1"
    id("io.freefair.lombok") version "6.3.0"
}

java {
    sourceCompatibility = VERSION_11
}

group = "dev.flikas"
version = "0.13.0-eap1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.commons", "commons-collections4", "4.4")
    implementation("com.miguelfonseca.completely", "completely-core", "0.9.0")

    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.8.1")
    testImplementation("org.mockito", "mockito-core", "2.12.0")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.8.1")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    type.set("IC")
    version.set("2019.3.1")
    sameSinceUntilBuild.set(false)
    plugins.set(listOf("properties", "yaml", "maven", "gradle", "com.intellij.java"))
    downloadSources.set(true)
}

changelog {
    header.set(provider { "[${version.get()}] - ${date()}" })
}

tasks {
    patchPluginXml {
        sinceBuild.set("193.5622.53")
        version.set(
            project.version.toString().run {
                val pieces = split('-')
                if (pieces.size > 1) {
                    //if this is not a release version, generate a sub version number from count of minutes from 2021-10-01.
                    pieces[0] + "." + (System.currentTimeMillis() / 1000 - 1633046400) / 60
                } else {
                    pieces[0]
                }
            }
        )

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(
            projectDir.resolve("README.md").readText().lines().run {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end))
            }.joinToString(
                separator = "\n",
                postfix = "\nProject [document](https://github.com/flikas/idea-spring-boot-assistant/#readme)\n"
            ).run { markdownToHTML(this) }
        )

        changeNotes.set(provider {
            changelog.run {
                getOrNull(version.get()) ?: getLatest()
            }.toHTML()
        })
    }

    signPlugin {
        cliVersion.set("0.1.8")
        val chain = rootProject.file("chain.crt")
        if (chain.exists()) {
            certificateChainFile.set(chain)
        } else {
            certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        }
        val private = rootProject.file("private.pem")
        if (private.exists()) {
            privateKeyFile.set(rootProject.file("private.pem"))
        } else {
            privateKey.set(System.getenv("PRIVATE_KEY"))
        }
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        if (!version.toString().contains('-')) {
            dependsOn("patchChangelog")
        }
        token.set(System.getenv("PUBLISH_TOKEN"))
        channels.set(listOf(version.toString().split('-').getOrElse(1) { "default" }.split('.').first()))
    }
}
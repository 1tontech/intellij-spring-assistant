import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import org.gradle.api.JavaVersion.VERSION_1_8
import java.nio.charset.StandardCharsets.UTF_8

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
//        classpath("org.junit.platform:junit-platform-gradle-plugin:1.0.1")
//        classpath("org.codehaus.groovy:groovy-all:2.4.13")
        classpath("com.vladsch.flexmark:flexmark:0.28.12")
    }
}

plugins {
    java
    id("org.jetbrains.intellij") version "1.2.0"
    id("io.freefair.lombok") version "6.2.0"
}

java {
    sourceCompatibility = VERSION_1_8
}

group = "dev.flikas"
version = "0.2.2"

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

fun readmeXmlAsHtml(): String {
    val parser = Parser.builder().build()
    val renderer = HtmlRenderer.builder().build()
    var readmeContent = rootProject.file("README.md").readText(UTF_8)
    // since these images needs to shown from within intellij, lest put absolute urls so that the images & changelog will be visible
    readmeContent = readmeContent.replace("![Plugin in action](help.gif)", "")
    readmeContent = readmeContent.replace(
        "CHANGELOG.md",
        "https://github.com/flikas/idea-spring-boot-assistant/blob/" + version + "/CHANGELOG.md"
    )
    val readmeDocument = parser.parse(readmeContent)
    return renderer.render(readmeDocument)
}

fun changeLogAsHtml(): String {
    val parser = Parser.builder().build()
    val renderer = HtmlRenderer.builder().build()
    val changeLogDocument = parser.parse(rootProject.file("CHANGELOG.md").readText(UTF_8))
    return renderer.render(changeLogDocument)
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    type.set("IC")
    version.set("2019.3")
    plugins.addAll("properties", "yaml", "maven", "gradle", "com.intellij.java")
    downloadSources.set(true)
}

tasks.patchPluginXml {
    sinceBuild.set("193.5233.102")
    untilBuild.set("213.4928.*")
    pluginDescription.set(readmeXmlAsHtml())
//    changeNotes = changeLogAsHtml()
}

tasks.signPlugin {
    certificateChainFile.set(rootProject.file("chain.crt"))
    privateKeyFile.set(rootProject.file("private.pem"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
}

tasks.publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
//    channels = ["eap", "nightly", "default"]
}
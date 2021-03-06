import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.21"
    id("org.jetbrains.compose") version "1.1.0"
}

group = "hoonkun.kiwi"
version = "1.0"

repositories {
    google()

    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    mavenCentral()

    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    mavenLocal()
}

@OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.5.31")
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.4.2")
    testImplementation("org.spigotmc:spigot:1.19-R0.1-SNAPSHOT")
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    implementation("io.ktor:ktor-client-core:2.0.3")
    implementation("io.ktor:ktor-client-cio:2.0.3")
    testImplementation(compose.uiTestJUnit4)
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.RequiresOptIn")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "16"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Exe, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "doodler"
            packageVersion = "1.0.0"
        }
    }
}
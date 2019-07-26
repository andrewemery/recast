import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
//    id("maven-publish")
}

group = "com.andrewemery.recast"
version = "0.0.1"

repositories {
    jcenter()
}

kotlin {
    jvm()
    iosArm64 { binaries { framework { freeCompilerArgs.add("-Xobjc-generics") } } }
    iosX64 { binaries { framework { freeCompilerArgs.add("-Xobjc-generics") } } }

    sourceSets {

        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(coroutines("core-common"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation(coroutines("core"))
            }
        }

        val jvmTest by getting {
            dependsOn(commonTest)
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }

        val iosArm64Main by getting {
            kotlin.srcDirs("src/iosMain/kotlin")
            dependsOn(commonMain)
            dependencies {
                implementation(coroutines("core-native"))
            }
        }

        val iosArm64Test by getting {
            kotlin.srcDirs("src/iosTest/kotlin")
            dependsOn(commonTest)
        }

        val iosX64Main by getting {
            kotlin.srcDirs("src/iosMain/kotlin")
            dependsOn(commonMain)
            dependencies {
                implementation(coroutines("core-native"))
            }
        }

        val iosX64Test by getting {
            kotlin.srcDirs("src/iosTest/kotlin")
            dependsOn(commonTest)
        }
    }
}

task("iosTest") {
    val device = project.findProperty("device")?.toString() ?: "iPhone 8"
    dependsOn("linkDebugTestIosX64")
    group = JavaBasePlugin.VERIFICATION_GROUP
    description = "Execute unit tests on ${device} simulator"
    doLast {
        val target = kotlin.targets.getByName("iosX64") as KotlinNativeTarget
        val binary = target.binaries.getTest("DEBUG")
        exec { commandLine("xcrun", "simctl", "spawn", device, binary.outputFile) }
    }
}

fun coroutines(moduleName: String): String =
    kotlinx("coroutines-$moduleName", "1.2.2")

fun kotlinx(moduleName: String, version: String): String =
    "org.jetbrains.kotlinx:kotlinx-$moduleName:$version"

 //fun coroutines(moduleName: String) = build.Build.coroutines(moduleName)
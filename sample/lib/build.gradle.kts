plugins {
    kotlin("multiplatform")
    kotlin("kapt")
}

group = "com.andrewemery.recast.sample"
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
                implementation(project(":lib"))
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

dependencies {
    configurations.get("kapt").dependencies.add(project(":kapt"))
}

//task("iosSyncFramework", Sync::class) {
//    val buildType = (project.findProperty("build_type") as String?)?.toLowerCase() ?: "debug"
//    val platform = (project.findProperty("platform") as String?)?.toLowerCase() ?: "iphonesimulator"
//    val target = (if (platform.contains("simulator")) kotlin.targets.getByName("iosX64") else kotlin.targets.getByName("iosArm64")) as KotlinNativeTarget
//    val framework = target.binaries.getFramework("", buildType)
//    inputs.property("mode", buildType.toUpperCase())
//    dependsOn(framework.linkTask)
//    var destination = project.findProperty("destination")
//    if (destination == null) destination = "../ios/frameworks"
//    println(buildType + " " + platform + " " + destination)
//    from(framework.outputFile.parentFile)
//    into(destination)
//}
//
//task("iosTest") {
//    val device = project.findProperty("device")?.toString() ?: "iPhone 8"
//    dependsOn("linkDebugTestIosX64")
//    group = JavaBasePlugin.VERIFICATION_GROUP
//    description = "Execute unit tests on ${device} simulator"
//    doLast {
//        val target = kotlin.targets.getByName("iosX64") as KotlinNativeTarget
//        val binary = target.binaries.getTest("DEBUG")
//        exec { commandLine("xcrun", "simctl", "spawn", device, binary.outputFile) }
//    }
//}

fun coroutines(moduleName: String): String =
    kotlinx("coroutines-$moduleName", "1.2.2")

fun kotlinx(moduleName: String, version: String): String =
    "org.jetbrains.kotlinx:kotlinx-$moduleName:$version"

 //fun coroutines(moduleName: String) = build.Build.coroutines(moduleName)
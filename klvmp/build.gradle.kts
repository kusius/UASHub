import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "io.kusius"
version = "1.0.0"

// This task is used on JVM platforms (except android).
// Builds the native C/C++ libraries we use for KLV parsing and
// MPEG-TS demultiplexing
val currentOs: OperatingSystem = DefaultNativePlatform.getCurrentOperatingSystem().internalOs
tasks.create<Exec>("buildJniNative") {
    group = "build"

    inputs.dir("src/nativeInterop/cinterop")
    outputs.dir("src/nativeInterop/cinterop")

    workingDir = file("src/nativeInterop/cinterop/")
    environment("TARGET", currentOs.nativePrefix)
    commandLine("./build.sh")
}

compose.resources {
    // Copy the native shared objects into our desktop compose resources
    // so that they will be packaged with our application and retrieved at runtime
    copy {
        include("lib*.*")
        from("src/nativeInterop/cinterop/build/${currentOs.nativePrefix}")
        into(layout.projectDirectory.dir("src/desktopMain/composeResources/files"))
    }
}

kotlin {
    jvm("desktop") {
        // Before processing resources we build the native libraries
        val processResources = compilations["main"].processResourcesTaskName
        (tasks[processResources] as ProcessResources).apply {
            dependsOn("buildJniNative")
        }
    }

    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                //put your multiplatform dependencies here
                implementation(compose.runtime)
                implementation(compose.components.resources)
            }
        }

        val jvmMain by creating {
            dependsOn(commonMain)
        }


        val desktopMain by getting {
            dependsOn(jvmMain)
            dependencies {
            }
        }

        val androidMain by getting {
            dependsOn(jvmMain)
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

android {
    namespace = "io.github.kusius.klvmp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        // These are specified here, because we build the native code
        // with the same CMakeLists.txt, which includes a "find package"
        // for JNI and also "include directories" which are not needed
        // when the android cmake gets invoked.
        // TODO: We could modify the CMakeLists.txt to have the same effect
        //  in order to remove this from here
        externalNativeBuild {
            cmake {
                arguments.addAll(listOf(
                    "-DJAVA_AWT_LIBRARY=NotNeeded",
                    "-DJAVA_JVM_LIBRARY=NotNeeded",
                    "-DJAVA_INCLUDE_PATH2=NotNeeded",
                    "-DJAVA_AWT_INCLUDE_PATH=NotNeeded"
                ))
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    externalNativeBuild {
        cmake {
            path = file("./src/nativeInterop/cinterop/CMakeLists.txt")
        }
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    coordinates(group.toString(), "klvmp", version.toString())

    pom {
        name = "KLV Multiplatform Library"
        description = "A library for parsing KLV metadata for UAS dataset (MISB 0601)"
        inceptionYear = "2024"
        url = "https://github.com/kusius/UASHUB"
        licenses {
            license {
                name = "MIT"
                url = "YYY"
                distribution = "ZZZ"
            }
        }
        developers {
            developer {
                id = "kusius"
                name = "George Kousis"
                url = "https://kusius.github.io"
            }
        }
        scm {
            url = "https://github.com/kusius/UASHUB"
            connection = "scm:git:git://github.com/kusius/UASHUB.git"
            developerConnection = "scm:git:ssh://github.com:kusius/UASHUB.git"
        }
    }
}
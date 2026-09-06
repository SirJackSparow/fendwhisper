import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.dg.whisperfend"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        externalNativeBuild{
            cmake{
                cppFlags += listOf()
                arguments += listOf("-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
                arguments += "-DCMAKE_BUILD_TYPE=Release"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

// ✅ Load version from version.properties
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) versionProps.load(versionPropsFile.inputStream())
val versionName = versionProps.getProperty("VERSION_NAME", "1.1.0")

// ✅ Auto increment patch version
fun incrementVersion(version: String): String {
    val parts = version.split(".").map { it.toInt() }.toMutableList()
    parts[2]++
    return parts.joinToString(".")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.dg.whisperfend"
            artifactId = "whisperfend"
            version = versionName

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("WhisperFend")
                description.set("Android library for Whisper ASR using whisper.cpp")
                url.set("https://github.com/SirJackSparow/fendwhisper")
                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("SirJackSparow")
                        name.set("SirJackSparow")
                    }
                }
                scm {
                    connection.set("scm:git:github.com/SirJackSparow/fendwhisper.git")
                    developerConnection.set("scm:git:ssh://github.com/SirJackSparow/fendwhisper.git")
                    url.set("https://github.com/SirJackSparow/fendwhisper/tree/main")
                }
            }
        }
    }

    repositories {
        maven {
            name = "WhisperFendLib"
            url = uri("https://maven.pkg.github.com/SirJackSparow/fendwhisper")

            credentials {
                username = project.findProperty("gpr.user") as String?
                    ?: System.getenv("USERNAME_GITHUB")
                password = project.findProperty("gpr.key") as String?
                    ?: System.getenv("TOKEN_GITHUB")
            }
        }
    }
}

// ✅ Update version.properties after successful publish
tasks.named("publish") {
    doLast {
        val newVersion = incrementVersion(versionName)
        versionProps["VERSION_NAME"] = newVersion
        versionProps.store(versionPropsFile.writer(), null)
        println("📦 Published version $versionName → Next version: $newVersion")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

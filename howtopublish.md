# 🧩 WhisperFend Android Library

On-device speech-to-text using whisper.cpp. Publish to GitHub Packages.

---

## ⚙️ Step 1: Create `version.properties`

Create this file at the **root** of your project (`ChatAndromedax/version.properties`):

```properties
VERSION_NAME=1.0.0
```

---

## 📦 Step 2: `build.gradle.kts` (whisperfend module)

```kotlin
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
val versionName = versionProps.getProperty("VERSION_NAME", "1.0.0")

// ✅ Auto increment patch version
fun incrementVersion(version: String): String {
    val parts = version.split(".").map { it.toInt() }.toMutableList()
    parts[2]++ // bump patch version (e.g. 1.0.0 → 1.0.1)
    return parts.joinToString(".")
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.dg.whisperfend"
            artifactId = "whisperfend"
            version = versionName
            afterEvaluate { from(components["release"]) }
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
```

---

## 🔑 Step 3: Set GitHub credentials

**Option A — Environment variables** (in `~/.zshrc` or shell):

```bash
export USERNAME_GITHUB=your-github-username
export TOKEN_GITHUB=your-personal-access-token
```

**Option B — `gradle.properties`** (in project root `ChatAndromedax/gradle.properties`):

```properties
gpr.user=your-github-username
gpr.key=your-personal-access-token
```

> Token needs `write:packages` scope.

---

## 🚀 Step 4: Tag and publish

```bash
# Tag the release first
git tag -a 1.0.0 -m "Release 1.0.0"

# Push tag
git push origin 1.0.0

# Publish to GitHub Packages
./gradlew :whisperfend:publish
```

This will:
1. Build the release AAR
2. Upload to https://maven.pkg.github.com/SirJackSparow/fendwhisper
3. Auto-increment patch version in `version.properties`

---

## 📥 Step 5: Use in consumer app

Add GitHub Packages repository to consumer's `build.gradle.kts`:

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/SirJackSparow/fendwhisper")
        credentials {
            username = "your-github-username"
            password = "your-personal-access-token"
        }
    }
}

dependencies {
    implementation("com.dg.whisperfend:whisperfend:1.0.0")
}
```
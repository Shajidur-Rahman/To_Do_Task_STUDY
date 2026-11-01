import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

android {
    namespace = "com.chronos.hsc"
    compileSdk = 34

    signingConfigs {
        create("releaseConfig") {
            val keyStorePath = keystoreProperties.getProperty("storeFile") ?: "app/keystore/release-key.jks"
            storeFile = rootProject.file(keyStorePath)
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }

    defaultConfig {
        applicationId = "com.chronos.hsc"
        minSdk = 26
        targetSdk = 34
        versionCode = 4
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("releaseConfig")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("releaseConfig")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Compose UI & Material 3
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Room SQLite Database with KSP
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-Core:1.8.1".lowercase().replace("core", "core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Jetpack Glance AppWidget
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")

    // Android WorkManager for Background Study Briefings & Domino Alerts
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}

tasks.register("buildAutoUpdateRelease") {
    group = "distribution"
    description = "Increments versionCode, syncs version.json, builds signed release APK, and copies to release/ and project root."

    doFirst {
        val buildGradleFile = file("build.gradle.kts")
        val buildGradleText = buildGradleFile.readText()

        val versionCodeRegex = Regex("""versionCode\s*=\s*(\d+)""")
        val versionNameRegex = Regex("""versionName\s*=\s*"([^"]+)"""")

        val currentVersionCode = versionCodeRegex.find(buildGradleText)?.groupValues?.get(1)?.toInt() ?: 1
        val currentVersionName = versionNameRegex.find(buildGradleText)?.groupValues?.get(1) ?: "1.0.0"

        val newVersionCode = currentVersionCode + 1
        val versionParts = currentVersionName.split(".").map { it.toIntOrNull() ?: 0 }.toMutableList()
        while (versionParts.size < 3) versionParts.add(0)
        versionParts[2] = versionParts[2] + 1
        val newVersionName = versionParts.joinToString(".")

        val updatedBuildGradleText = buildGradleText
            .replaceFirst(versionCodeRegex, "versionCode = $newVersionCode")
            .replaceFirst(versionNameRegex, """versionName = "$newVersionName"""")

        buildGradleFile.writeText(updatedBuildGradleText)
        println("🚀 BUMPED: versionCode=$newVersionCode, versionName=$newVersionName")

        val versionJsonFile = rootProject.file("version.json")
        val newJsonContent = """{
  "versionCode": $newVersionCode,
  "versionName": "$newVersionName",
  "apkUrl": "https://github.com/Shajidur-Rahman/To_Do_Task_STUDY/raw/main/Chronos_HSC_Smart_Study_Engine.apk",
  "releaseNotes": "Version $newVersionName build $newVersionCode with latest feature upgrades and performance optimizations."
}"""
        versionJsonFile.writeText(newJsonContent)
        println("📝 UPDATED: version.json (versionCode=$newVersionCode, versionName=$newVersionName)")
    }

    dependsOn("assembleRelease")

    doLast {
        val builtApk = file("build/outputs/apk/release/app-release.apk")
        if (builtApk.exists()) {
            val releaseDir = rootProject.file("release")
            if (!releaseDir.exists()) releaseDir.mkdirs()
            val targetReleaseApk = File(releaseDir, "app-release.apk")
            builtApk.copyTo(targetReleaseApk, overwrite = true)
            builtApk.copyTo(rootProject.file("Chronos_HSC_Smart_Study_Engine.apk"), overwrite = true)
            println("✅ SIGNED APK COPIED TO: ${targetReleaseApk.absolutePath}")
            println("✅ ROOT APK COPIED TO: ${rootProject.file("Chronos_HSC_Smart_Study_Engine.apk").absolutePath}")
            println("📄 VERSION JSON AT: ${rootProject.file("version.json").absolutePath}")
        }
    }
}

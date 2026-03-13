plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val versionProps = hashMapOf<String, String>()
rootProject.file("version.properties").readLines().forEach { line ->
    val trimmed = line.trim()
    if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
        val (key, value) = trimmed.split("=", limit = 2)
        versionProps[key.trim()] = value.trim()
    }
}

android {
    namespace = "com.kana.phone"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kana.phone"
        minSdk = 26
        targetSdk = 35
        versionCode = versionProps["versionCode"]!!.toInt()
        versionName = "${versionProps["versionMajor"]}.${versionProps["versionMinor"]}"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE", "\"http://10.0.2.2:3001/api\"")
            buildConfigField("String", "AUDIO_BASE", "\"http://10.0.2.2:3001/api/audio/\"")
            buildConfigField("String", "IMAGE_BASE", "\"http://10.0.2.2:3001/api/images/\"")
        }
        release {
            buildConfigField("String", "API_BASE", "\"https://watch.osrs.lv/api\"")
            buildConfigField("String", "AUDIO_BASE", "\"https://watch.osrs.lv/api/audio/\"")
            buildConfigField("String", "IMAGE_BASE", "\"https://watch.osrs.lv/api/images/\"")
            isMinifyEnabled = false
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
}

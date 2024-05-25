import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "org.baiyu.fucklauncher"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.baiyu.fucklauncher"
        minSdk = 26
        targetSdk = 34
        versionCode = 9
        versionName = "6.0"

        resourceConfigurations += setOf("en")

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val properties = Properties().apply {
                load(File("signing.properties").reader())
            }
            storeFile = File(properties.getProperty("storeFilePath"))
            storePassword = properties.getProperty("storePassword")
            keyPassword = properties.getProperty("keyPassword")
            keyAlias = properties.getProperty("keyAlias")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.github.topjohnwu.libsu:core:5.2.2")
    compileOnly("de.robv.android.xposed:api:82")
}

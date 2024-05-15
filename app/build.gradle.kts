plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "io.mehow.audioclip"
    compileSdk = 34

    val dummyConfig by signingConfigs.creating {
        storeFile = file("${rootDir}/mehow-io.keystore")
        storePassword = "mehow-io"
        keyAlias = "mehow-io"
        keyPassword = "mehow-io"
    }

    defaultConfig {
        applicationId = "io.mehow.audioclip"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        signingConfig = dummyConfig

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.okhttp)
    implementation(libs.ffmpeg)
}
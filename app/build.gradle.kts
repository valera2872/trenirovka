plugins {
    id("com.android.application")
}

android {
    namespace = "com.valera2872.bjjarm"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.valera2872.grapplingarm"
        minSdk = 26
        targetSdk = 35
        versionCode = 20
        versionName = "0.9.0"
    }

    buildTypes {
        release {
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
}

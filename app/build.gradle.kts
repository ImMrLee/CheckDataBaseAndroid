plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.checkdatabase"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.checkdatabase"
        minSdk = 23
        targetSdk = 36
        versionCode = 4
        versionName = "1.0-release2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation(platform("androidx.compose:compose-bom:2026.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.junit.ktx)
    implementation("mysql:mysql-connector-java:5.1.49")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    testImplementation(kotlin("test"))
}
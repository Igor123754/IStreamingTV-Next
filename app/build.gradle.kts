plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.igor.istreamingtv"

    compileSdk = 35

    defaultConfig {
        applicationId = "com.igor.istreamingtv"

        minSdk = 23
        targetSdk = 35

        versionCode = 1
        versionName = "0.1.0"

        buildConfigField(
            "String",
            "TMDB_API_KEY",
            "\"${System.getenv("TMDB_API_KEY") ?: ""}\""
        )
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // Android
    implementation("androidx.core:core-ktx:1.15.0")

    implementation(
        "androidx.activity:activity-compose:1.10.1"
    )

    // Compose
    implementation(
        platform(
            "androidx.compose:compose-bom:2025.02.00"
        )
    )

    implementation("androidx.compose.ui:ui")

    implementation(
        "androidx.compose.ui:ui-tooling-preview"
    )

    implementation(
        "androidx.compose.material3:material3"
    )

    debugImplementation(
        "androidx.compose.ui:ui-tooling"
    )

    // ViewModel
    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7"
    )

    // Retrofit
    implementation(
        "com.squareup.retrofit2:retrofit:2.11.0"
    )

    implementation(
        "com.squareup.retrofit2:converter-gson:2.11.0"
    )

    // Coil
    implementation(
        "io.coil-kt:coil-compose:2.7.0"
    )
}

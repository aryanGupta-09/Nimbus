plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt") // Using kapt instead of KSP
}

import java.io.FileInputStream
import java.util.Properties

// Read API keys from properties file
val apikeyPropertiesFile = rootProject.file("apikey.properties")
val apikeyProperties = Properties()
if (apikeyPropertiesFile.exists()) {
    apikeyProperties.load(FileInputStream(apikeyPropertiesFile))
} else {
    apikeyProperties.setProperty("WEATHER_API_KEY", "")
    apikeyProperties.setProperty("GEMINI_API_KEY", "")
}

android {
    namespace = "com.example.nimbus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.nimbus"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "WEATHER_API_KEY", apikeyProperties["WEATHER_API_KEY"]?.toString()?.let { "\"$it\"" } ?: "\"\"")
            buildConfigField("String", "GEMINI_API_KEY", apikeyProperties["GEMINI_API_KEY"]?.toString()?.let { "\"$it\"" } ?: "\"\"")
        }
        debug {
            buildConfigField("String", "WEATHER_API_KEY", apikeyProperties["WEATHER_API_KEY"]?.toString()?.let { "\"$it\"" } ?: "\"\"")
            buildConfigField("String", "GEMINI_API_KEY", apikeyProperties["GEMINI_API_KEY"]?.toString()?.let { "\"$it\"" } ?: "\"\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Gemini AI dependencies
    implementation("com.google.ai.client.generativeai:generativeai:0.2.2")
    
    // WorkManager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Compose dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    
    // Pull-to-refresh
    implementation("androidx.compose.material:material:1.7.0")
    
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0") // Kotlin support for Moshi
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    
    // Image loading
    implementation(libs.coil)
    
    // Location
    implementation(libs.play.services.location)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    // DataStore for storing API keys
    implementation(libs.datastore.preferences)
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Support for AutoMirrored icons
    implementation("androidx.compose.material:material-icons-extended")
    
    // Lottie animations for weather icons
    implementation("com.airbnb.android:lottie-compose:4.0.0")
    
    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
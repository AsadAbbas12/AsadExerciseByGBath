plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias (libs.plugins.kapt)
}

android {
    namespace = "com.task.asadexercise"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.task.asadexercise"
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
    }


}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.play.services.cast.framework)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    kapt(libs.hilt.compiler) // ✅ Inject Hilt compiler
    implementation(libs.hilt.navigation.compose) // Hilt integration with Jetpack Compose
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

   implementation (libs.androidx.foundation)
   implementation (libs.ui)
   implementation (libs.androidx.runtime.livedata)
   implementation (libs.androidx.lifecycle.viewmodel.compose)
   implementation (libs.androidx.media)
   implementation (libs.exoplayer) // using deprecated api because exo three has some bugs yet.
    implementation(libs.coil.compose) // Use the latest version
    implementation ("androidx.browser:browser:1.8.0")
    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation ("androidx.core:core:1.12.0")
    implementation ("androidx.media:media:1.6.0") // For MediaSessionCompat
    implementation("androidx.datastore:datastore-preferences:1.0.0")



//    implementation(libs.androidx.media3.exoplayer)
//    implementation (libs.androidx.media3.ui)


}
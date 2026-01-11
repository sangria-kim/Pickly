plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.cola.pickly"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.cola.pickly"
        minSdk = 26
        targetSdk = 36
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
    // --- Splash Screen ---
    implementation("androidx.core:core-splashscreen:1.0.1")

    // --- Compose BOM ---
    implementation(platform(libs.androidx.compose.bom))

    // --- Android 기본 ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose) // compose + activity-ktx 연동

    // --- DataStore (Settings persistence) ---
    implementation(libs.androidx.datastore.preferences)

    // --- Lifecycle Compose (collectAsStateWithLifecycle) ---
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // (viewModel() 같은 걸 Compose에서 쓸 때 필요, 지금은 없어도 되지만 남겨도 무방)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("io.coil-kt:coil-compose:2.7.0")

    // --- Navigation Compose ---
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // --- Compose UI ---
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.text)
    // Material 아이콘 확장 라이브러리 추가
    implementation("androidx.compose.material:material-icons-extended-android:1.6.7")

    // --- ML Kit ---
    implementation(libs.mlkit.face.detection)
    
    // --- Exif Interface ---
    implementation(libs.androidx.exifinterface)

    // --- Room ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.foundation)
    ksp(libs.androidx.room.compiler)

    // --- Gson ---
    implementation(libs.google.gson)

    // --- Hilt ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // --- Debug / Test ---
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
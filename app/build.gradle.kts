plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
    alias(libs.plugins.hilt)
    alias(libs.plugins.aboutlibraries)
}

android {
    namespace = "com.cola.pickly"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cola.pickly"
        minSdk = 26
        targetSdk = 36
        versionCode = 10100
        versionName = "1.1.0"

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
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        freeCompilerArgs.addAll(
            "-Xskip-prerelease-check",
            "-Xsuppress-version-warnings"
        )
    }
}

dependencies {
    // --- Core Modules ---
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    
    // --- Feature Modules ---
    implementation(project(":feature:settings"))
    implementation(project(":feature:archive"))
    implementation(project(":feature:organize"))
    
    // --- Splash Screen ---
    implementation(libs.androidx.core.splashscreen)

    // --- Coil ---
    implementation(libs.coil)
    implementation(libs.coil.compose)

    // --- Compose BOM ---
    implementation(platform(libs.androidx.compose.bom))

    // --- Android 기본 ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose) // compose + activity-ktx 연동

    // --- Lifecycle Compose (collectAsStateWithLifecycle) ---
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // --- Navigation Compose ---
    implementation(libs.androidx.navigation.compose)

    // --- Compose UI ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)

    // --- Hilt ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // --- AboutLibraries ---
    implementation(libs.aboutlibraries.compose.m3.android)

    // --- Debug / Test ---
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

aboutLibraries {
    // AboutLibraries 설정
    // 라이선스 정보를 자동으로 수집하여 생성
}

// AboutLibraries가 생성한 JSON 파일을 res/raw로 복사
// Note: 이 파일은 빌드 시 자동으로 생성되며, 수동으로 복사해야 합니다.
// 개발 시에는 exportLibraryDefinitions 태스크를 실행한 후 수동으로 복사하거나,
// CI/CD 파이프라인에서 자동화할 수 있습니다.
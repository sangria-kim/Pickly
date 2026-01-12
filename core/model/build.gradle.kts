plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(11)
}

// 순수 Kotlin 모듈 - Android 의존성 없음

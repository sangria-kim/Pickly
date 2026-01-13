plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    api(project(":core:model"))

    // SharedFlow 기반 Notifier 인터페이스 및 Domain 레이어의 coroutine 사용을 위한 의존성
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

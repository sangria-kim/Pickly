# ============================================================
# Pickly ProGuard / R8 Rules
# ============================================================

# --- 디버깅: 크래시 스택트레이스에 소스 파일/라인 번호 유지 ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================================
# Gson (Room TypeConverter에서 리플렉션 기반 직렬화 사용)
# ============================================================
-keepattributes Signature
-keepattributes *Annotation*

# Gson이 리플렉션으로 접근하는 모델 클래스 필드 유지
-keep class com.cola.pickly.core.model.RecommendationScore { *; }
-keep class com.cola.pickly.core.model.FaceBoundingBox { *; }
-keep class com.cola.pickly.core.model.RejectReason { *; }

# Gson TypeToken (제네릭 타입 정보 유지)
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# ============================================================
# Room
# ============================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# ============================================================
# Hilt / Dagger
# ============================================================
# Hilt는 자체 ProGuard 규칙을 포함하므로 추가 규칙 최소화
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# ============================================================
# ML Kit (Face Detection, Pose Detection)
# ============================================================
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_** { *; }

# ============================================================
# DataStore Enum (이름 기반 저장/복원에 사용)
# ============================================================
-keepclassmembers enum com.cola.pickly.core.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================================
# Coil (이미지 로딩)
# ============================================================
-keep class coil3.** { *; }

# ============================================================
# Kotlin
# ============================================================
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# ============================================================
# AndroidX / Jetpack Compose
# ============================================================
-dontwarn androidx.**

# ============================================================
# AboutLibraries
# ============================================================
-keep class com.mikepenz.aboutlibraries.** { *; }

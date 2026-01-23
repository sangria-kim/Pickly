# CLAUDE.md
## 대답은 항상 한국어로 작성

## 문서 구조
Detailed documentation is available in `/docs/`:
1. `PROJECT_CONTEXT.md` - 아키텍처, 기술 스택, 모듈 구조
2. `CORE_MODELS_AND_CONTRACTS.md` - 데이터 모델, UI State, Repository
3. `rules.md` - Git 브랜치 전략 & 커밋 컨벤션

## 문서 읽는 순서 (작업 전)
- 리팩토링: `PROJECT_CONTEXT.md` → `CORE_MODELS_AND_CONTRACTS.md`
- 커밋 작성: `rules.md` 확인

## 작업 시작 전 필수 확인 (rules.md 기반)
### 브랜치 체크
- 현재 브랜치가 `main`인가? → ❌ 작업 중단
- `feature/xxx` 형태인가? → ✅ 작업 진행

### Claude 동작 규칙
- main 브랜치 감지 시:
    1. 사용자에게 경고
    2. feature 브랜치 생성 명령 제안
    3. 코드 작성 시작 안 함

##  커밋 메시지 작성 (rules.md 준수)
모든 코드 제안 시 커밋 메시지 예시 제공 필수

### 템플릿
\`\`\`
<type>: <subject>

<body 2-3줄>
\`\`\`

### 예시
\`\`\`
feat: add photo selection state management

OrganizeViewModel에 선택 상태 관리 로직을 추가했습니다.
사용자가 사진을 선택/해제할 때 StateFlow로 상태를 관리하며,
UI에서는 collectAsStateWithLifecycle()로 관찰합니다.
\`\`\`

##  코딩 컨벤션

### 1. Kotlin & Style
- **Hard Rule**: Kotlin 공식 스타일 가이드 준수 (4 space indent)
- **File Structure**: Imports → Class → Properties → Init → Public Methods → Private Methods → Companion Object
- **Naming**:
  - Class/Object/Interface: `PascalCase`
  - Function/Property: `camelCase`
  - Composable Function: `PascalCase` (명사/동사구 허용)
  - Const: `UPPER_SNAKE_CASE`

### 2. Architecture (MVVM + Clean)
- **Flow**: UI → ViewModel → UseCase(Optional) → Repository → DataSource
- **Dependencies**:
  - UI는 ViewModel만 의존
  - ViewModel은 UseCase 또는 Repository만 의존 (Android Framework 의존성 최소화)
  - Domain Layer는 순수 Kotlin (Android 의존성 없음)

### 3. State Management (StateFlow)

#### ViewModel에서 State 노출
```kotlin
// ❌ 잘못된 코드
class OrganizeViewModel : ViewModel() {
    val state = MutableStateFlow<OrganizeUiState>(NoFolderSelected)
}

// ✅ 올바른 코드
class OrganizeViewModel : ViewModel() {
    private val _state = MutableStateFlow<OrganizeUiState>(NoFolderSelected)
    val state = _state.asStateFlow()
}
```

#### State 업데이트
```kotlin
// ✅ update 사용 권장 (동시성 안전)
_state.update { currentState ->
    GridReady(photos = newPhotos, selectedIds = currentState.selectedIds)
}

// ⚠️ value 직접 할당 (간단한 상태 변경 시 사용)
_state.value = OrganizeUiState.Loading
```

#### Compose에서 수집
```kotlin
// ❌ 기본 collectAsState
val state = viewModel.state.collectAsState()

// ✅ Lifecycle 고려
val state by viewModel.state.collectAsStateWithLifecycle()
```

### 4. Threading (Coroutines & Dispatchers)

#### Repository 내부 (IO 처리)
```kotlin
// ✅ withContext(Dispatchers.IO) 필수
class MediaStorePhotoRepository @Inject constructor(...) : PhotoRepository {
    override suspend fun getPhotos(): List<Photo> = withContext(Dispatchers.IO) {
        // MediaStore 쿼리 등 무거운 작업 수행
        contentResolver.query(...)
    }
}
```

#### ViewModel에서 로직 호출 (Main-Safe)
```kotlin
// ✅ Repository가 IO 처리를 담당하므로 ViewModel은 Main에서 호출
fun loadPhotos() {
    viewModelScope.launch { // Dispatchers.Main (Default)
        val photos = repository.getPhotos() // Suspend function (Main-Safe)
        _state.value = GridReady(photos = photos) // UI 상태 업데이트는 Main 스레드 필수
    }
}
```

**규칙**: Repository 구현체는 반드시 `withContext(Dispatchers.IO)`를 사용하여 **Main-Safe**를 보장해야 함.

### 5. Error Handling

#### ViewModel에서 예외 처리
```kotlin
// ✅ try-catch + UI State 반영
fun loadPhotos() {
    _state.value = OrganizeUiState.Loading
    viewModelScope.launch {
        try {
            val photos = repository.getPhotos()
            _state.value = OrganizeUiState.GridReady(photos = photos)
        } catch (e: Exception) {
            // 에러 발생 시 적절한 에러 UI 상태로 전환 또는 스낵바노출
            _state.value = OrganizeUiState.NoFolderSelected 
            // 또는 _snackbarMessages.emit("Error loading photos")
        }
    }
}
```

### 6. Do NOT Use (금지 목록)
```kotlin
// ❌ LiveData (StateFlow 사용)
val state: LiveData<UiState>

// ❌ RxJava (Coroutines/Flow 사용)
Observable.just(...)

// ❌ XML Layouts (Compose Only)
setContentView(R.layout.activity_main)

// ❌ Glide (Coil 사용)
Glide.with(context).load(uri)

// ❌ Mutable State 직접 노출
val state = MutableStateFlow<UiState>(...)

// ❌ ViewModel에서 명시적 IO Dispatcher 사용 (Repository 위임 권장)
viewModelScope.launch(Dispatchers.IO) { ... } // ❌ (UI 업데이트 시 크래시 가능성)
```

### 7. File & Module Structure

#### 파일명 규칙
- ViewModel: `OrganizeViewModel.kt`
- Screen: `OrganizeScreen.kt`
- UI State: `OrganizeUiState.kt`
- Repository Interface: `PhotoRepository.kt`
- Repository 구현: `MediaStorePhotoRepository.kt`

#### 모듈 의존성 방향
```
:app
  ↓
:feature:organize
  ↓
:core:domain (Repository Interface, UseCase)
  ↓
:core:data (Repository 구현)
  ↓
:core:model (Data Classes)
```

**규칙**: `:core:model`은 어디에도 의존하면 안 됨 (순수 Kotlin)

### 8. Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run all unit tests
./gradlew test

# Run tests for a specific module
./gradlew :feature:organize:test

# Run Android instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean build
```
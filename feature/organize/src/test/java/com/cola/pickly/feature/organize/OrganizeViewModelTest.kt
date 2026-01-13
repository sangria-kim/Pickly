package com.cola.pickly.feature.organize

import com.cola.pickly.core.domain.repository.PhotoRepository
import com.cola.pickly.core.domain.refresh.PhotoDataRefreshNotifier
import com.cola.pickly.core.domain.refresh.RefreshReason
import com.cola.pickly.core.data.photo.PhotoActionReport
import com.cola.pickly.core.data.photo.PhotoActionRepository
import com.cola.pickly.core.data.settings.DuplicateFilenamePolicy
import com.cola.pickly.core.data.settings.Settings
import com.cola.pickly.core.data.settings.SettingsRepository
import com.cola.pickly.core.model.WeeklyPhoto
import com.cola.pickly.feature.organize.domain.usecase.CopySelectedPhotosUseCase
import com.cola.pickly.feature.organize.domain.usecase.MoveSelectedPhotosUseCase
import com.cola.pickly.feature.organize.domain.usecase.ShareSelectedPhotosUseCase
import com.cola.pickly.feature.organize.domain.usecase.SoftDeleteSelectedPhotosUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizeViewModelTest {

    @After
    fun tearDown() {
        // 테스트마다 setMain을 호출하므로, 혹시 남아있을 수 있는 메인 디스패처를 정리합니다.
        Dispatchers.resetMain()
    }

    @Test
    fun `delete confirmation dialog shows when requested`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val photoRepo = FakePhotoRepository(
            bucketPhotos = mapOf(
                "bucket1" to listOf(
                    WeeklyPhoto(
                        id = 1L,
                        filePath = "/tmp/1.jpg",
                        takenAt = 0L,
                        width = 100,
                        height = 100,
                        bucketId = "bucket1"
                    )
                )
            )
        )
        val settingsRepo = FakeSettingsRepository(
            Settings(
                duplicateFilenamePolicy = DuplicateFilenamePolicy.Skip
            )
        )
        val actionRepo = ControlledPhotoActionRepository()

        val viewModel = newViewModel(photoRepo, actionRepo, settingsRepo)

        viewModel.updateSelectedFolder("bucket1", "Folder1")
        advanceUntilIdle()

        viewModel.toggleSelection(1L)
        viewModel.requestDeleteConfirmation()

        assertTrue(viewModel.showDeleteConfirm.value)
    }

    @Test
    fun `delete confirmation dialog dismisses when cancelled`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val photoRepo = FakePhotoRepository(
            bucketPhotos = mapOf(
                "bucket1" to listOf(
                    WeeklyPhoto(
                        id = 1L,
                        filePath = "/tmp/1.jpg",
                        takenAt = 0L,
                        width = 100,
                        height = 100,
                        bucketId = "bucket1"
                    )
                )
            )
        )
        val settingsRepo = FakeSettingsRepository(
            Settings(
                duplicateFilenamePolicy = DuplicateFilenamePolicy.Skip
            )
        )
        val actionRepo = ControlledPhotoActionRepository()

        val viewModel = newViewModel(photoRepo, actionRepo, settingsRepo)

        viewModel.updateSelectedFolder("bucket1", "Folder1")
        advanceUntilIdle()
        viewModel.toggleSelection(1L)

        viewModel.requestDeleteConfirmation()
        assertTrue(viewModel.showDeleteConfirm.value)

        viewModel.dismissDeleteConfirmation()
        assertFalse(viewModel.showDeleteConfirm.value)
    }

    @Test
    fun `deleteSelectedPhotos toggles progress and emits snackbar`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val photoRepo = FakePhotoRepository(
            bucketPhotos = mapOf(
                "bucket1" to listOf(
                    WeeklyPhoto(
                        id = 1L,
                        filePath = "/tmp/1.jpg",
                        takenAt = 0L,
                        width = 100,
                        height = 100,
                        bucketId = "bucket1"
                    )
                )
            )
        )
        val settingsRepo = FakeSettingsRepository(
            Settings(
                duplicateFilenamePolicy = DuplicateFilenamePolicy.Skip
            )
        )
        val actionRepo = ControlledPhotoActionRepository()
        val viewModel = newViewModel(photoRepo, actionRepo, settingsRepo)

        viewModel.updateSelectedFolder("bucket1", "Folder1")
        advanceUntilIdle()
        viewModel.toggleSelection(1L)
        viewModel.requestDeleteConfirmation()
        assertTrue(viewModel.showDeleteConfirm.value)

        val snackbarDeferred = async { viewModel.snackbarMessages.first() }

        // delete가 진행 중인 상태를 만들기 위해 리포트 완료를 지연
        val reportDeferred = CompletableDeferred<PhotoActionReport>()
        actionRepo.softDeleteReport = reportDeferred

        viewModel.deleteSelectedPhotos()
        runCurrent()
        assertTrue(viewModel.isActionInProgress.value)
        assertFalse(viewModel.showDeleteConfirm.value)

        reportDeferred.complete(PhotoActionReport(successCount = 1, failedCount = 0))
        advanceUntilIdle()

        assertFalse(viewModel.isActionInProgress.value)
        val snackbarMessage = snackbarDeferred.await()
        assertTrue(
            "삭제 스낵바 메시지가 기대값을 포함해야 합니다. actual=$snackbarMessage",
            snackbarMessage.contains("삭제 완료")
        )

        // 상태 업데이트(선택 해제)가 반영될 때까지 Flow로 기다립니다.
        viewModel.uiState.first { state ->
            state is OrganizeUiState.GridReady && state.selectedIds.isEmpty()
        }
    }
}

private fun newViewModel(
    photoRepository: PhotoRepository,
    actionRepository: PhotoActionRepository,
    settingsRepository: SettingsRepository
): OrganizeViewModel {
    return OrganizeViewModel(
        photoRepository = photoRepository,
        photoDataRefreshNotifier = FakePhotoDataRefreshNotifier(),
        shareSelectedPhotosUseCase = ShareSelectedPhotosUseCase(actionRepository),
        moveSelectedPhotosUseCase = MoveSelectedPhotosUseCase(actionRepository, settingsRepository),
        copySelectedPhotosUseCase = CopySelectedPhotosUseCase(actionRepository, settingsRepository),
        softDeleteSelectedPhotosUseCase = SoftDeleteSelectedPhotosUseCase(actionRepository)
    )
}

private class FakePhotoDataRefreshNotifier : PhotoDataRefreshNotifier {
    private val _refreshEvents = MutableSharedFlow<RefreshReason>(extraBufferCapacity = 1)
    override val refreshEvents: SharedFlow<RefreshReason> = _refreshEvents.asSharedFlow()

    override suspend fun notify(reason: RefreshReason) {
        _refreshEvents.emit(reason)
    }
}

private class FakePhotoRepository(
    private val bucketPhotos: Map<String, List<WeeklyPhoto>> = emptyMap()
) : PhotoRepository {
    override suspend fun getPhotosInFolder(folderRelativePath: String): List<WeeklyPhoto> = emptyList()
    override suspend fun getPhotosByBucketId(bucketId: String): List<WeeklyPhoto> = bucketPhotos[bucketId].orEmpty()
    override suspend fun getRecentPhotos(): List<WeeklyPhoto> = emptyList()
    override suspend fun getFolders(): List<com.cola.pickly.core.model.PhotoFolder> = emptyList()
    override suspend fun getAllPhotos(): List<WeeklyPhoto> = emptyList()
}

private class ControlledPhotoActionRepository : PhotoActionRepository {
    var softDeleteReport: CompletableDeferred<PhotoActionReport>? = null

    override suspend fun getShareUris(photoIds: List<Long>) = emptyList<android.net.Uri>()

    override suspend fun createWriteRequestIntentSender(photoIds: List<Long>): android.content.IntentSender? = null

    override suspend fun createDeleteRequestIntentSender(photoIds: List<Long>): android.content.IntentSender? = null

    override suspend fun movePhotos(
        photoIds: List<Long>,
        destinationRelativePath: String,
        policy: DuplicateFilenamePolicy
    ): PhotoActionReport = PhotoActionReport(successCount = 0)

    override suspend fun copyPhotos(
        photoIds: List<Long>,
        destinationRelativePath: String,
        policy: DuplicateFilenamePolicy
    ): PhotoActionReport = PhotoActionReport(successCount = 0)

    override suspend fun softDeletePhotos(photoIds: List<Long>): PhotoActionReport {
        return softDeleteReport?.await() ?: PhotoActionReport(successCount = photoIds.size)
    }
}

private class FakeSettingsRepository(initial: Settings) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: Flow<Settings> = state

    override suspend fun setDuplicateFilenamePolicy(policy: DuplicateFilenamePolicy) {
        state.value = state.value.copy(duplicateFilenamePolicy = policy)
    }

    override suspend fun setRecommendationEnabled(enabled: Boolean) {
        state.value = state.value.copy(isRecommendationEnabled = enabled)
    }

    override suspend fun setThemeMode(mode: com.cola.pickly.core.data.settings.ThemeMode) {
        state.value = state.value.copy(themeMode = mode)
    }
}

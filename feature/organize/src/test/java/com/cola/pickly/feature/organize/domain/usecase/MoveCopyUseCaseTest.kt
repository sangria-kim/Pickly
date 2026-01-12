package com.cola.pickly.feature.organize.domain.usecase

import com.cola.pickly.core.data.photo.PhotoActionReport
import com.cola.pickly.core.data.photo.PhotoActionRepository
import com.cola.pickly.core.data.settings.DuplicateFilenamePolicy
import com.cola.pickly.core.data.settings.ResultSaveLocationPolicy
import com.cola.pickly.core.data.settings.Settings
import com.cola.pickly.core.data.settings.SettingsRepository
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveCopyUseCaseTest {

    @Test
    fun `move uses last used path when remember policy`() = runBlocking {
        val actionRepo = RecordingPhotoActionRepository()
        val settingsRepo = FakeSettingsRepository(
            Settings(
                resultSaveLocationPolicy = ResultSaveLocationPolicy.RememberLastUsedFolder,
                duplicateFilenamePolicy = DuplicateFilenamePolicy.Skip,
                lastUsedSaveFolder = "DCIM/Pickly/old"
            )
        )

        val useCase = MoveSelectedPhotosUseCase(actionRepo, settingsRepo)

        useCase(listOf(1L, 2L))

        assertEquals("DCIM/Pickly/old", actionRepo.lastDestination.get())
        assertEquals(DuplicateFilenamePolicy.Skip, actionRepo.lastPolicy.get())
    }

    @Test
    fun `copy creates timestamp path for new folder policy`() = runBlocking {
        val actionRepo = RecordingPhotoActionRepository()
        val settingsRepo = FakeSettingsRepository(
            Settings(
                resultSaveLocationPolicy = ResultSaveLocationPolicy.AlwaysCreateNewFolder,
                duplicateFilenamePolicy = DuplicateFilenamePolicy.Overwrite,
                lastUsedSaveFolder = null
            )
        )

        val useCase = CopySelectedPhotosUseCase(actionRepo, settingsRepo)

        useCase(listOf(10L))

        val destination = actionRepo.lastDestination.get()
        assertTrue(destination.startsWith("DCIM/Pickly/"))
        assertEquals(DuplicateFilenamePolicy.Overwrite, actionRepo.lastPolicy.get())
        assertEquals(destination, settingsRepo.lastSavedPath.get())
    }
}

private class RecordingPhotoActionRepository : PhotoActionRepository {
    val lastDestination = AtomicReference<String>()
    val lastPolicy = AtomicReference<DuplicateFilenamePolicy>()

    override suspend fun getShareUris(photoIds: List<Long>) = emptyList<android.net.Uri>()

    override suspend fun movePhotos(
        photoIds: List<Long>,
        destinationRelativePath: String,
        policy: DuplicateFilenamePolicy
    ): PhotoActionReport {
        lastDestination.set(destinationRelativePath)
        lastPolicy.set(policy)
        return PhotoActionReport(successCount = photoIds.size)
    }

    override suspend fun copyPhotos(
        photoIds: List<Long>,
        destinationRelativePath: String,
        policy: DuplicateFilenamePolicy
    ): PhotoActionReport {
        lastDestination.set(destinationRelativePath)
        lastPolicy.set(policy)
        return PhotoActionReport(successCount = photoIds.size)
    }

    override suspend fun softDeletePhotos(photoIds: List<Long>): PhotoActionReport {
        return PhotoActionReport(successCount = photoIds.size)
    }
}

private class FakeSettingsRepository(initial: Settings) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    val lastSavedPath = AtomicReference<String?>()

    override val settings: Flow<Settings> = state

    override suspend fun setResultSaveLocationPolicy(policy: ResultSaveLocationPolicy) {
        state.value = state.value.copy(resultSaveLocationPolicy = policy)
    }

    override suspend fun setDuplicateFilenamePolicy(policy: DuplicateFilenamePolicy) {
        state.value = state.value.copy(duplicateFilenamePolicy = policy)
    }

    override suspend fun setLastUsedSaveFolder(relativePath: String) {
        lastSavedPath.set(relativePath)
        state.value = state.value.copy(lastUsedSaveFolder = relativePath)
    }

    override suspend fun setRecommendationEnabled(enabled: Boolean) {
        state.value = state.value.copy(isRecommendationEnabled = enabled)
    }

    override suspend fun setThemeMode(mode: com.cola.pickly.core.data.settings.ThemeMode) {
        state.value = state.value.copy(themeMode = mode)
    }
}

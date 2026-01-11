package com.cola.pickly.data.cache

import android.content.Context
import coil.Coil
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoilApi::class)
@Singleton
class DefaultCacheRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : CacheRepository {

    override suspend fun getCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        directorySizeBytes(context.cacheDir)
    }

    override suspend fun clearCache() = withContext(Dispatchers.IO) {
        val imageLoader: ImageLoader = Coil.imageLoader(context)

        // Coil caches
        runCatching { imageLoader.memoryCache?.clear() }
        runCatching { imageLoader.diskCache?.clear() }

        // App cache dir (includes coil disk cache directory under cacheDir by default in this app)
        deleteDirectoryContents(context.cacheDir)
    }

    private fun directorySizeBytes(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        if (dir.isFile) return dir.length()

        var total = 0L
        dir.listFiles()?.forEach { child ->
            total += directorySizeBytes(child)
        }
        return total
    }

    private fun deleteDirectoryContents(dir: File?) {
        if (dir == null || !dir.exists() || !dir.isDirectory) return
        dir.listFiles()?.forEach { child ->
            child.deleteRecursively()
        }
    }
}



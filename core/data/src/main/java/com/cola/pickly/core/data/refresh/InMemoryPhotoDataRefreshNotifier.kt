package com.cola.pickly.core.data.refresh

import com.cola.pickly.core.domain.refresh.PhotoDataRefreshNotifier
import com.cola.pickly.core.domain.refresh.RefreshReason
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class InMemoryPhotoDataRefreshNotifier @Inject constructor() : PhotoDataRefreshNotifier {
    private val _refreshEvents = MutableSharedFlow<RefreshReason>(extraBufferCapacity = 1)
    override val refreshEvents: SharedFlow<RefreshReason> = _refreshEvents.asSharedFlow()

    override suspend fun notify(reason: RefreshReason) {
        _refreshEvents.emit(reason)
    }
}


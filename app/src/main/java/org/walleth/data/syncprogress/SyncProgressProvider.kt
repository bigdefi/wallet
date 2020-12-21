package org.BigDefi.data.syncprogress

import androidx.lifecycle.MutableLiveData

class SyncProgressProvider : MutableLiveData<BigDefiSyncProgress>() {

    init {
        value = BigDefiSyncProgress(false, 0L, 0L)
    }
}
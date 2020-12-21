package org.BigDefi.data.syncprogress

data class BigDefiSyncProgress(val isSyncing: Boolean = false, val currentBlock: Long=0, val highestBlock: Long=0)
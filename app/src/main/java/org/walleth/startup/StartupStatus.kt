package org.BigDefi.startup

sealed class StartupStatus {
    object NeedsAddress : StartupStatus()
    object HasChainAndAddress : StartupStatus()
    object Timeout : StartupStatus()
}
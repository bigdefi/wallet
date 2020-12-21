package org.BigDefi.toolbar

import org.koin.core.KoinComponent
import org.koin.core.inject
import org.BigDefi.data.config.Settings

interface ToolbarColorChangeDetector {
    val BigDefiSettings: Settings
    var lastToolbarColor: Long
    fun calcToolbarColorCombination() = BigDefiSettings.toolbarBackgroundColor.toLong() + BigDefiSettings.toolbarForegroundColor

    fun didToolbarColorChange() = (lastToolbarColor != calcToolbarColorCombination()).also {
        lastToolbarColor = calcToolbarColorCombination()
    }

}

class DefaultToolbarChangeDetector : ToolbarColorChangeDetector, KoinComponent {
    override val BigDefiSettings: Settings by inject()
    override var lastToolbarColor: Long = calcToolbarColorCombination()
}
package org.BigDefi.preferences

import android.os.Bundle
import org.BigDefi.R
import org.BigDefi.base_activities.BaseSubActivity
import org.BigDefi.toolbar.DefaultToolbarChangeDetector
import org.BigDefi.toolbar.ToolbarColorChangeDetector

class PreferenceActivity : BaseSubActivity() ,  ToolbarColorChangeDetector by DefaultToolbarChangeDetector() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_prefs)

        supportActionBar?.subtitle = getString(R.string.preferences_activity_subtitle)
    }

    override fun onResume() {
        super.onResume()

        if (didToolbarColorChange()) {
            recreate()
        }
    }
}
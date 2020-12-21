package org.BigDefi.preferences


import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.koin.android.ext.android.inject
import org.BigDefi.App
import org.BigDefi.R
import org.BigDefi.data.config.Settings
import org.BigDefi.data.tokens.CurrentTokenProvider

class WalletPrefsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val settings: Settings by inject()
    private val currentTokenProvider: CurrentTokenProvider by inject()

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        findPreference<Preference>(getString(R.string.key_reference))?.summary = getString(R.string.settings_currently, settings.currentFiat)
        findPreference<Preference>(getString(R.string.key_token))?.summary = getString(R.string.settings_currently, currentTokenProvider.getCurrent().name)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        try {
            if (key == getString(R.string.key_prefs_day_night)) {

                App.applyNightMode(settings)
                activity?.recreate()
            }
            if (key == getString(R.string.key_noscreenshots)) {
                activity?.recreate()
            }
            if (key == getString(R.string.key_prefs_start_light)) {

            }
        } catch (ignored: IllegalStateException) {
        }
    }


    override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        App.extraPreferences.forEach {
            addPreferencesFromResource(it.first)
            it.second(preferenceScreen)
        }
    }

}
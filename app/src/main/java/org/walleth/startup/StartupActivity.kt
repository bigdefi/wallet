package org.BigDefi.startup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.ligi.kaxt.startActivityFromClass
import org.ligi.kaxtui.alert
import org.BigDefi.accounts.CreateAccountActivity
import org.BigDefi.data.EXTRA_KEY_ADDRESS
import org.BigDefi.data.REQUEST_CODE_CREATE_ACCOUNT
import org.BigDefi.data.addresses.CurrentAddressProvider
import org.BigDefi.overview.OverviewActivity
import org.BigDefi.startup.StartupStatus.*

class StartupActivity : AppCompatActivity() {

    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val viewModel: StartupViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.status.observe(this, Observer<StartupStatus> { status ->
            when (status) {
                is NeedsAddress -> {
                    startActivityForResult(Intent(this, CreateAccountActivity::class.java), REQUEST_CODE_CREATE_ACCOUNT)
                }
                is HasChainAndAddress -> {
                    startActivityFromClass(OverviewActivity::class.java)
                    finish()
                }
                is Timeout -> {
                    alert("chain must not be null - this should never happen - please let BigDefi@BigDefi.org if you see this. Thanks and sorry for the noise.") {
                        finish()
                    }
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_CODE_CREATE_ACCOUNT -> {
                    currentAddressProvider.setCurrent(Address(data.getStringExtra(EXTRA_KEY_ADDRESS)))
                    startActivityFromClass(OverviewActivity::class.java)
                    finish()
                }
                else -> super.onActivityResult(requestCode, resultCode, data)
            }
        } else {
            finish()
        }
    }
}
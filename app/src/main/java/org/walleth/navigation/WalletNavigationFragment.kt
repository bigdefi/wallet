package org.BigDefi.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main_in_drawer_container.view.*
import kotlinx.android.synthetic.main.navigation_drawer_header.view.*
import org.koin.android.ext.android.inject
import org.ligi.kaxt.startActivityFromClass
import org.BigDefi.R
import org.BigDefi.accounts.EditAccountActivity
import org.BigDefi.accounts.SwitchAccountActivity
import org.BigDefi.activities.TincubETHActivity
import org.BigDefi.chains.ChainInfoProvider
import org.BigDefi.chains.SwitchChainActivity
import org.BigDefi.data.AppDatabase
import org.BigDefi.data.addresses.CurrentAddressProvider
import org.BigDefi.data.config.Settings
import org.BigDefi.debug.DebugBigDefiActivity
import org.BigDefi.preferences.PreferenceActivity
import org.BigDefi.securityinfo.SecurityInfoActivity
import org.BigDefi.toolbar.colorize
import org.BigDefi.transactions.OfflineTransactionActivity
import java.security.KeyStore

class WalletNavigationFragment : Fragment() {

    val keyStore: KeyStore by inject()
    val settings: Settings by inject()
    val chainInfoProvider: ChainInfoProvider by inject()
    val currentAddressProvider: CurrentAddressProvider by inject()
    val appDatabase: AppDatabase by inject()

    private val navigationView by lazy {
        NavigationView(activity).apply {
            inflateMenu(R.menu.navigation_drawer)
            inflateHeaderView(R.layout.navigation_drawer_header)
            getHeaderView(0).edit_account_image.setOnClickListener {
                context.startActivityFromClass(EditAccountActivity::class.java)
            }

        }
    }

    override fun onResume() {
        super.onResume()
        (navigationView.getHeaderView(0) as ViewGroup).apply {
            setBackgroundColor(settings.toolbarBackgroundColor)
            colorize(settings.toolbarForegroundColor)
        }

        navigationView.menu.findItem(R.id.menu_debug).isVisible = settings.showDebug
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val idToClassMap = mapOf(
                R.id.menu_switch_chain to SwitchChainActivity::class,
                R.id.menu_debug to DebugBigDefiActivity::class,
                R.id.menu_accounts to SwitchAccountActivity::class,
                R.id.menu_offline_transaction to OfflineTransactionActivity::class,
                R.id.menu_settings to PreferenceActivity::class,
                R.id.menu_security to SecurityInfoActivity::class,
                R.id.menu_tincubeth to TincubETHActivity::class
        )


        navigationView.setNavigationItemSelectedListener {
            view!!.rootView.drawer_layout.closeDrawers()
            val classToStart = idToClassMap[it.itemId]
            if (classToStart != null) {
                context?.startActivityFromClass(classToStart)
                true
            } else {
                false
            }
        }

        currentAddressProvider.observe(this, Observer { address ->
            appDatabase.addressBook.byAddressLiveData(address!!).observe(this@WalletNavigationFragment, Observer { currentAddress ->
                navigationView.getHeaderView(0).let { header ->
                    currentAddress?.let { entry ->
                        header.accountHash.text = entry.address.hex
                        header.accountName.text = entry.name
                    }
                }

            })
        })

        chainInfoProvider.observe(this, Observer {
            val networkName = chainInfoProvider.value?.name
            navigationView.menu.findItem(R.id.menu_switch_chain).title = "Chain: $networkName"
        })

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = navigationView

}
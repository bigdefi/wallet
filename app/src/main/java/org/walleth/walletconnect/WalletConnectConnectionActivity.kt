package org.BigDefi.walletconnect

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import coil.api.load
import kotlinx.android.synthetic.main.activity_wallet_connect.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kethereum.erc681.ERC681
import org.kethereum.erc681.generateURL
import org.kethereum.extensions.hexToBigInteger
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.komputing.khex.model.HexString
import org.ligi.kaxt.setVisibility
import org.ligi.kaxtui.alert
import org.walletconnect.Session
import org.walletconnect.Session.Status.Approved
import org.walletconnect.Session.Status.Closed
import org.BigDefi.R
import org.BigDefi.accounts.AccountPickActivity
import org.BigDefi.base_activities.BaseSubActivity
import org.BigDefi.chains.ChainInfoProvider
import org.BigDefi.chains.SwitchChainActivity
import org.BigDefi.data.EXTRA_KEY_ADDRESS
import org.BigDefi.data.REQUEST_CODE_SELECT_TO_ADDRESS
import org.BigDefi.data.addresses.CurrentAddressProvider
import org.BigDefi.sign.SignTextActivity
import org.BigDefi.transactions.CreateTransactionActivity

fun Context.getWalletConnectIntent(data: Uri) = Intent(this, WalletConnectConnectionActivity::class.java).apply {
    setData(data)
}

private const val REQUEST_ID_SIGN_TEXT = 100
private const val REQUEST_ID_SIGN_TX = 101
private const val REQUEST_ID_SWITCH_NET = 102

class WalletConnectConnectionActivity : BaseSubActivity() {

    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val currentNetworkProvider: ChainInfoProvider by inject()

    private val wcViewModel: WalletConnectViewModel by viewModel()

    private var currentRequestId: Long? = null

    private var accounts = listOf<String>()

    private val sessionCallback = object : Session.Callback {
        override fun onMethodCall(call: Session.MethodCall) {
            lifecycleScope.launch(Dispatchers.Main) {
                when (call) {
                    is Session.MethodCall.SessionRequest -> {
                        wcViewModel.peerMeta = call.peer.meta
                        wcViewModel.statusText = "waiting for interactions with " + call.peer.meta?.name
                        wcViewModel.iconURL = call.peer.meta?.icons?.firstOrNull()
                        applyViewModel()

                        requestInitialAccount()
                    }

                    is Session.MethodCall.SignMessage -> {
                        currentRequestId = call.id
                        val intent = Intent(this@WalletConnectConnectionActivity, SignTextActivity::class.java).apply {
                            putExtra(Intent.EXTRA_TEXT, call.message)

                        }
                        startActivityForResult(intent, REQUEST_ID_SIGN_TEXT)
                    }

                    is Session.MethodCall.SendTransaction -> {
                        currentRequestId = call.id
                        lifecycleScope.launch(Dispatchers.Main) {
                            val url = ERC681(scheme = "ethereum",
                                    address = call.to,
                                    value = HexString(call.value).hexToBigInteger(),
                                    gas = call.gasLimit?.let { HexString(it).hexToBigInteger() }
                            ).generateURL()


                            val intent = Intent(this@WalletConnectConnectionActivity, CreateTransactionActivity::class.java).apply {
                                this.data = Uri.parse(url)
                                if (call.data.isNotEmpty()) {
                                    putExtra("data", call.data)
                                }

                                putExtra("gasPrice", call.gasPrice)
                                putExtra("nonce", call.nonce)
                                putExtra("from", call.from)
                                putExtra("parityFlow", false)
                            }

                            startActivityForResult(intent, REQUEST_ID_SIGN_TX)
                        }
                    }

                    is Session.MethodCall.Custom -> {
                        currentRequestId = call.id
                        if (call.method == "personal_sign") {
                            if (call.params == null) {
                                alert("got personal_sign without parameters")
                            } else {
                                signText("" + call.params!!.first())
                            }
                        }
                    }
                    else -> {
                        lifecycleScope.launch(Dispatchers.Main) {
                            alert("" + call)
                        }
                    }
                }
            }

        }

        override fun onStatus(status: Session.Status) {
            lifecycleScope.launch(Dispatchers.Main) {
                when (status) {
                    is Error -> alert("Error:" + status.message)
                    is Approved -> {
                        wcViewModel.showSwitchAccountButton = true
                        wcViewModel.showSwitchNetworkButton = true
                        applyViewModel()
                    }
                    is Closed -> {
                        finish()
                    }
                    is Session.Status.Connected -> {
                        //requestInitialAccount()
                    }
                    else -> alert("Error:" + status)
                }

            }

        }


    }

    private fun signText(message: String) {
        val intent = Intent(this@WalletConnectConnectionActivity, SignTextActivity::class.java).apply {
            putExtra(Intent.EXTRA_TEXT, message)

        }
        startActivityForResult(intent, REQUEST_ID_SIGN_TEXT)
    }

    private fun requestInitialAccount(): AlertDialog? {
        return AlertDialog.Builder(this@WalletConnectConnectionActivity)
                .setTitle(getString(R.string.walletconnect_do_you_want_to_use, wcViewModel.peerMeta?.name))
                .setItems(R.array.walletconnect_options) { _, i ->
                    when (i) {
                        0 -> {
                            accounts = listOf(currentAddressProvider.getCurrentNeverNull().hex)
                            wcViewModel.session?.approve(accounts, currentNetworkProvider.getCurrent()!!.chainId.toLong())
                        }

                        1 -> selectAccount()

                        else -> {
                            wcViewModel.session?.reject()
                            finish()
                        }
                    }
                }
                .setOnCancelListener { finish() }
                .show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_wallet_connect)
        supportActionBar?.subtitle = getString(R.string.wallet_connect)

        wc_change_account.setOnClickListener {
            selectAccount()
        }

        wc_change_network.setOnClickListener {
            startActivityForResult(Intent(this@WalletConnectConnectionActivity, SwitchChainActivity::class.java), REQUEST_ID_SWITCH_NET)
        }

        if (!wcViewModel.processURI(intent?.data.toString())) {
            requestInitialAccount()
        }
    }

    override fun onResume() {
        super.onResume()
        applyViewModel()

        wcViewModel.session?.addCallback(sessionCallback)
    }

    override fun onPause() {
        super.onPause()

        wcViewModel.session?.removeCallback(sessionCallback)
    }

    private fun applyViewModel() {

        wc_change_account.setVisibility(wcViewModel.showSwitchAccountButton)
        wc_change_network.setVisibility(wcViewModel.showSwitchNetworkButton)
        status_text.text = wcViewModel.statusText
        wcViewModel.iconURL?.let { url ->
            dapp_icon.load(url)
        }
    }

    private fun selectAccount() {
        val intent = Intent(this@WalletConnectConnectionActivity, AccountPickActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_SELECT_TO_ADDRESS)
    }

    override fun onDestroy() {
        super.onDestroy()
        wcViewModel.session?.kill()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        wcViewModel.session?.addCallback(sessionCallback)
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_ID_SWITCH_NET -> {
                wcViewModel.session?.approve(accounts, currentNetworkProvider.getCurrent()!!.chainId.toLong())
            }


            REQUEST_CODE_SELECT_TO_ADDRESS -> {
                if (data?.hasExtra(EXTRA_KEY_ADDRESS) == true) {
                    val addressHex = data.getStringExtra(EXTRA_KEY_ADDRESS)
                    currentAddressProvider.setCurrent(Address(addressHex))
                    accounts = listOf(addressHex)
                    wcViewModel.session?.approve(accounts, currentNetworkProvider.getCurrent()!!.chainId.toLong())
                }
            }

            REQUEST_ID_SIGN_TEXT -> {
                if (data?.hasExtra("SIGNATURE") == true) {
                    val result = data.getStringExtra("SIGNATURE")
                    wcViewModel.session?.approveRequest(currentRequestId!!, "0x$result")
                } else {
                    wcViewModel.session?.rejectRequest(currentRequestId!!, 1L, "user canceled")
                }

            }

            REQUEST_ID_SIGN_TX -> {
                if (data?.hasExtra("TXHASH") == true) {
                    val result = data.getStringExtra("TXHASH")
                    wcViewModel.session?.approveRequest(currentRequestId!!, result)
                }

            }

            else -> {
                alert("unknown")
            }
        }

    }
}

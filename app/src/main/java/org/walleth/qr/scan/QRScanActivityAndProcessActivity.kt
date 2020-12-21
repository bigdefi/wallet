package org.BigDefi.qr.scan

import android.net.Uri
import androidx.appcompat.app.AlertDialog
import org.kethereum.erc681.ERC681
import org.kethereum.erc681.generateURL
import org.kethereum.erc831.isEthereumURLString
import org.BigDefi.R
import org.BigDefi.accounts.KeyType
import org.BigDefi.accounts.getCreateImportIntentFor
import org.BigDefi.intents.getEthereumViewIntent
import org.BigDefi.transactions.getOfflineTransactionIntent
import org.BigDefi.util.isJSONKey
import org.BigDefi.util.isParityUnsignedTransactionJSON
import org.BigDefi.util.isSignedTransactionJSON
import org.BigDefi.util.isUnsignedTransactionJSON
import org.BigDefi.walletconnect.getWalletConnectIntent

class QRScanActivityAndProcessActivity : QRScanActivity() {

    override fun finishWithResult(value: String) {

        when {
            value.startsWith("wc:") -> {
                startActivity(getWalletConnectIntent(Uri.parse(value)))
            }

            value.isEthereumURLString() -> {
                startActivity(getEthereumViewIntent(value))
            }

            value.length == 64 -> {
                startActivity(getCreateImportIntentFor(value, KeyType.ECDSA))
            }

            value.isJSONKey() -> {
                startActivity(getCreateImportIntentFor(value, KeyType.JSON))
            }

            value.isUnsignedTransactionJSON() || value.isSignedTransactionJSON() || value.isParityUnsignedTransactionJSON() -> {
                startActivity(getOfflineTransactionIntent(value))
            }

            value.startsWith("0x") -> {
                startActivity(getEthereumViewIntent(ERC681(address = value).generateURL()))
            }

            else -> {
                AlertDialog.Builder(this)
                        .setMessage(R.string.scan_not_interpreted_error_message)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            this@QRScanActivityAndProcessActivity.finish()
                        }
                        .show()
            }
        }
    }

}
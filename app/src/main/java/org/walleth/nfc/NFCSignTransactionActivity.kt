package org.BigDefi.nfc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kethereum.crypto.toAddress
import org.kethereum.extensions.transactions.encodeRLP
import org.kethereum.keccakshortcut.keccak
import org.koin.android.ext.android.inject
import org.komputing.khex.extensions.toHexString
import org.BigDefi.data.AppDatabase
import org.BigDefi.data.REQUEST_CODE_NFC
import org.BigDefi.data.transactions.TransactionState
import org.BigDefi.data.transactions.toEntity
import org.BigDefi.kethereum.android.TransactionParcel
import org.BigDefi.khartwarewallet.KHardwareChannel

private const val KEY_TRANSACTION = "TX"

fun Activity.startNFCSigningActivity(transactionParcel: TransactionParcel) {

    val nfcIntent = Intent(this, NFCSignTransactionActivity::class.java).putExtra(KEY_TRANSACTION, transactionParcel)
    startActivityForResult(nfcIntent, REQUEST_CODE_NFC)
}

class NFCSignTransactionActivity : NFCBaseActivityWithPINHandling() {

    private val transaction by lazy { intent.getParcelableExtra<TransactionParcel>("TX") }

    private val appDatabase: AppDatabase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.subtitle = "Sign Transaction via NFC"
    }

    override fun afterCorrectPin(channel: KHardwareChannel) {

        val address = channel.toPublicKey().toAddress()

        if (transaction.transaction.from != address) {
            setText("The given card does not match the account")
        } else {
            setText("signing")


            val oldHash = transaction.transaction.txHash

            val signedTransaction = channel.signTransaction(transaction.transaction)

            setText("signed")
            lifecycleScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.Default) {
                    transaction.transaction.txHash = signedTransaction.encodeRLP().keccak().toHexString()
                    appDatabase.runInTransaction {
                        oldHash?.let { appDatabase.transactions.deleteByHash(it) }
                        appDatabase.transactions.upsert(transaction.transaction.toEntity(signedTransaction.signatureData, TransactionState()))
                    }
                }
                setResult(RESULT_OK, Intent().apply { putExtra("TXHASH", transaction.transaction.txHash) })
                finish()
            }
        }
    }

}

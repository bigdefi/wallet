package org.BigDefi.geth.services

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ethereum.geth.*
import org.kethereum.extensions.transactions.encodeRLP
import org.koin.android.ext.android.inject
import org.ligi.tracedroid.logging.Log
import org.BigDefi.R
import org.BigDefi.chains.ChainInfoProvider
import org.BigDefi.data.AppDatabase
import org.BigDefi.data.addresses.CurrentAddressProvider
import org.BigDefi.data.balances.Balance
import org.BigDefi.data.config.Settings
import org.BigDefi.data.syncprogress.SyncProgressProvider
import org.BigDefi.data.syncprogress.BigDefiSyncProgress
import org.BigDefi.data.tokens.getRootToken
import org.BigDefi.data.transactions.TransactionEntity
import org.BigDefi.geth.toGethAddr
import org.BigDefi.overview.OverviewActivity
import java.io.File
import java.math.BigInteger
import org.ethereum.geth.Context as EthereumContext

private const val NOTIFICATION_ID = 101
private const val NOTIFICATION_CHANNEL_ID = "geth"

class GethLightEthereumService : LifecycleService() {

    companion object {
        const val STOP_SERVICE_ACTION = "STOPSERVICE"
        fun Context.gethStopIntent() = Intent(this, GethLightEthereumService::class.java).apply {
            action = STOP_SERVICE_ACTION
        }

        var shouldRun = false
        var isRunning = false
    }

    private val syncProgress: SyncProgressProvider by inject()
    private val appDatabase: AppDatabase by inject()
    private val settings: Settings by inject()
    private val networkDefinitionProvider: ChainInfoProvider by inject()
    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val path by lazy { File(baseContext.cacheDir, "ethereumdata").absolutePath }
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private var isSyncing = false
    private var finishedSyncing = false

    private var shouldRestart = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == STOP_SERVICE_ACTION) {
            shouldRun = false
            return START_NOT_STICKY
        }

        shouldRun = true


        lifecycleScope.launch(Dispatchers.Default) {
            Geth.setVerbosity(settings.currentGoVerbosity.toLong())
            val ethereumContext = EthereumContext()

            var initial = true
            while (shouldRestart || initial) {
                initial = false
                shouldRestart = false // just did restart
                shouldRun = true

                async(Dispatchers.Main) {
                    val pendingStopIntent = PendingIntent.getService(baseContext, 0, gethStopIntent(), 0)
                    val contentIntent = PendingIntent.getActivity(baseContext, 0, Intent(baseContext, OverviewActivity::class.java), 0)

                    if (Build.VERSION.SDK_INT > 25) {
                        setNotificationChannel()
                    }

                    val notification = NotificationCompat.Builder(this@GethLightEthereumService, NOTIFICATION_CHANNEL_ID)
                            .setContentTitle(getString(R.string.geth_service_notification_title))
                            .setContentText(resources.getString(R.string.geth_service_notification_content_text, networkDefinitionProvider.getCurrent()?.name))
                            .setContentIntent(contentIntent)
                            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "exit", pendingStopIntent)
                            .setSmallIcon(R.drawable.notification)
                            .build()

                    startForeground(NOTIFICATION_ID, notification)
                }.await()

                val network = networkDefinitionProvider.getCurrent()

                if (network != null) {
                    networkDefinitionProvider.observe(this@GethLightEthereumService, Observer {
                        if (network != networkDefinitionProvider.getCurrent()) {
                            shouldRun = false
                            shouldRestart = true
                        }
                    })

                    val subPath = File(path, "chain" + network.chainId)
                    subPath.mkdirs()
                    val nodeConfig = NodeConfig().apply {

                        ethereumNetworkID = network.chainId.toLong()

                        ethereumGenesis = when (ethereumNetworkID) {
                            1L -> Geth.mainnetGenesis()
                            3L -> Geth.testnetGenesis()
                            4L -> Geth.rinkebyGenesis()
                            else -> throw (IllegalStateException("NO genesis"))
                        }

                    }
                    val ethereumNode = Geth.newNode(subPath.absolutePath, nodeConfig)

                    Log.i("Starting Node for " + nodeConfig.ethereumNetworkID)
                    ethereumNode.start()
                    isRunning = true
                    while (shouldRun && !finishedSyncing) {
                        delay(1000)
                        syncTick(ethereumNode, ethereumContext)
                    }
                    val transactionsLiveData = appDatabase.transactions.getAllToRelayLive()
                    val transactionObserver = Observer<List<TransactionEntity>> {
                        it?.forEach { transaction ->
                            transaction.execute(ethereumNode.ethereumClient, ethereumContext)
                        }
                    }
                    transactionsLiveData.observe(this@GethLightEthereumService, transactionObserver)
                    try {
                        ethereumNode.ethereumClient.subscribeNewHead(ethereumContext, object : NewHeadHandler {
                            override fun onNewHead(p0: Header) {
                                val address = currentAddressProvider.getCurrentNeverNull()
                                val gethAddress = address.toGethAddr()
                                val balance = ethereumNode.ethereumClient.getBalanceAt(ethereumContext, gethAddress, p0.number)
                                appDatabase.balances.upsert(Balance(
                                        address = address,
                                        tokenAddress = network.getRootToken().address,
                                        chain = network.chainId,
                                        balance = BigInteger(balance.string()),
                                        block = p0.number))
                            }

                            override fun onError(p0: String?) {}

                        }, 16)

                    } catch (e: Exception) {
                        Log.e("node error", e)
                    }

                    while (shouldRun) {
                        syncTick(ethereumNode, ethereumContext)
                    }

                    async(Dispatchers.Main) {
                        transactionsLiveData.removeObserver(transactionObserver)
                        launch {
                            ethereumNode.stop()
                        }

                        if (!shouldRestart) {
                            stopForeground(true)
                            stopSelf()
                            isRunning = false
                        } else {
                            notificationManager.cancel(NOTIFICATION_ID)
                        }
                    }.await()
                }
            }

        }
        return START_NOT_STICKY
    }

    @TargetApi(26)
    private fun setNotificationChannel() {
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Geth Service", IMPORTANCE_HIGH)
        channel.description = getString(R.string.geth_service_notification_channel_description)
        notificationManager.createNotificationChannel(channel)
    }

    private suspend fun syncTick(ethereumNode: Node, ethereumContext: EthereumContext) {
        try {
            val ethereumSyncProgress = ethereumNode.ethereumClient.syncProgress(ethereumContext)

            lifecycleScope.async(Dispatchers.Main) {
                if (ethereumSyncProgress != null) {
                    isSyncing = true
                    val newSyncProgress = ethereumSyncProgress.let {
                        BigDefiSyncProgress(true, it.currentBlock, it.highestBlock)
                    }
                    syncProgress.postValue(newSyncProgress)
                } else {
                    syncProgress.postValue(BigDefiSyncProgress())
                    if (isSyncing) {
                        finishedSyncing = true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        delay(1000)
    }


    private fun TransactionEntity.execute(client: EthereumClient, ethereumContext: EthereumContext) {
        try {
            val rlp = transaction.encodeRLP()
            val transactionWithSignature = Geth.newTransactionFromRLP(rlp)
            client.sendTransaction(ethereumContext, transactionWithSignature)
            transactionState.relayed = "GethLight"
        } catch (e: Exception) {
            transactionState.error = e.message
        }
    }

}

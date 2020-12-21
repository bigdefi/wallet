package org.BigDefi

import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.os.StrictMode
import androidx.annotation.XmlRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Observer
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceScreen
import androidx.room.Room
import com.chibatching.kotpref.Kotpref
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.kethereum.keystore.api.InitializingFileKeyStore
import org.kethereum.keystore.api.KeyStore
import org.kethereum.methodsignatures.CachedOnlineMethodSignatureRepository
import org.kethereum.methodsignatures.CachedOnlineMethodSignatureRepositoryImpl
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.ligi.tracedroid.TraceDroid
import org.walletconnect.impls.FileWCSessionStore
import org.walletconnect.impls.WCSessionStore
import org.BigDefi.activities.findChainsWithTincubethSupportAndStore
import org.BigDefi.chains.ChainInfoProvider
import org.BigDefi.data.*
import org.BigDefi.data.addresses.*
import org.BigDefi.data.blockexplorer.BlockExplorerProvider
import org.BigDefi.data.chaininfo.ChainInfo
import org.BigDefi.data.config.KotprefSettings
import org.BigDefi.data.config.Settings
import org.BigDefi.data.ens.ENSProvider
import org.BigDefi.data.ens.ENSProviderImpl
import org.BigDefi.data.exchangerate.CryptoCompareExchangeProvider
import org.BigDefi.data.exchangerate.ExchangeRateProvider
import org.BigDefi.data.rpc.RPCProvider
import org.BigDefi.data.rpc.RPCProviderImpl
import org.BigDefi.data.syncprogress.SyncProgressProvider
import org.BigDefi.data.tokens.CurrentTokenProvider
import org.BigDefi.data.tokens.getRootToken
import org.BigDefi.migrations.ChainAddingAndRecreatingMigration
import org.BigDefi.migrations.TransactionExtendingMigration
import org.BigDefi.nfc.NFCCredentialStore
import org.BigDefi.notifications.TransactionNotificationService
import org.BigDefi.overview.TransactionListViewModel
import org.BigDefi.startup.StartupViewModel
import org.BigDefi.util.DelegatingSocketFactory
import org.BigDefi.util.jsonadapter.BigIntegerJSONAdapter
import org.BigDefi.walletconnect.WalletConnectViewModel
import java.io.File
import java.net.Socket
import java.security.Security
import javax.net.SocketFactory


open class App : MultiDexApplication() {

    private val koinModule = module {
        single { Moshi.Builder().add(BigIntegerJSONAdapter()).build() }
    }

    private val keyStore by lazy { InitializingFileKeyStore(File(filesDir, "keystore")) }
    val appDatabase: AppDatabase by inject()
    val settings: Settings by inject()

    open fun createKoin() = module {
        single { CryptoCompareExchangeProvider(this@App, get()) as ExchangeRateProvider }
        single { SyncProgressProvider() }
        single { keyStore as KeyStore }
        single { KotprefSettings as Settings }
        single { CurrentTokenProvider(get()) }
        single { RPCProviderImpl(this@App, get(), get(), get()) as RPCProvider }
        single { ENSProviderImpl(get()) as ENSProvider }
        single {
            Room.databaseBuilder(applicationContext, AppDatabase::class.java, "maindb")
                    .addMigrations(
                            ChainAddingAndRecreatingMigration(1),
                            ChainAddingAndRecreatingMigration(2),
                            ChainAddingAndRecreatingMigration(3),
                            ChainAddingAndRecreatingMigration(4),
                            TransactionExtendingMigration()
                    ).build()
        }

        single { ChainInfoProvider(get(), get(), get(), assets) }
        single { BlockExplorerProvider(get()) }
        single {
            InitializingCurrentAddressProvider(settings = get()) as CurrentAddressProvider
        }
        single {
            CachedOnlineMethodSignatureRepositoryImpl(get(), File(cacheDir, "funsignatures").apply {
                mkdirs()
            }) as CachedOnlineMethodSignatureRepository
        }

        single {
            FileWCSessionStore(File(this@App.filesDir, "walletConnectSessions.json").apply {
                createNewFile()
            }, get()) as WCSessionStore
        }

        single {
            NFCCredentialStore(this@App)
        }

        single {
            val socketFactory = object : DelegatingSocketFactory(SocketFactory.getDefault()) {
                override fun configureSocket(socket: Socket): Socket {
                    // https://github.com/BigDefi/BigDefi/issues/164
                    // https://github.com/square/okhttp/issues/3537
                    TrafficStats.tagSocket(socket)

                    return socket
                }
            }
            OkHttpClient.Builder().socketFactory(socketFactory).build()
        }
        viewModel { TransactionListViewModel(this@App, get(), get(), get()) }
        viewModel { WalletConnectViewModel(this@App, get(), get(), get()) }
        viewModel { StartupViewModel(get(), get()) }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()

        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(listOf(koinModule, createKoin()))
        }

        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build())
        }

        Kotpref.init(this)
        TraceDroid.init(this)
        AndroidThreeTen.init(this)
        applyNightMode(settings)
        executeCodeWeWillIgnoreInTests()
        if (settings.addressInitVersion < 2) {
            settings.addressInitVersion = 2

            GlobalScope.launch(Dispatchers.Default) {
                appDatabase.addressBook.upsert(allPrePopulationAddresses)
            }
        }
        postInitCallbacks.forEach { it.invoke() }

        val currentTokenProvider: CurrentTokenProvider by inject()
        val chainInfoProvider: ChainInfoProvider by inject()

        val initialChainObserver = object : Observer<ChainInfo> {
            override fun onChanged(chainInfo: ChainInfo?) {
                chainInfo?.getRootToken()?.let { rootToken ->
                    initTokens(settings, assets, appDatabase)
                    currentTokenProvider.setCurrent(rootToken)
                    chainInfoProvider.removeObserver(this)
                }

                GlobalScope.launch(Dispatchers.Default) {
                    if (settings.dataVersion < 4) {
                        findChainsWithTincubethSupportAndStore(this@App, appDatabase)
                    }
                    if (settings.dataVersion < 3) {
                        val all = appDatabase.chainInfo.getAll()
                        var currentMin = all.filter { it.order != null }.minBy { it.order!! }?.order ?: 0
                        all.forEach {
                            if (it.order == null) {
                                it.order = currentMin
                            }
                            currentMin -= 10
                        }
                        appDatabase.chainInfo.upsert(all)
                    }
                    if (settings.dataVersion < 1) {
                        appDatabase.addressBook.all().forEach {
                            if (it.keySpec == null || it.keySpec?.isBlank() == true) {
                                val type = if (keyStore.hasKeyForForAddress(it.address)) ACCOUNT_TYPE_BURNER else ACCOUNT_TYPE_WATCH_ONLY
                                it.keySpec = AccountKeySpec(type).toJSON()
                                appDatabase.addressBook.upsert(it)
                            } else if (it.keySpec?.startsWith("m") == true) {
                                it.keySpec = AccountKeySpec(ACCOUNT_TYPE_TREZOR, derivationPath = it.keySpec).toJSON()
                                appDatabase.addressBook.upsert(it)
                            }
                        }
                    }
                    settings.dataVersion = 4
                }

                isInitialized = true
            }
        }

        chainInfoProvider.observeForever(initialChainObserver)

    }

    open fun executeCodeWeWillIgnoreInTests() {
        try {
            startService(Intent(this, TransactionNotificationService::class.java))
        } catch (e: IllegalStateException) {
        }
    }

    companion object {
        var isInitialized = false
        val postInitCallbacks = mutableListOf<() -> Unit>()
        val extraPreferences = mutableListOf<Pair<@XmlRes Int, (preferenceScreen: PreferenceScreen) -> Unit>>()

        fun applyNightMode(settings: Settings) {
            @AppCompatDelegate.NightMode val nightMode = settings.getNightMode()
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }
}


package org.BigDefi.activities

import android.content.Context
import android.os.Bundle
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_in3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.BigDefi.R
import org.BigDefi.base_activities.BaseSubActivity
import org.BigDefi.data.AppDatabase
import org.BigDefi.data.chaininfo.ChainInfo
import org.BigDefi.data.rpc.KEY_IN3_RPC
import org.BigDefi.util.hasTincubethSupport
import java.math.BigInteger

class TincubETHActivity : BaseSubActivity() {

    val appDatabase: AppDatabase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_in3)

        supportActionBar?.subtitle = "TinCubETH preferences"

        security_seek.max = 29
        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(p0: SeekBar?) {}

            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                refresh()
            }
        }
        security_seek.setOnSeekBarChangeListener(listener)

        privacy_seek.max = 29
        privacy_seek.setOnSeekBarChangeListener(listener)
        refresh()
    }

    fun refresh() {
        security_details_text.text = when (security_seek.progress / 10) {
            0 -> "-> Weak security but cheaper and faster"
            1 -> "-> Better security but also more expensive and slower"
            2 -> "-> Maximum security but also most expensive and slow"
            else -> TODO()
        }
        privacy_details_text.text = when (privacy_seek.progress / 10) {
            0 -> "-> Weak privacy but faster and cheaper"
            1 -> "-> Better privacy but also more expensive and slower"
            2 -> "-> Maximum privacy but also most expensive and slow"
            else -> TODO()
        }
    }
}

suspend fun findChainsWithTincubethSupportAndStore(context: Context, appDatabase: AppDatabase): List<ChainInfo> {
    val res = findTincubethChains(appDatabase)
    res.forEach {
        if (!it.hasTincubethSupport()) {
            appDatabase.chainInfo.upsert(it.copy(rpc = it.rpc + KEY_IN3_RPC))
        }
    }
    return res
}

private suspend fun findTincubethChains(appDatabase: AppDatabase) = withContext(Dispatchers.IO) {
    val res = appDatabase.chainInfo.getAll().filter {
        it.chainId == BigInteger.ONE || it.chainId == BigInteger.valueOf(5L)
    }
    res
}

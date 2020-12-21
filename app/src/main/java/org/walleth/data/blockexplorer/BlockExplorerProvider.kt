package org.BigDefi.data.blockexplorer

import android.content.Context
import org.kethereum.model.BlockExplorer
import org.ligi.kaxtui.alert
import org.BigDefi.chains.ChainInfoProvider
import org.BigDefi.kethereum.blockscout.getBlockScoutBlockExplorer
import org.BigDefi.kethereum.etherscan.getEtherScanBlockExplorer

class BlockExplorerProvider(var network: ChainInfoProvider) {

    fun get() = getBlockScoutBlockExplorer(network.getCurrentChainId())?: getEtherScanBlockExplorer(network.getCurrentChainId())

    fun getOrAlert(context: Context): BlockExplorer? {
        val result = get()
        if (result == null) {
            context.alert("No blockExplorer found for the current Network")
        }
        return result
    }
}


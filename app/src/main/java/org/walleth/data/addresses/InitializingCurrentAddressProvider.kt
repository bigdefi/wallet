package org.BigDefi.data.addresses

import org.kethereum.model.Address
import org.BigDefi.data.config.Settings

class InitializingCurrentAddressProvider(settings: Settings) : CurrentAddressProvider(settings) {

    init {
        val lastAddress = settings.accountAddress
        if (lastAddress != null) {
            setCurrent(Address(lastAddress))
        }
    }

}
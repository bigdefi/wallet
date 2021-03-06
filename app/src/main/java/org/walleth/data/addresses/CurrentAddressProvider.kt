package org.BigDefi.data.addresses

import androidx.lifecycle.MutableLiveData
import org.kethereum.model.Address
import org.BigDefi.data.config.Settings

open class CurrentAddressProvider(val settings: Settings) : MutableLiveData<Address>() {

    fun setCurrent(value: Address) {
        settings.accountAddress = value.hex
        setValue(value)
    }

    fun getCurrent() = value
    fun getCurrentNeverNull() = value!!
}
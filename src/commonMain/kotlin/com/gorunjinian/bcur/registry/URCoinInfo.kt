package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/** UR variant of [CryptoCoinInfo] (CBOR tag 40305). */
class URCoinInfo : CryptoCoinInfo {

    constructor(type: Int?, network: Int?) : super(type, network)
    constructor(type: Type?, network: Network?) : super(type, network)

    override val registryType: RegistryType get() = RegistryType.COIN_INFO

    companion object {
        fun fromCbor(item: CborItem): URCoinInfo {
            var type: Int? = null
            var network: Int? = null

            val map = item as CborItem.Map
            for ((k, v) in map.entries) {
                when ((k as CborItem.UInt).toInt()) {
                    TYPE_KEY -> type = (v as CborItem.UInt).toInt()
                    NETWORK_KEY -> network = (v as CborItem.UInt).toInt()
                }
            }

            return URCoinInfo(type, network)
        }
    }
}

package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/** UR variant of [CryptoAddress] (CBOR tag 40307). */
class URAddress(
    info: CryptoCoinInfo?,
    type: Type?,
    data: ByteArray
) : CryptoAddress(info, type, data) {

    override val registryType: RegistryType get() = RegistryType.ADDRESS

    companion object {
        fun fromCbor(item: CborItem): URAddress {
            var info: URCoinInfo? = null
            var type: Type? = null
            var data: ByteArray? = null

            val map = item as CborItem.Map
            for ((k, v) in map.entries) {
                when ((k as CborItem.UInt).toInt()) {
                    INFO_KEY -> info = URCoinInfo.fromCbor(v)
                    TYPE_KEY -> type = Type.entries[(v as CborItem.UInt).toInt()]
                    DATA_KEY -> data = (v as CborItem.Bytes).value
                }
            }

            requireNotNull(data) { "Address data is null" }
            return URAddress(info, type, data)
        }
    }
}

package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/** UR variant of [CryptoECKey] (CBOR tag 40306). */
class URECKey(
    curve: Int?,
    privateKey: Boolean?,
    data: ByteArray
) : CryptoECKey(curve, privateKey, data) {

    override val registryType: RegistryType get() = RegistryType.ECKEY

    companion object {
        fun fromCbor(item: CborItem): URECKey {
            var curve: Int? = null
            var privateKey: Boolean? = null
            var data: ByteArray? = null

            val map = item as CborItem.Map
            for ((k, v) in map.entries) {
                when ((k as CborItem.UInt).toInt()) {
                    CURVE_KEY -> curve = (v as CborItem.UInt).toInt()
                    PRIVATE_KEY -> privateKey = (v as CborItem.Bool).value
                    DATA_KEY -> data = (v as CborItem.Bytes).value
                }
            }

            requireNotNull(data) { "EC key data is null" }
            return URECKey(curve, privateKey, data)
        }
    }
}

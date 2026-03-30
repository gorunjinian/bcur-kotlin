package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/**
 * BCR-2020-007: Cryptocurrency address (CBOR tag 307).
 */
open class CryptoAddress(
    val info: CryptoCoinInfo?,
    val type: Type?,
    val data: ByteArray
) : RegistryItem() {

    override val registryType: RegistryType get() = RegistryType.CRYPTO_ADDRESS

    override fun toCbor(): CborItem {
        val map = CborItem.Map()
        if (info != null) {
            map.put(CborItem.UInt(INFO_KEY), info.toCbor())
        }
        if (type != null) {
            map.put(CborItem.UInt(TYPE_KEY), CborItem.UInt(type.ordinal))
        }
        map.put(CborItem.UInt(DATA_KEY), CborItem.Bytes(data))
        return map
    }

    enum class Type {
        P2PKH, P2SH, P2WPKH
    }

    companion object {
        const val INFO_KEY = 1
        const val TYPE_KEY = 2
        const val DATA_KEY = 3

        fun fromCbor(item: CborItem): CryptoAddress {
            var info: CryptoCoinInfo? = null
            var type: Type? = null
            var data: ByteArray? = null

            val map = item as CborItem.Map
            for ((k, v) in map.entries) {
                when ((k as CborItem.UInt).toInt()) {
                    INFO_KEY -> info = CryptoCoinInfo.fromCbor(v)
                    TYPE_KEY -> type = Type.entries[(v as CborItem.UInt).toInt()]
                    DATA_KEY -> data = (v as CborItem.Bytes).value
                }
            }

            requireNotNull(data) { "Address data is null" }
            return CryptoAddress(info, type, data)
        }
    }
}

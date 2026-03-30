package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/**
 * BCR-2020-007: Elliptic curve key (CBOR tag 306).
 */
open class CryptoECKey(
    private val curve: Int?,
    private val privateKey: Boolean?,
    val data: ByteArray
) : RegistryItem() {

    val resolvedCurve: Int get() = curve ?: 0
    val isPrivateKey: Boolean get() = privateKey ?: false

    override val registryType: RegistryType get() = RegistryType.CRYPTO_ECKEY

    override fun toCbor(): CborItem {
        val map = CborItem.Map()
        if (curve != null) {
            map.put(CborItem.UInt(CURVE_KEY), CborItem.UInt(curve))
        }
        if (privateKey != null) {
            map.put(CborItem.UInt(PRIVATE_KEY), CborItem.Bool(privateKey))
        }
        map.put(CborItem.UInt(DATA_KEY), CborItem.Bytes(data))
        return map
    }

    companion object {
        const val CURVE_KEY = 1
        const val PRIVATE_KEY = 2
        const val DATA_KEY = 3

        fun fromCbor(item: CborItem): CryptoECKey {
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
            return CryptoECKey(curve, privateKey, data)
        }
    }
}

package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/**
 * Multi-signature key container for output descriptors.
 *
 * Implements [CborSerializable] but NOT [RegistryItem] — it cannot be
 * independently serialized to a UR, only used within [CryptoOutput].
 */
class MultiKey(
    val threshold: Int,
    val ecKeys: List<CryptoECKey>,
    val hdKeys: List<CryptoHDKey>
) : CborSerializable {

    override fun toCbor(): CborItem {
        val map = CborItem.Map()
        map.put(CborItem.UInt(THRESHOLD_KEY), CborItem.UInt(threshold))

        val array = CborItem.Array()
        if (ecKeys.isNotEmpty()) {
            for (ecKey in ecKeys) {
                val item = ecKey.toCbor()
                item.tagged(RegistryType.CRYPTO_ECKEY.tag!!.toULong())
                array.items.add(item)
            }
        } else {
            for (hdKey in hdKeys) {
                val item = hdKey.toCbor()
                item.tagged(RegistryType.CRYPTO_HDKEY.tag!!.toULong())
                array.items.add(item)
            }
        }
        map.put(CborItem.UInt(KEYS_KEY), array)
        return map
    }

    companion object {
        const val THRESHOLD_KEY = 1
        const val KEYS_KEY = 2

        fun fromCbor(item: CborItem): MultiKey {
            var threshold = 0
            val ecKeys = mutableListOf<CryptoECKey>()
            val hdKeys = mutableListOf<CryptoHDKey>()

            val map = item as CborItem.Map
            for ((k, v) in map.entries) {
                when ((k as CborItem.UInt).toInt()) {
                    THRESHOLD_KEY -> threshold = (v as CborItem.UInt).toInt()
                    KEYS_KEY -> {
                        val keysArray = v as CborItem.Array
                        for (keyItem in keysArray.items) {
                            val tag = keyItem.tags.firstOrNull()
                            when (tag) {
                                RegistryType.CRYPTO_ECKEY.tag!!.toULong() ->
                                    ecKeys.add(CryptoECKey.fromCbor(keyItem))
                                RegistryType.CRYPTO_HDKEY.tag!!.toULong() ->
                                    hdKeys.add(CryptoHDKey.fromCbor(keyItem))
                            }
                        }
                    }
                }
            }

            require(ecKeys.isNotEmpty() || hdKeys.isNotEmpty()) {
                "One or more of eckey or hdkey must be specified"
            }
            return MultiKey(threshold, ecKeys, hdKeys)
        }
    }
}

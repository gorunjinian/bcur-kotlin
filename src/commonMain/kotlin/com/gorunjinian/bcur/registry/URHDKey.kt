package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/**
 * UR variant of [CryptoHDKey] (CBOR tag 40303).
 *
 * Overrides [toCbor] to re-tag inner fields (useInfo, origin, children)
 * with the newer 40xxx tag numbers instead of the 30x tags.
 */
class URHDKey : CryptoHDKey {

    /** Master key constructor. */
    constructor(key: ByteArray, chainCode: ByteArray) : super(key, chainCode)

    /** Derived key constructor. */
    constructor(
        privateKey: Boolean?,
        key: ByteArray,
        chainCode: ByteArray?,
        useInfo: URCoinInfo?,
        origin: URKeypath?,
        children: URKeypath?,
        parentFingerprint: ByteArray?,
        name: String? = null,
        note: String? = null
    ) : super(privateKey, key, chainCode, useInfo, origin, children, parentFingerprint, name, note)

    override val registryType: RegistryType get() = RegistryType.HDKEY

    override fun toCbor(): CborItem {
        val map = super.toCbor() as CborItem.Map

        // Re-tag useInfo from 305 -> 40305
        retagEntry(map, USE_INFO_KEY, RegistryType.COIN_INFO.tag!!.toULong())
        // Re-tag origin from 304 -> 40304
        retagEntry(map, ORIGIN_KEY, RegistryType.KEYPATH.tag!!.toULong())
        // Re-tag children from 304 -> 40304
        retagEntry(map, CHILDREN_KEY, RegistryType.KEYPATH.tag.toULong())

        return map
    }

    private fun retagEntry(map: CborItem.Map, key: Int, newTag: ULong) {
        val keyULong = key.toULong()
        for ((k, v) in map.entries) {
            if (k is CborItem.UInt && k.value == keyULong) {
                v.tags.clear()
                v.tags.add(newTag)
                return
            }
        }
    }

    companion object {
        fun fromCbor(item: CborItem): URHDKey {
            var isMasterKey = false
            var isPrivateKey: Boolean? = null
            var keyData: ByteArray? = null
            var chainCode: ByteArray? = null
            var useInfo: URCoinInfo? = null
            var origin: URKeypath? = null
            var children: URKeypath? = null
            var parentFingerprint: ByteArray? = null
            var name: String? = null
            var note: String? = null

            val map = item as CborItem.Map
            for ((k, v) in map.entries) {
                when ((k as CborItem.UInt).toInt()) {
                    IS_MASTER_KEY -> isMasterKey = (v as CborItem.Bool).value
                    IS_PRIVATE_KEY -> isPrivateKey = (v as CborItem.Bool).value
                    KEY_DATA_KEY -> keyData = (v as CborItem.Bytes).value
                    CHAIN_CODE_KEY -> chainCode = (v as CborItem.Bytes).value
                    USE_INFO_KEY -> useInfo = URCoinInfo.fromCbor(v)
                    ORIGIN_KEY -> origin = URKeypath.fromCbor(v)
                    CHILDREN_KEY -> children = URKeypath.fromCbor(v)
                    PARENT_FINGERPRINT_KEY -> parentFingerprint =
                        uLongToFingerprint((v as CborItem.UInt).value, 4)
                    NAME_KEY -> name = (v as CborItem.Text).value
                    NOTE_KEY -> note = (v as CborItem.Text).value
                }
            }

            requireNotNull(keyData) { "Key data is null" }

            return if (isMasterKey) {
                requireNotNull(chainCode) { "Chain code is null for master key" }
                URHDKey(keyData, chainCode)
            } else {
                URHDKey(isPrivateKey, keyData, chainCode, useInfo, origin, children, parentFingerprint, name, note)
            }
        }
    }
}

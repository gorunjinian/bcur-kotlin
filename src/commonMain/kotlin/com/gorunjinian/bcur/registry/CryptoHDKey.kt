package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/**
 * BCR-2020-007: HD key (CBOR tag 303).
 *
 * Open for extension by [URHDKey] which re-tags inner fields with 40xxx tags.
 */
open class CryptoHDKey private constructor(
    val isMaster: Boolean,
    val privateKey: Boolean?,
    val key: ByteArray,
    val chainCode: ByteArray?,
    val useInfo: CryptoCoinInfo?,
    val origin: CryptoKeypath?,
    val children: CryptoKeypath?,
    parentFingerprint: ByteArray?,
    val name: String?,
    val note: String?
) : RegistryItem() {

    val isPrivateKey: Boolean get() = privateKey ?: false

    val parentFingerprint: ByteArray? = parentFingerprint?.let {
        it.copyOfRange(it.size - 4, it.size)
    }

    /** Master key constructor. */
    constructor(key: ByteArray, chainCode: ByteArray) : this(
        isMaster = true,
        privateKey = true,
        key = key,
        chainCode = chainCode,
        useInfo = null,
        origin = null,
        children = null,
        parentFingerprint = null,
        name = null,
        note = null
    )

    /** Derived key constructor. */
    constructor(
        privateKey: Boolean?,
        key: ByteArray,
        chainCode: ByteArray?,
        useInfo: CryptoCoinInfo?,
        origin: CryptoKeypath?,
        children: CryptoKeypath?,
        parentFingerprint: ByteArray?,
        name: String? = null,
        note: String? = null
    ) : this(
        isMaster = false,
        privateKey = privateKey,
        key = key,
        chainCode = chainCode,
        useInfo = useInfo,
        origin = origin,
        children = children,
        parentFingerprint = parentFingerprint,
        name = name,
        note = note
    )

    override val registryType: RegistryType get() = RegistryType.CRYPTO_HDKEY

    override fun toCbor(): CborItem {
        val map = CborItem.Map()
        if (isMaster) {
            map.put(CborItem.UInt(IS_MASTER_KEY), CborItem.Bool(true))
            map.put(CborItem.UInt(KEY_DATA_KEY), CborItem.Bytes(key))
            map.put(CborItem.UInt(CHAIN_CODE_KEY), CborItem.Bytes(chainCode!!))
        } else {
            if (privateKey != null) {
                map.put(CborItem.UInt(IS_PRIVATE_KEY), CborItem.Bool(privateKey))
            }
            map.put(CborItem.UInt(KEY_DATA_KEY), CborItem.Bytes(key))
            if (chainCode != null) {
                map.put(CborItem.UInt(CHAIN_CODE_KEY), CborItem.Bytes(chainCode))
            }
            if (useInfo != null) {
                val useInfoItem = useInfo.toCbor()
                useInfoItem.tagged(RegistryType.CRYPTO_COIN_INFO.tag!!.toULong())
                map.put(CborItem.UInt(USE_INFO_KEY), useInfoItem)
            }
            if (origin != null) {
                val originItem = origin.toCbor()
                originItem.tagged(RegistryType.CRYPTO_KEYPATH.tag!!.toULong())
                map.put(CborItem.UInt(ORIGIN_KEY), originItem)
            }
            if (children != null) {
                val childrenItem = children.toCbor()
                childrenItem.tagged(RegistryType.CRYPTO_KEYPATH.tag!!.toULong())
                map.put(CborItem.UInt(CHILDREN_KEY), childrenItem)
            }
            if (parentFingerprint != null) {
                map.put(
                    CborItem.UInt(PARENT_FINGERPRINT_KEY),
                    CborItem.UInt(fingerprintToULong(parentFingerprint))
                )
            }
            if (name != null) {
                map.put(CborItem.UInt(NAME_KEY), CborItem.Text(name))
            }
            if (note != null) {
                map.put(CborItem.UInt(NOTE_KEY), CborItem.Text(note))
            }
        }
        return map
    }

    companion object {
        const val IS_MASTER_KEY = 1
        const val IS_PRIVATE_KEY = 2
        const val KEY_DATA_KEY = 3
        const val CHAIN_CODE_KEY = 4
        const val USE_INFO_KEY = 5
        const val ORIGIN_KEY = 6
        const val CHILDREN_KEY = 7
        const val PARENT_FINGERPRINT_KEY = 8
        const val NAME_KEY = 9
        const val NOTE_KEY = 10

        fun fromCbor(item: CborItem): CryptoHDKey {
            var isMasterKey = false
            var isPrivateKey: Boolean? = null
            var keyData: ByteArray? = null
            var chainCode: ByteArray? = null
            var useInfo: CryptoCoinInfo? = null
            var origin: CryptoKeypath? = null
            var children: CryptoKeypath? = null
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
                    USE_INFO_KEY -> useInfo = CryptoCoinInfo.fromCbor(v)
                    ORIGIN_KEY -> origin = CryptoKeypath.fromCbor(v)
                    CHILDREN_KEY -> children = CryptoKeypath.fromCbor(v)
                    PARENT_FINGERPRINT_KEY -> parentFingerprint =
                        uLongToFingerprint((v as CborItem.UInt).value, 4)
                    NAME_KEY -> name = (v as CborItem.Text).value
                    NOTE_KEY -> note = (v as CborItem.Text).value
                }
            }

            requireNotNull(keyData) { "Key data is null" }

            return if (isMasterKey) {
                requireNotNull(chainCode) { "Chain code is null for master key" }
                CryptoHDKey(keyData, chainCode)
            } else {
                CryptoHDKey(isPrivateKey, keyData, chainCode, useInfo, origin, children, parentFingerprint, name, note)
            }
        }
    }
}

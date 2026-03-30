package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem
import com.gorunjinian.bcur.registry.pathcomponent.PathComponent

/**
 * BCR-2020-007: Key derivation path (CBOR tag 304).
 */
open class CryptoKeypath(
    val components: List<PathComponent>,
    sourceFingerprint: ByteArray?,
    val depth: Int? = null
) : RegistryItem() {

    val sourceFingerprint: ByteArray? = sourceFingerprint?.let {
        it.copyOfRange(it.size - 4, it.size)
    }

    val path: String?
        get() = if (components.isEmpty()) null
        else components.joinToString("/")

    override val registryType: RegistryType get() = RegistryType.CRYPTO_KEYPATH

    override fun toCbor(): CborItem {
        val map = CborItem.Map()
        map.put(CborItem.UInt(COMPONENTS_KEY), PathComponent.toCbor(components))
        if (sourceFingerprint != null) {
            map.put(
                CborItem.UInt(SOURCE_FINGERPRINT_KEY),
                CborItem.UInt(fingerprintToULong(sourceFingerprint))
            )
        }
        if (depth != null) {
            map.put(CborItem.UInt(DEPTH_KEY), CborItem.UInt(depth))
        }
        return map
    }

    companion object {
        const val COMPONENTS_KEY = 1
        const val SOURCE_FINGERPRINT_KEY = 2
        const val DEPTH_KEY = 3

        fun fromCbor(item: CborItem): CryptoKeypath {
            val components = mutableListOf<PathComponent>()
            var sourceFingerprint: ByteArray? = null
            var depth: Int? = null

            val map = item as CborItem.Map
            for ((k, v) in map.entries) {
                when ((k as CborItem.UInt).toInt()) {
                    COMPONENTS_KEY -> components.addAll(PathComponent.fromCbor(v))
                    SOURCE_FINGERPRINT_KEY -> sourceFingerprint =
                        uLongToFingerprint((v as CborItem.UInt).value, 4)
                    DEPTH_KEY -> depth = (v as CborItem.UInt).toInt()
                }
            }

            return CryptoKeypath(components, sourceFingerprint, depth)
        }
    }
}

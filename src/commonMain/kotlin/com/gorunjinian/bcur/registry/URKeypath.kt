package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem
import com.gorunjinian.bcur.registry.pathcomponent.PathComponent

/** UR variant of [CryptoKeypath] (CBOR tag 40304). */
class URKeypath(
    components: List<PathComponent>,
    sourceFingerprint: ByteArray?,
    depth: Int? = null
) : CryptoKeypath(components, sourceFingerprint, depth) {

    override val registryType: RegistryType get() = RegistryType.KEYPATH

    companion object {
        fun fromCbor(item: CborItem): URKeypath {
            var components = listOf<PathComponent>()
            var sourceFingerprint: ByteArray? = null
            var depth: Int? = null

            val map = item as CborItem.Map
            for ((k, v) in map.entries) {
                when ((k as CborItem.UInt).toInt()) {
                    COMPONENTS_KEY -> components = PathComponent.fromCbor(v)
                    SOURCE_FINGERPRINT_KEY -> sourceFingerprint =
                        uLongToFingerprint((v as CborItem.UInt).value, 4)
                    DEPTH_KEY -> depth = (v as CborItem.UInt).toInt()
                }
            }

            return URKeypath(components, sourceFingerprint, depth)
        }
    }
}

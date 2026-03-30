package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/** UR variant of [CryptoSskr] (CBOR tag 40309). */
class URSSKR(split: ByteArray) : CryptoSskr(split) {

    override val registryType: RegistryType get() = RegistryType.SSKR

    companion object {
        fun fromCbor(item: CborItem): URSSKR {
            val bytes = (item as CborItem.Bytes).value
            val normalized = bytes.copyOfRange(1, bytes.size)
            return URSSKR(normalized)
        }
    }
}

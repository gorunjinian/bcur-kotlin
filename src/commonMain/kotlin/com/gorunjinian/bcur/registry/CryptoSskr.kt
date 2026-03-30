package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/**
 * BCR-2020-006: SSKR share (CBOR tag 309).
 *
 * Unlike most registry types, this encodes as a bare byte string, not a map.
 */
open class CryptoSskr(val split: ByteArray) : RegistryItem() {

    override val registryType: RegistryType get() = RegistryType.CRYPTO_SSKR

    override fun toCbor(): CborItem = CborItem.Bytes(split)

    companion object {
        fun fromCbor(item: CborItem): CryptoSskr {
            val bytes = (item as CborItem.Bytes).value
            // Strip the first byte (normalized split)
            val normalized = bytes.copyOfRange(1, bytes.size)
            return CryptoSskr(normalized)
        }
    }
}

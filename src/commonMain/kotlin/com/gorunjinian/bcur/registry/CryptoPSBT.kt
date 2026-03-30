package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/**
 * BCR-2020-006: Partially Signed Bitcoin Transaction (CBOR tag 310).
 *
 * Encodes as a bare byte string, not a map.
 */
open class CryptoPSBT(val psbt: ByteArray) : RegistryItem() {

    override val registryType: RegistryType get() = RegistryType.CRYPTO_PSBT

    override fun toCbor(): CborItem = CborItem.Bytes(psbt)

    companion object {
        fun fromCbor(item: CborItem): CryptoPSBT {
            return CryptoPSBT((item as CborItem.Bytes).value)
        }
    }
}

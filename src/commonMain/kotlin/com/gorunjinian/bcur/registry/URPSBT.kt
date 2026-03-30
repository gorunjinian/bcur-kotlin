package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/** UR variant of [CryptoPSBT] (CBOR tag 40310). */
class URPSBT(psbt: ByteArray) : CryptoPSBT(psbt) {

    override val registryType: RegistryType get() = RegistryType.PSBT

    companion object {
        fun fromCbor(item: CborItem): URPSBT {
            return URPSBT((item as CborItem.Bytes).value)
        }
    }
}

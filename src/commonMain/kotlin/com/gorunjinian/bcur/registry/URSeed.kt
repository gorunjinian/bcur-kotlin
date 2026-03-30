package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/** UR variant of [CryptoSeed] (CBOR tag 40300). */
class URSeed(
    seed: ByteArray,
    birthdate: Long?,
    name: String? = null,
    note: String? = null
) : CryptoSeed(seed, birthdate, name, note) {

    override val registryType: RegistryType get() = RegistryType.SEED

    companion object {
        fun fromCbor(item: CborItem): URSeed {
            var seed: ByteArray? = null
            var birthdate: Long? = null
            var name: String? = null
            var note: String? = null

            val map = item as CborItem.Map
            for ((k, v) in map.entries) {
                when ((k as CborItem.UInt).toInt()) {
                    PAYLOAD_KEY -> seed = (v as CborItem.Bytes).value
                    BIRTHDATE_KEY -> birthdate = (v as CborItem.UInt).toLong()
                    NAME_KEY -> name = (v as CborItem.Text).value
                    NOTE_KEY -> note = (v as CborItem.Text).value
                }
            }

            requireNotNull(seed) { "Seed is null" }
            return URSeed(seed, birthdate, name, note)
        }
    }
}

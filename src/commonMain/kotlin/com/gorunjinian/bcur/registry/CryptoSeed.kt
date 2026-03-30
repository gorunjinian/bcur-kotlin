package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/**
 * BCR-2020-006: Cryptographic seed (CBOR tag 300).
 *
 * Birthdate is stored as days since Unix epoch (1970-01-01), matching
 * the CBOR tag 100 encoding. This avoids java.util.Date for KMP compatibility.
 */
open class CryptoSeed(
    val seed: ByteArray,
    val birthdate: Long?,
    val name: String? = null,
    val note: String? = null
) : RegistryItem() {

    override val registryType: RegistryType get() = RegistryType.CRYPTO_SEED

    override fun toCbor(): CborItem {
        val map = CborItem.Map()
        map.put(CborItem.UInt(PAYLOAD_KEY), CborItem.Bytes(seed))
        if (birthdate != null) {
            val dateItem = CborItem.UInt(birthdate.toULong())
            dateItem.tags.add(BIRTHDATE_TAG.toULong())
            map.put(CborItem.UInt(BIRTHDATE_KEY), dateItem)
        }
        if (name != null) {
            map.put(CborItem.UInt(NAME_KEY), CborItem.Text(name))
        }
        if (note != null) {
            map.put(CborItem.UInt(NOTE_KEY), CborItem.Text(note))
        }
        return map
    }

    companion object {
        const val PAYLOAD_KEY = 1
        const val BIRTHDATE_KEY = 2
        const val NAME_KEY = 3
        const val NOTE_KEY = 4
        const val BIRTHDATE_TAG = 100L

        fun fromCbor(item: CborItem): CryptoSeed {
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
            return CryptoSeed(seed, birthdate, name, note)
        }
    }
}

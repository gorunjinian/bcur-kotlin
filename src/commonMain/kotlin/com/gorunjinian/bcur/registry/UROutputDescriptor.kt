package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/**
 * UR output descriptor (CBOR tag 40308).
 *
 * A string-based output descriptor format with optional key metadata.
 * Unlike [CryptoOutput], this uses a text source string rather than
 * structured script expression tags.
 */
class UROutputDescriptor(
    val source: String,
    val keys: List<RegistryItem>? = null,
    val name: String? = null,
    val note: String? = null
) : RegistryItem() {

    init {
        if (keys != null) {
            require(keys.all { it is URHDKey || it is URECKey || it is URAddress }) {
                "All keys must be one of URHDKey, URECKey or URAddress"
            }
        }
    }

    override val registryType: RegistryType get() = RegistryType.OUTPUT_DESCRIPTOR

    override fun toCbor(): CborItem {
        val map = CborItem.Map()
        map.put(CborItem.UInt(SOURCE_KEY), CborItem.Text(source))

        if (!keys.isNullOrEmpty()) {
            val array = CborItem.Array()
            for (key in keys) {
                val keyItem = key.toCbor()
                when (key) {
                    is URHDKey -> keyItem.tagged(RegistryType.HDKEY.tag!!.toULong())
                    is URECKey -> keyItem.tagged(RegistryType.ECKEY.tag!!.toULong())
                    is URAddress -> keyItem.tagged(RegistryType.ADDRESS.tag!!.toULong())
                    else -> {}
                }
                array.items.add(keyItem)
            }
            map.put(CborItem.UInt(KEYS_KEY), array)
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
        const val SOURCE_KEY = 1
        const val KEYS_KEY = 2
        const val NAME_KEY = 3
        const val NOTE_KEY = 4

        fun fromCbor(item: CborItem): UROutputDescriptor {
            var source: String? = null
            var keys: MutableList<RegistryItem>? = null
            var name: String? = null
            var note: String? = null

            val map = item as CborItem.Map
            for ((k, v) in map.entries) {
                when ((k as CborItem.UInt).toInt()) {
                    SOURCE_KEY -> source = (v as CborItem.Text).value
                    KEYS_KEY -> {
                        val keyArray = v as CborItem.Array
                        keys = mutableListOf()
                        for (keyItem in keyArray.items) {
                            val tag = keyItem.tags.firstOrNull()
                            when (tag) {
                                RegistryType.HDKEY.tag!!.toULong() ->
                                    keys.add(URHDKey.fromCbor(keyItem))
                                RegistryType.ECKEY.tag!!.toULong() ->
                                    keys.add(URECKey.fromCbor(keyItem))
                                RegistryType.ADDRESS.tag!!.toULong() ->
                                    keys.add(URAddress.fromCbor(keyItem))
                                else -> throw IllegalArgumentException(
                                    "All keys must be one of HDKey, ECKey or Address"
                                )
                            }
                        }
                    }
                    NAME_KEY -> name = (v as CborItem.Text).value
                    NOTE_KEY -> note = (v as CborItem.Text).value
                }
            }

            requireNotNull(source) { "Source is null" }
            return UROutputDescriptor(source, keys, name, note)
        }
    }
}

package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/**
 * BCR-2020-006: BIP39 mnemonic words (CBOR tag 301).
 */
class CryptoBip39(
    val words: List<String>,
    val language: String? = null
) : RegistryItem() {

    override val registryType: RegistryType get() = RegistryType.CRYPTO_BIP39

    override fun toCbor(): CborItem {
        val map = CborItem.Map()
        val wordsArray = CborItem.Array()
        for (word in words) {
            wordsArray.items.add(CborItem.Text(word))
        }
        map.put(CborItem.UInt(WORDS_KEY), wordsArray)
        if (language != null) {
            map.put(CborItem.UInt(LANG_KEY), CborItem.Text(language))
        }
        return map
    }

    companion object {
        const val WORDS_KEY = 1
        const val LANG_KEY = 2

        fun fromCbor(item: CborItem): CryptoBip39 {
            val words = mutableListOf<String>()
            var language: String? = null

            val map = item as CborItem.Map
            for ((k, v) in map.entries) {
                when ((k as CborItem.UInt).toInt()) {
                    WORDS_KEY -> {
                        val array = v as CborItem.Array
                        for (wordItem in array.items) {
                            words.add((wordItem as CborItem.Text).value)
                        }
                    }
                    LANG_KEY -> language = (v as CborItem.Text).value
                }
            }

            require(words.isNotEmpty()) { "No BIP39 words" }
            return CryptoBip39(words, language)
        }
    }
}

package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/**
 * BCR-2020-010: Output descriptor (CBOR tag 308).
 *
 * Contains a list of [ScriptExpression] tags wrapping a key (EC, HD, or multi).
 * Uses CBOR tag chaining: script expression tags are stacked outermost-first
 * before the key type tag and the map data.
 */
class CryptoOutput private constructor(
    val scriptExpressions: List<ScriptExpression>,
    val ecKey: CryptoECKey?,
    val hdKey: CryptoHDKey?,
    val multiKey: MultiKey?
) : RegistryItem() {

    constructor(scriptExpressions: List<ScriptExpression>, ecKey: CryptoECKey) :
            this(scriptExpressions, ecKey, null, null)

    constructor(scriptExpressions: List<ScriptExpression>, hdKey: CryptoHDKey) :
            this(scriptExpressions, null, hdKey, null)

    constructor(scriptExpressions: List<ScriptExpression>, multiKey: MultiKey) :
            this(scriptExpressions, null, null, multiKey)

    override val registryType: RegistryType get() = RegistryType.CRYPTO_OUTPUT

    override fun toCbor(): CborItem {
        // Build the inner item with its type tag
        val item: CborItem = when {
            multiKey != null -> multiKey.toCbor()
            ecKey != null -> ecKey.toCbor().apply {
                tagged(RegistryType.CRYPTO_ECKEY.tag!!.toULong())
            }
            hdKey != null -> hdKey.toCbor().apply {
                tagged(RegistryType.CRYPTO_HDKEY.tag!!.toULong())
            }
            else -> throw IllegalStateException("No key set")
        }

        // Prepend script expression tags (outermost first)
        // Current tags: e.g., [303] (from hdKey)
        // After: [wsh, wpkh, 303] = script expressions outermost, key tag innermost
        val existingTags = item.tags.toList()
        item.tags.clear()
        for (expr in scriptExpressions) {
            item.tags.add(expr.tagValue.toULong())
        }
        item.tags.addAll(existingTags)

        return item
    }

    companion object {
        fun fromCbor(cbor: CborItem): CryptoOutput {
            val expressions = mutableListOf<ScriptExpression>()

            // Peel tags: collect script expressions, skip key-type and output tags
            val cryptoOutputTag = RegistryType.CRYPTO_OUTPUT.tag!!.toULong()
            val hdKeyTag = RegistryType.CRYPTO_HDKEY.tag!!.toULong()
            val ecKeyTag = RegistryType.CRYPTO_ECKEY.tag!!.toULong()

            for (tag in cbor.tags) {
                if (tag != hdKeyTag && tag != ecKeyTag && tag != cryptoOutputTag) {
                    expressions.add(ScriptExpression.fromTagValue(tag.toLong()))
                }
            }

            // Check if this is a multi-key by looking at the first (outermost) script expression
            val isMultiKey = expressions.isNotEmpty() &&
                    (expressions.last() == ScriptExpression.MULTISIG ||
                            expressions.last() == ScriptExpression.SORTED_MULTISIG)

            // Expressions were collected outermost-first from tags, which matches
            // the Java's reversed list. The list is already in the correct order
            // (outermost script expression first).

            return if (isMultiKey) {
                CryptoOutput(expressions, MultiKey.fromCbor(cbor))
            } else if (cbor.tags.contains(ecKeyTag)) {
                CryptoOutput(expressions, CryptoECKey.fromCbor(cbor))
            } else if (cbor.tags.contains(hdKeyTag)) {
                CryptoOutput(expressions, CryptoHDKey.fromCbor(cbor))
            } else {
                throw IllegalStateException("Unknown key type in CryptoOutput, tags: ${cbor.tags}")
            }
        }
    }
}

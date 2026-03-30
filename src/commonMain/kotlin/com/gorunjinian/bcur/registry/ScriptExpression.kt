package com.gorunjinian.bcur.registry

/**
 * Script expression tags used by BCR-2020-010 (crypto-output).
 *
 * Each expression wraps an output descriptor key with a script type tag.
 */
enum class ScriptExpression(val tagValue: Long, val expression: String) {
    SCRIPT_HASH(400, "sh"),
    WITNESS_SCRIPT_HASH(401, "wsh"),
    PUBLIC_KEY(402, "pk"),
    PUBLIC_KEY_HASH(403, "pkh"),
    WITNESS_PUBLIC_KEY_HASH(404, "wpkh"),
    COMBO(405, "combo"),
    MULTISIG(406, "multi"),
    SORTED_MULTISIG(407, "sorted"),
    ADDRESS(307, "addr"),
    RAW_SCRIPT(408, "raw"),
    TAPROOT(409, "tr"),
    COSIGNER(410, "cosigner");

    companion object {
        fun fromTagValue(value: Long): ScriptExpression {
            return entries.firstOrNull { it.tagValue == value }
                ?: throw IllegalArgumentException("Unknown script expression tag value: $value")
        }
    }
}

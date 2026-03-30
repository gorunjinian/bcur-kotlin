package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/**
 * Enumeration of all BCR registry types with their UR type strings,
 * CBOR tag numbers, and decoder functions.
 */
enum class RegistryType(
    val type: String,
    val tag: Long?,
    val decoder: ((CborItem) -> RegistryItem)?
) {
    BYTES("bytes", null, null),
    CBOR_PNG("cbor-png", null, null),
    CBOR_SVG("cbor-svg", null, null),
    COSE_SIGN("cose-sign", 98, null),
    COSE_SIGN1("cose-sign1", 18, null),
    COSE_ENCRYPT("cose-encrypt", 96, null),
    COSE_ENCRYPT0("cose-encrypt0", 16, null),
    COSE_MAC("cose-mac", 97, null),
    COSE_MAC0("cose-mac0", 17, null),
    COSE_KEY("cose-key", null, null),
    COSE_KEYSET("cose-keyset", null, null),
    CRYPTO_SEED("crypto-seed", 300, { CryptoSeed.fromCbor(it) }),
    CRYPTO_BIP39("crypto-bip39", 301, { CryptoBip39.fromCbor(it) }),
    CRYPTO_HDKEY("crypto-hdkey", 303, { CryptoHDKey.fromCbor(it) }),
    CRYPTO_KEYPATH("crypto-keypath", 304, { CryptoKeypath.fromCbor(it) }),
    CRYPTO_COIN_INFO("crypto-coininfo", 305, { CryptoCoinInfo.fromCbor(it) }),
    CRYPTO_ECKEY("crypto-eckey", 306, { CryptoECKey.fromCbor(it) }),
    CRYPTO_ADDRESS("crypto-address", 307, { CryptoAddress.fromCbor(it) }),
    CRYPTO_OUTPUT("crypto-output", 308, { CryptoOutput.fromCbor(it) }),
    CRYPTO_SSKR("crypto-sskr", 309, { CryptoSskr.fromCbor(it) }),
    CRYPTO_PSBT("crypto-psbt", 310, { CryptoPSBT.fromCbor(it) }),
    CRYPTO_ACCOUNT("crypto-account", 311, { CryptoAccount.fromCbor(it) }),
    SEED("seed", 40300, { URSeed.fromCbor(it) }),
    HDKEY("hdkey", 40303, { URHDKey.fromCbor(it) }),
    KEYPATH("keypath", 40304, { URKeypath.fromCbor(it) }),
    COIN_INFO("coininfo", 40305, { URCoinInfo.fromCbor(it) }),
    ECKEY("eckey", 40306, { URECKey.fromCbor(it) }),
    ADDRESS("address", 40307, { URAddress.fromCbor(it) }),
    OUTPUT_DESCRIPTOR("output-descriptor", 40308, { UROutputDescriptor.fromCbor(it) }),
    SSKR("sskr", 40309, { URSSKR.fromCbor(it) }),
    PSBT("psbt", 40310, { URPSBT.fromCbor(it) }),
    ACCOUNT_DESCRIPTOR("account-descriptor", 40311, { URAccountDescriptor.fromCbor(it) });

    override fun toString(): String = type

    companion object {
        fun fromString(type: String): RegistryType {
            return entries.firstOrNull { it.type == type.lowercase() }
                ?: throw IllegalArgumentException("Unknown UR registry type: $type")
        }
    }
}

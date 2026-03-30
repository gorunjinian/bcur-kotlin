package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/**
 * BCR-2020-015: Crypto account (CBOR tag 311).
 *
 * Contains a master fingerprint and a list of output descriptors.
 */
class CryptoAccount(
    masterFingerprint: ByteArray,
    val outputDescriptors: List<CryptoOutput>
) : RegistryItem() {

    val masterFingerprint: ByteArray =
        masterFingerprint.copyOfRange(masterFingerprint.size - 4, masterFingerprint.size)

    override val registryType: RegistryType get() = RegistryType.CRYPTO_ACCOUNT

    override fun toCbor(): CborItem {
        val map = CborItem.Map()
        map.put(
            CborItem.UInt(MASTER_FINGERPRINT_KEY),
            CborItem.UInt(fingerprintToULong(masterFingerprint))
        )

        val array = CborItem.Array()
        for (output in outputDescriptors) {
            val outputItem = output.toCbor()
            // Append CRYPTO_OUTPUT tag at the innermost position
            outputItem.tags.add(RegistryType.CRYPTO_OUTPUT.tag!!.toULong())
            array.items.add(outputItem)
        }
        map.put(CborItem.UInt(OUTPUT_DESCRIPTORS_KEY), array)

        return map
    }

    companion object {
        const val MASTER_FINGERPRINT_KEY = 1
        const val OUTPUT_DESCRIPTORS_KEY = 2

        fun fromCbor(cbor: CborItem): CryptoAccount {
            val map = cbor as CborItem.Map

            val fpItem = map.get(MASTER_FINGERPRINT_KEY)
                ?: throw IllegalStateException("Missing master fingerprint")
            val masterFingerprint = uLongToFingerprint((fpItem as CborItem.UInt).value, 4)

            val outputArray = map.get(OUTPUT_DESCRIPTORS_KEY) as CborItem.Array
            val outputs = outputArray.items.map { CryptoOutput.fromCbor(it) }

            return CryptoAccount(masterFingerprint, outputs)
        }
    }
}

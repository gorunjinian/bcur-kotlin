package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/**
 * UR account descriptor (CBOR tag 40311).
 *
 * Contains a master fingerprint and a list of [UROutputDescriptor] items.
 */
class URAccountDescriptor(
    masterFingerprint: ByteArray,
    val outputDescriptors: List<UROutputDescriptor>
) : RegistryItem() {

    val masterFingerprint: ByteArray =
        masterFingerprint.copyOfRange(masterFingerprint.size - 4, masterFingerprint.size)

    override val registryType: RegistryType get() = RegistryType.ACCOUNT_DESCRIPTOR

    override fun toCbor(): CborItem {
        val map = CborItem.Map()
        map.put(
            CborItem.UInt(MASTER_FINGERPRINT_KEY),
            CborItem.UInt(fingerprintToULong(masterFingerprint))
        )

        val array = CborItem.Array()
        for (descriptor in outputDescriptors) {
            val item = descriptor.toCbor()
            // Append OUTPUT_DESCRIPTOR tag at the innermost position
            item.tags.add(RegistryType.OUTPUT_DESCRIPTOR.tag!!.toULong())
            array.items.add(item)
        }
        map.put(CborItem.UInt(OUTPUT_DESCRIPTORS_KEY), array)

        return map
    }

    companion object {
        const val MASTER_FINGERPRINT_KEY = 1
        const val OUTPUT_DESCRIPTORS_KEY = 2

        fun fromCbor(cbor: CborItem): URAccountDescriptor {
            val map = cbor as CborItem.Map

            val fpItem = map.get(MASTER_FINGERPRINT_KEY)
                ?: throw IllegalStateException("Missing master fingerprint")
            val masterFingerprint = uLongToFingerprint((fpItem as CborItem.UInt).value, 4)

            val descriptorArray = map.get(OUTPUT_DESCRIPTORS_KEY) as CborItem.Array
            val descriptors = descriptorArray.items.map { UROutputDescriptor.fromCbor(it) }

            return URAccountDescriptor(masterFingerprint, descriptors)
        }
    }
}

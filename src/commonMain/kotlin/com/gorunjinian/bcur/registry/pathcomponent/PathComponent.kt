package com.gorunjinian.bcur.registry.pathcomponent

import com.gorunjinian.bcur.CborItem

/**
 * Abstract base for BIP32 key path components.
 *
 * Path components are encoded as a flat CBOR array where each component
 * contributes multiple array elements (value + hardened flag).
 */
sealed class PathComponent {

    companion object {
        const val HARDENED_BIT: Int = 0x80000000.toInt()

        /** Encode a list of path components into a CBOR array. */
        fun toCbor(components: List<PathComponent>): CborItem.Array {
            val array = CborItem.Array()
            for (component in components) {
                when (component) {
                    is WildcardPathComponent -> {
                        array.items.add(CborItem.Array())  // empty array = wildcard marker
                        array.items.add(CborItem.Bool(component.hardened))
                    }
                    is RangePathComponent -> {
                        val rangeArray = CborItem.Array()
                        rangeArray.items.add(CborItem.UInt(component.start))
                        rangeArray.items.add(CborItem.UInt(component.end))
                        array.items.add(rangeArray)
                        array.items.add(CborItem.Bool(component.hardened))
                    }
                    is PairPathComponent -> {
                        val pairArray = CborItem.Array()
                        pairArray.items.add(CborItem.UInt(component.external.index))
                        pairArray.items.add(CborItem.Bool(component.external.hardened))
                        pairArray.items.add(CborItem.UInt(component.internal.index))
                        pairArray.items.add(CborItem.Bool(component.internal.hardened))
                        array.items.add(pairArray)
                    }
                    is IndexPathComponent -> {
                        array.items.add(CborItem.UInt(component.index))
                        array.items.add(CborItem.Bool(component.hardened))
                    }
                }
            }
            return array
        }

        /** Decode a list of path components from a CBOR array. */
        fun fromCbor(item: CborItem): List<PathComponent> {
            val components = mutableListOf<PathComponent>()
            val array = item as CborItem.Array
            var i = 0
            while (i < array.items.size) {
                val component = array.items[i]
                if (component is CborItem.Array) {
                    val sub = component.items
                    when {
                        sub.isEmpty() -> {
                            // Wildcard
                            i++
                            val hardened = (array.items[i] as CborItem.Bool).value
                            components.add(WildcardPathComponent(hardened))
                        }
                        sub.size == 2 -> {
                            // Range
                            i++
                            val hardened = (array.items[i] as CborItem.Bool).value
                            val start = (sub[0] as CborItem.UInt).toInt()
                            val end = (sub[1] as CborItem.UInt).toInt()
                            components.add(RangePathComponent(start, end, hardened))
                        }
                        sub.size == 4 -> {
                            // Pair
                            val extIndex = (sub[0] as CborItem.UInt).toInt()
                            val extHardened = (sub[1] as CborItem.Bool).value
                            val intIndex = (sub[2] as CborItem.UInt).toInt()
                            val intHardened = (sub[3] as CborItem.Bool).value
                            components.add(
                                PairPathComponent(
                                    IndexPathComponent(extIndex, extHardened),
                                    IndexPathComponent(intIndex, intHardened)
                                )
                            )
                        }
                    }
                } else if (component is CborItem.UInt) {
                    // Index
                    val index = component.toInt()
                    i++
                    val hardened = (array.items[i] as CborItem.Bool).value
                    components.add(IndexPathComponent(index, hardened))
                }
                i++
            }
            return components
        }
    }
}

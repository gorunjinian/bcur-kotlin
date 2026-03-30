package com.gorunjinian.bcur

/**
 * Full CBOR data model for structured encoding/decoding.
 *
 * Supports all major types needed by the BCR registry:
 * - Major type 0: Unsigned integers
 * - Major type 1: Negative integers
 * - Major type 2: Byte strings
 * - Major type 3: Text strings (UTF-8)
 * - Major type 4: Arrays
 * - Major type 5: Maps (ordered key-value pairs)
 * - Major type 6: Semantic tags (stored as a list on each item)
 * - Major type 7: Simple values (booleans, null)
 *
 * Independent of [Cbor] (the minimal transport-layer encoder).
 */
sealed class CborItem {

    /** Semantic tags applied to this item, outermost first. */
    val tags: MutableList<ULong> = mutableListOf()

    /** Fluent helper: adds [tag] at the outermost position and returns this item. */
    fun tagged(tag: ULong): CborItem {
        tags.add(0, tag)
        return this
    }

    // --- Concrete types ---

    /** Major type 0: unsigned integer. */
    class UInt(val value: ULong) : CborItem() {
        constructor(value: Int) : this(value.toULong())

        fun toInt(): Int = value.toInt()
        fun toLong(): Long = value.toLong()

        override fun equals(other: Any?): Boolean =
            other is UInt && value == other.value && tags == other.tags

        override fun hashCode(): Int = value.hashCode() * 31 + tags.hashCode()
        override fun toString(): String = "UInt($value)"
    }

    /** Major type 1: negative integer (encodes -1 - value). */
    class NInt(val value: ULong) : CborItem() {
        fun toLong(): Long = -1L - value.toLong()

        override fun equals(other: Any?): Boolean =
            other is NInt && value == other.value && tags == other.tags

        override fun hashCode(): Int = value.hashCode() * 31 + tags.hashCode()
        override fun toString(): String = "NInt(-${value + 1UL})"
    }

    /** Major type 2: byte string. */
    class Bytes(val value: ByteArray) : CborItem() {
        override fun equals(other: Any?): Boolean =
            other is Bytes && value.contentEquals(other.value) && tags == other.tags

        override fun hashCode(): Int = value.contentHashCode() * 31 + tags.hashCode()
        override fun toString(): String = "Bytes(${value.size} bytes)"
    }

    /** Major type 3: text string (UTF-8). */
    class Text(val value: String) : CborItem() {
        override fun equals(other: Any?): Boolean =
            other is Text && value == other.value && tags == other.tags

        override fun hashCode(): Int = value.hashCode() * 31 + tags.hashCode()
        override fun toString(): String = "Text(\"$value\")"
    }

    /** Major type 4: array of items. */
    class Array(val items: MutableList<CborItem> = mutableListOf()) : CborItem() {
        constructor(vararg elements: CborItem) : this(elements.toMutableList())

        override fun equals(other: Any?): Boolean =
            other is Array && items == other.items && tags == other.tags

        override fun hashCode(): Int = items.hashCode() * 31 + tags.hashCode()
        override fun toString(): String = "Array(${items.size} items)"
    }

    /** Major type 5: ordered map of key-value pairs. */
    class Map(val entries: MutableList<Pair<CborItem, CborItem>> = mutableListOf()) : CborItem() {

        /** Put a key-value pair, appending to the end. */
        fun put(key: CborItem, value: CborItem) {
            entries.add(key to value)
        }

        /** Look up a value by integer key (linear scan). */
        fun get(key: Int): CborItem? {
            val target = key.toULong()
            for ((k, v) in entries) {
                if (k is UInt && k.value == target) return v
            }
            return null
        }

        /** Look up a value by ULong key. */
        fun get(key: ULong): CborItem? {
            for ((k, v) in entries) {
                if (k is UInt && k.value == key) return v
            }
            return null
        }

        override fun equals(other: Any?): Boolean =
            other is Map && entries == other.entries && tags == other.tags

        override fun hashCode(): Int = entries.hashCode() * 31 + tags.hashCode()
        override fun toString(): String = "Map(${entries.size} entries)"
    }

    /** Major type 7: boolean (simple value 20=false, 21=true). */
    class Bool(val value: Boolean) : CborItem() {
        override fun equals(other: Any?): Boolean =
            other is Bool && value == other.value && tags == other.tags

        override fun hashCode(): Int = value.hashCode() * 31 + tags.hashCode()
        override fun toString(): String = "Bool($value)"
    }

    /** Major type 7: null (simple value 22). */
    object Null : CborItem() {
        override fun toString(): String = "Null"
    }

    // ==================== Encoder ====================

    /** Encode this item to CBOR bytes. */
    fun encode(): ByteArray {
        val out = mutableListOf<Byte>()
        encodeTo(out)
        return out.toByteArray()
    }

    private fun encodeTo(out: MutableList<Byte>) {
        // Write tags outermost first
        for (tag in tags) {
            encodeHead(out, MAJOR_TAG, tag)
        }
        // Write the item
        when (this) {
            is UInt -> encodeHead(out, MAJOR_UINT, value)
            is NInt -> encodeHead(out, MAJOR_NINT, value)
            is Bytes -> {
                encodeHead(out, MAJOR_BYTES, value.size.toULong())
                for (b in value) out.add(b)
            }
            is Text -> {
                val utf8 = value.encodeToByteArray()
                encodeHead(out, MAJOR_TEXT, utf8.size.toULong())
                for (b in utf8) out.add(b)
            }
            is Array -> {
                encodeHead(out, MAJOR_ARRAY, items.size.toULong())
                for (item in items) item.encodeTo(out)
            }
            is Map -> {
                encodeHead(out, MAJOR_MAP, entries.size.toULong())
                for ((k, v) in entries) {
                    k.encodeTo(out)
                    v.encodeTo(out)
                }
            }
            is Bool -> out.add(if (value) 0xF5.toByte() else 0xF4.toByte())
            is Null -> out.add(0xF6.toByte())
        }
    }

    // ==================== Decoder ====================

    companion object {
        // Major type constants (shifted to top 3 bits position)
        private const val MAJOR_UINT: Int = 0    // 000
        private const val MAJOR_NINT: Int = 1    // 001
        private const val MAJOR_BYTES: Int = 2   // 010
        private const val MAJOR_TEXT: Int = 3     // 011
        private const val MAJOR_ARRAY: Int = 4   // 100
        private const val MAJOR_MAP: Int = 5     // 101
        private const val MAJOR_TAG: Int = 6     // 110
        private const val MAJOR_SIMPLE: Int = 7  // 111

        /** Decode a single CBOR item from bytes. */
        fun decode(data: ByteArray): CborItem {
            val (item, _) = decodeItem(data, 0)
            return item
        }

        /** Decode a CBOR item starting at [offset], returning (item, nextOffset). */
        fun decodeItem(data: ByteArray, offset: Int): Pair<CborItem, Int> {
            // Collect tags
            val collectedTags = mutableListOf<ULong>()
            var pos = offset

            while (pos < data.size) {
                val initial = data[pos].toInt() and 0xFF
                val majorType = initial ushr 5
                if (majorType != MAJOR_TAG) break
                val (tagValue, nextPos) = decodeArgument(data, pos)
                collectedTags.add(tagValue)
                pos = nextPos
            }

            val (item, nextPos) = decodeNonTagItem(data, pos)
            item.tags.addAll(collectedTags)
            return item to nextPos
        }

        private fun decodeNonTagItem(data: ByteArray, offset: Int): Pair<CborItem, Int> {
            val initial = data[offset].toInt() and 0xFF
            return when (val majorType = initial ushr 5) {
                MAJOR_UINT -> {
                    val (value, next) = decodeArgument(data, offset)
                    UInt(value) to next
                }
                MAJOR_NINT -> {
                    val (value, next) = decodeArgument(data, offset)
                    NInt(value) to next
                }
                MAJOR_BYTES -> {
                    val (length, dataStart) = decodeArgument(data, offset)
                    val len = length.toInt()
                    val bytes = data.copyOfRange(dataStart, dataStart + len)
                    Bytes(bytes) to (dataStart + len)
                }
                MAJOR_TEXT -> {
                    val (length, dataStart) = decodeArgument(data, offset)
                    val len = length.toInt()
                    val text = data.copyOfRange(dataStart, dataStart + len).decodeToString()
                    Text(text) to (dataStart + len)
                }
                MAJOR_ARRAY -> {
                    val (count, next) = decodeArgument(data, offset)
                    val items = mutableListOf<CborItem>()
                    var pos = next
                    for (i in 0 until count.toInt()) {
                        val (item, nextPos) = decodeItem(data, pos)
                        items.add(item)
                        pos = nextPos
                    }
                    Array(items) to pos
                }
                MAJOR_MAP -> {
                    val (count, next) = decodeArgument(data, offset)
                    val entries = mutableListOf<Pair<CborItem, CborItem>>()
                    var pos = next
                    for (i in 0 until count.toInt()) {
                        val (key, keyNext) = decodeItem(data, pos)
                        val (value, valueNext) = decodeItem(data, keyNext)
                        entries.add(key to value)
                        pos = valueNext
                    }
                    Map(entries) to pos
                }
                MAJOR_SIMPLE -> {
                    when (val additionalInfo = initial and 0x1F) {
                        20 -> Bool(false) to (offset + 1)
                        21 -> Bool(true) to (offset + 1)
                        22 -> Null to (offset + 1)
                        else -> throw IllegalArgumentException(
                            "Unsupported simple value: $additionalInfo at offset $offset"
                        )
                    }
                }
                else -> throw IllegalArgumentException(
                    "Unexpected major type $majorType at offset $offset"
                )
            }
        }

        /**
         * Decode the argument (value/length/count) from a CBOR head byte.
         * Returns (argument value, offset after the head).
         */
        private fun decodeArgument(data: ByteArray, offset: Int): Pair<ULong, Int> {
            val initial = data[offset].toInt() and 0xFF
            val additionalInfo = initial and 0x1F

            return when {
                additionalInfo <= 23 -> additionalInfo.toULong() to (offset + 1)
                additionalInfo == 24 -> {
                    (data[offset + 1].toInt() and 0xFF).toULong() to (offset + 2)
                }
                additionalInfo == 25 -> {
                    val v = ((data[offset + 1].toInt() and 0xFF) shl 8) or
                            (data[offset + 2].toInt() and 0xFF)
                    v.toULong() to (offset + 3)
                }
                additionalInfo == 26 -> {
                    val v = ((data[offset + 1].toLong() and 0xFF) shl 24) or
                            ((data[offset + 2].toLong() and 0xFF) shl 16) or
                            ((data[offset + 3].toLong() and 0xFF) shl 8) or
                            (data[offset + 4].toLong() and 0xFF)
                    v.toULong() to (offset + 5)
                }
                additionalInfo == 27 -> {
                    val v = ((data[offset + 1].toLong() and 0xFF) shl 56) or
                            ((data[offset + 2].toLong() and 0xFF) shl 48) or
                            ((data[offset + 3].toLong() and 0xFF) shl 40) or
                            ((data[offset + 4].toLong() and 0xFF) shl 32) or
                            ((data[offset + 5].toLong() and 0xFF) shl 24) or
                            ((data[offset + 6].toLong() and 0xFF) shl 16) or
                            ((data[offset + 7].toLong() and 0xFF) shl 8) or
                            (data[offset + 8].toLong() and 0xFF)
                    v.toULong() to (offset + 9)
                }
                else -> throw IllegalArgumentException(
                    "Unsupported additional info: $additionalInfo at offset $offset"
                )
            }
        }

        /** Encode a CBOR head (major type + argument) into the output list. */
        private fun encodeHead(out: MutableList<Byte>, majorType: Int, argument: ULong) {
            val mt = (majorType shl 5)
            when {
                argument <= 23UL -> {
                    out.add((mt or argument.toInt()).toByte())
                }
                argument <= 0xFFUL -> {
                    out.add((mt or 24).toByte())
                    out.add(argument.toByte())
                }
                argument <= 0xFFFFUL -> {
                    out.add((mt or 25).toByte())
                    out.add((argument shr 8).toByte())
                    out.add(argument.toByte())
                }
                argument <= 0xFFFFFFFFUL -> {
                    out.add((mt or 26).toByte())
                    out.add((argument shr 24).toByte())
                    out.add((argument shr 16).toByte())
                    out.add((argument shr 8).toByte())
                    out.add(argument.toByte())
                }
                else -> {
                    out.add((mt or 27).toByte())
                    out.add((argument shr 56).toByte())
                    out.add((argument shr 48).toByte())
                    out.add((argument shr 40).toByte())
                    out.add((argument shr 32).toByte())
                    out.add((argument shr 24).toByte())
                    out.add((argument shr 16).toByte())
                    out.add((argument shr 8).toByte())
                    out.add(argument.toByte())
                }
            }
        }
    }
}

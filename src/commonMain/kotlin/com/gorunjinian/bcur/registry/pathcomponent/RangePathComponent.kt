package com.gorunjinian.bcur.registry.pathcomponent

/** A range path component (e.g., "[0-99]"). */
data class RangePathComponent(val start: Int, val end: Int, val hardened: Boolean) : PathComponent() {
    init {
        require((start and HARDENED_BIT) == 0 && (end and HARDENED_BIT) == 0) {
            "Invalid range [$start, $end] - most significant bit cannot be set"
        }
        require(start < end) {
            "Invalid range [$start, $end] - start must be lower than end"
        }
    }

    override fun toString(): String {
        val h = if (hardened) "'" else ""
        return "[$start$h-$end$h]"
    }
}

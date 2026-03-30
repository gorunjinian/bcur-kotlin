package com.gorunjinian.bcur.registry.pathcomponent

/** A single index path component (e.g., "0", "1'"). */
data class IndexPathComponent(val index: Int, val hardened: Boolean) : PathComponent() {
    init {
        require((index and HARDENED_BIT) == 0) {
            "Invalid index $index - most significant bit cannot be set"
        }
    }

    override fun toString(): String = "$index${if (hardened) "'" else ""}"
}

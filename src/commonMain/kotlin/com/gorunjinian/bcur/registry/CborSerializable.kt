package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/** Interface for types that can be serialized to CBOR. */
interface CborSerializable {
    fun toCbor(): CborItem
}

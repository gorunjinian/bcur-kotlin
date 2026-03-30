package com.gorunjinian.bcur.registry.pathcomponent

/** A paired internal/external path component (e.g., "<0;1>"). */
data class PairPathComponent(
    val external: IndexPathComponent,
    val internal: IndexPathComponent
) : PathComponent() {
    override fun toString(): String = "<$external;$internal>"
}

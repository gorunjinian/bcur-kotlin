package com.gorunjinian.bcur.registry.pathcomponent

/** A wildcard path component (e.g., "*"). */
data class WildcardPathComponent(val hardened: Boolean) : PathComponent() {
    override fun toString(): String = "*"
}

package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem

/**
 * BCR-2020-007: Coin type and network information (CBOR tag 305).
 */
open class CryptoCoinInfo(
    private val type: Int?,
    private val network: Int?
) : RegistryItem() {

    init {
        if (network == Network.GOERLI.value && type != Type.ETHEREUM.value) {
            throw IllegalArgumentException("Goerli network can only be selected for Ethereum")
        }
    }

    constructor(type: Type?, network: Network?) : this(type?.value, network?.value)

    val resolvedType: Type get() = type?.let { Type.fromValue(it) } ?: Type.BITCOIN
    val resolvedNetwork: Network get() = network?.let { Network.fromValue(it) } ?: Network.MAINNET

    override val registryType: RegistryType get() = RegistryType.CRYPTO_COIN_INFO

    override fun toCbor(): CborItem {
        val map = CborItem.Map()
        if (type != null) {
            map.put(CborItem.UInt(TYPE_KEY), CborItem.UInt(type))
        }
        if (network != null) {
            map.put(CborItem.UInt(NETWORK_KEY), CborItem.UInt(network))
        }
        return map
    }

    enum class Type(val value: Int) {
        BITCOIN(0), ETHEREUM(60);

        companion object {
            fun fromValue(value: Int): Type =
                entries.firstOrNull { it.value == value }
                    ?: throw IllegalArgumentException("Unknown coin type: $value")
        }
    }

    enum class Network(val value: Int) {
        MAINNET(0), TESTNET(1), GOERLI(4);

        companion object {
            fun fromValue(value: Int): Network =
                entries.firstOrNull { it.value == value }
                    ?: throw IllegalArgumentException("Unknown network: $value")
        }
    }

    companion object {
        const val TYPE_KEY = 1
        const val NETWORK_KEY = 2

        fun fromCbor(item: CborItem): CryptoCoinInfo {
            var type: Int? = null
            var network: Int? = null

            val map = item as CborItem.Map
            for ((k, v) in map.entries) {
                when ((k as CborItem.UInt).toInt()) {
                    TYPE_KEY -> type = (v as CborItem.UInt).toInt()
                    NETWORK_KEY -> network = (v as CborItem.UInt).toInt()
                }
            }

            return CryptoCoinInfo(type, network)
        }
    }
}

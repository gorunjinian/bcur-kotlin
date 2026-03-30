package com.gorunjinian.bcur.registry

import com.gorunjinian.bcur.CborItem
import com.gorunjinian.bcur.registry.pathcomponent.IndexPathComponent
import com.gorunjinian.bcur.registry.pathcomponent.WildcardPathComponent
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegistryRoundTripTest {

    // --- CryptoCoinInfo ---

    @Test
    fun testCryptoCoinInfoRoundTrip() {
        val original = CryptoCoinInfo(CryptoCoinInfo.Type.BITCOIN, CryptoCoinInfo.Network.MAINNET)
        val cbor = original.toCbor()
        val encoded = cbor.encode()
        val decoded = CborItem.decode(encoded)
        val restored = CryptoCoinInfo.fromCbor(decoded)

        assertEquals(CryptoCoinInfo.Type.BITCOIN, restored.resolvedType)
        assertEquals(CryptoCoinInfo.Network.MAINNET, restored.resolvedNetwork)
    }

    @Test
    fun testCryptoCoinInfoTestnet() {
        val original = CryptoCoinInfo(CryptoCoinInfo.Type.BITCOIN, CryptoCoinInfo.Network.TESTNET)
        val encoded = original.toCbor().encode()
        val restored = CryptoCoinInfo.fromCbor(CborItem.decode(encoded))

        assertEquals(CryptoCoinInfo.Type.BITCOIN, restored.resolvedType)
        assertEquals(CryptoCoinInfo.Network.TESTNET, restored.resolvedNetwork)
    }

    // --- CryptoECKey ---

    @Test
    fun testCryptoECKeyRoundTrip() {
        val keyData = ByteArray(33) { it.toByte() }
        val original = CryptoECKey(0, false, keyData)
        val encoded = original.toCbor().encode()
        val restored = CryptoECKey.fromCbor(CborItem.decode(encoded))

        assertEquals(0, restored.resolvedCurve)
        assertEquals(false, restored.isPrivateKey)
        assertContentEquals(keyData, restored.data)
    }

    // --- CryptoKeypath ---

    @Test
    fun testCryptoKeypathRoundTrip() {
        val components = listOf(
            IndexPathComponent(44, true),
            IndexPathComponent(0, true),
            IndexPathComponent(0, true)
        )
        val fingerprint = byteArrayOf(0x12, 0x34, 0x56, 0x78)
        val original = CryptoKeypath(components, fingerprint, 3)

        val encoded = original.toCbor().encode()
        val restored = CryptoKeypath.fromCbor(CborItem.decode(encoded))

        assertEquals("44'/0'/0'", restored.path)
        assertContentEquals(fingerprint, restored.sourceFingerprint)
        assertEquals(3, restored.depth)
    }

    // --- CryptoHDKey (master) ---

    @Test
    fun testCryptoHDKeyMasterRoundTrip() {
        val key = ByteArray(32) { (it + 1).toByte() }
        val chainCode = ByteArray(32) { (it + 33).toByte() }
        val original = CryptoHDKey(key, chainCode)

        val encoded = original.toCbor().encode()
        val restored = CryptoHDKey.fromCbor(CborItem.decode(encoded))

        assertTrue(restored.isMaster)
        assertContentEquals(key, restored.key)
        assertContentEquals(chainCode, restored.chainCode)
    }

    // --- CryptoHDKey (derived) ---

    @Test
    fun testCryptoHDKeyDerivedRoundTrip() {
        val key = ByteArray(33) { it.toByte() }
        val chainCode = ByteArray(32) { (it + 1).toByte() }
        val useInfo = CryptoCoinInfo(CryptoCoinInfo.Type.BITCOIN, CryptoCoinInfo.Network.MAINNET)
        val origin = CryptoKeypath(
            listOf(IndexPathComponent(84, true), IndexPathComponent(0, true), IndexPathComponent(0, true)),
            byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte()),
            3
        )
        val children = CryptoKeypath(
            listOf(IndexPathComponent(0, false), WildcardPathComponent(false)),
            null
        )
        val parentFp = byteArrayOf(0x11, 0x22, 0x33, 0x44)

        val original = CryptoHDKey(false, key, chainCode, useInfo, origin, children, parentFp, "test", "a note")

        val encoded = original.toCbor().encode()
        val restored = CryptoHDKey.fromCbor(CborItem.decode(encoded))

        assertEquals(false, restored.isMaster)
        assertEquals(false, restored.isPrivateKey)
        assertContentEquals(key, restored.key)
        assertContentEquals(chainCode, restored.chainCode)
        assertEquals("84'/0'/0'", restored.origin?.path)
        assertEquals("0/*", restored.children?.path)
        assertContentEquals(parentFp, restored.parentFingerprint)
        assertEquals("test", restored.name)
        assertEquals("a note", restored.note)
    }

    // --- MultiKey ---

    @Test
    fun testMultiKeyRoundTrip() {
        val key1 = CryptoHDKey(false, ByteArray(33) { 1 }, ByteArray(32) { 2 }, null, null, null, null)
        val key2 = CryptoHDKey(false, ByteArray(33) { 3 }, ByteArray(32) { 4 }, null, null, null, null)
        val original = MultiKey(2, emptyList(), listOf(key1, key2))

        val encoded = original.toCbor().encode()
        val restored = MultiKey.fromCbor(CborItem.decode(encoded))

        assertEquals(2, restored.threshold)
        assertEquals(2, restored.hdKeys.size)
    }

    // --- CryptoOutput (single key) ---

    @Test
    fun testCryptoOutputSingleKeyRoundTrip() {
        val key = CryptoHDKey(false, ByteArray(33) { it.toByte() }, ByteArray(32) { 1 }, null, null, null, null)
        val original = CryptoOutput(
            listOf(ScriptExpression.WITNESS_PUBLIC_KEY_HASH),
            key
        )

        val encoded = original.toCbor().encode()
        val restored = CryptoOutput.fromCbor(CborItem.decode(encoded))

        assertEquals(1, restored.scriptExpressions.size)
        assertEquals(ScriptExpression.WITNESS_PUBLIC_KEY_HASH, restored.scriptExpressions[0])
        assertTrue(restored.hdKey != null)
    }

    // --- CryptoOutput (multi-sig) ---

    @Test
    fun testCryptoOutputMultiSigRoundTrip() {
        val key1 = CryptoHDKey(false, ByteArray(33) { 1 }, ByteArray(32) { 2 }, null, null, null, null)
        val key2 = CryptoHDKey(false, ByteArray(33) { 3 }, ByteArray(32) { 4 }, null, null, null, null)
        val multiKey = MultiKey(2, emptyList(), listOf(key1, key2))
        val original = CryptoOutput(
            listOf(ScriptExpression.WITNESS_SCRIPT_HASH, ScriptExpression.SORTED_MULTISIG),
            multiKey
        )

        val encoded = original.toCbor().encode()
        val restored = CryptoOutput.fromCbor(CborItem.decode(encoded))

        assertEquals(2, restored.scriptExpressions.size)
        assertEquals(ScriptExpression.WITNESS_SCRIPT_HASH, restored.scriptExpressions[0])
        assertEquals(ScriptExpression.SORTED_MULTISIG, restored.scriptExpressions[1])
        assertTrue(restored.multiKey != null)
        assertEquals(2, restored.multiKey!!.threshold)
    }

    // --- CryptoAccount ---

    @Test
    fun testCryptoAccountRoundTrip() {
        val key = CryptoHDKey(false, ByteArray(33) { it.toByte() }, ByteArray(32) { 1 }, null, null, null, null)
        val output = CryptoOutput(listOf(ScriptExpression.WITNESS_PUBLIC_KEY_HASH), key)
        val fingerprint = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        val original = CryptoAccount(fingerprint, listOf(output))

        val encoded = original.toCbor().encode()
        val restored = CryptoAccount.fromCbor(CborItem.decode(encoded))

        assertContentEquals(fingerprint, restored.masterFingerprint)
        assertEquals(1, restored.outputDescriptors.size)
    }

    // --- CryptoSeed ---

    @Test
    fun testCryptoSeedRoundTrip() {
        val seedBytes = ByteArray(16) { (it + 10).toByte() }
        val original = CryptoSeed(seedBytes, 18000L, "myseed", "test note")

        val encoded = original.toCbor().encode()
        val restored = CryptoSeed.fromCbor(CborItem.decode(encoded))

        assertContentEquals(seedBytes, restored.seed)
        assertEquals(18000L, restored.birthdate)
        assertEquals("myseed", restored.name)
        assertEquals("test note", restored.note)
    }

    // --- CryptoBip39 ---

    @Test
    fun testCryptoBip39RoundTrip() {
        val words = listOf("abandon", "abandon", "abandon", "about")
        val original = CryptoBip39(words, "en")

        val encoded = original.toCbor().encode()
        val restored = CryptoBip39.fromCbor(CborItem.decode(encoded))

        assertEquals(words, restored.words)
        assertEquals("en", restored.language)
    }

    // --- CryptoPSBT ---

    @Test
    fun testCryptoPSBTRoundTrip() {
        val psbtData = ByteArray(100) { it.toByte() }
        val original = CryptoPSBT(psbtData)

        val encoded = original.toCbor().encode()
        val restored = CryptoPSBT.fromCbor(CborItem.decode(encoded))

        assertContentEquals(psbtData, restored.psbt)
    }

    // --- CryptoAddress ---

    @Test
    fun testCryptoAddressRoundTrip() {
        val info = CryptoCoinInfo(CryptoCoinInfo.Type.BITCOIN, CryptoCoinInfo.Network.MAINNET)
        val addressData = ByteArray(20) { it.toByte() }
        val original = CryptoAddress(info, CryptoAddress.Type.P2WPKH, addressData)

        val encoded = original.toCbor().encode()
        val restored = CryptoAddress.fromCbor(CborItem.decode(encoded))

        assertEquals(CryptoAddress.Type.P2WPKH, restored.type)
        assertContentEquals(addressData, restored.data)
    }

    // --- UROutputDescriptor ---

    @Test
    fun testUROutputDescriptorRoundTrip() {
        val original = UROutputDescriptor(
            source = "wpkh([deadbeef/84'/0'/0']xpub.../0/*)",
            name = "My Wallet",
            note = "test"
        )

        val encoded = original.toCbor().encode()
        val restored = UROutputDescriptor.fromCbor(CborItem.decode(encoded))

        assertEquals("wpkh([deadbeef/84'/0'/0']xpub.../0/*)", restored.source)
        assertEquals("My Wallet", restored.name)
        assertEquals("test", restored.note)
    }

    // --- RegistryType lookup ---

    @Test
    fun testRegistryTypeFromString() {
        assertEquals(RegistryType.CRYPTO_OUTPUT, RegistryType.fromString("crypto-output"))
        assertEquals(RegistryType.CRYPTO_HDKEY, RegistryType.fromString("crypto-hdkey"))
        assertEquals(RegistryType.OUTPUT_DESCRIPTOR, RegistryType.fromString("output-descriptor"))
    }

    // --- ScriptExpression lookup ---

    @Test
    fun testScriptExpressionFromTagValue() {
        assertEquals(ScriptExpression.WITNESS_PUBLIC_KEY_HASH, ScriptExpression.fromTagValue(404))
        assertEquals(ScriptExpression.WITNESS_SCRIPT_HASH, ScriptExpression.fromTagValue(401))
        assertEquals(ScriptExpression.TAPROOT, ScriptExpression.fromTagValue(409))
    }
}

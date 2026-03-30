# BC-UR (Uniform Resources) Documentation

Complete reference for `bcur-kotlin` - a pure Kotlin Multiplatform implementation of BC-UR with full encoder/decoder support and BCR registry types.

**Package:** `com.gorunjinian.bcur`

---

## Table of Contents

1. [Overview](#overview)
2. [Transport Layer API](#transport-layer-api)
   - [UR](#ur)
   - [UREncoder](#urencoder)
   - [URDecoder](#urdecoder)
   - [Bytewords](#bytewords)
   - [Cbor (Transport)](#cbor-transport)
   - [FountainEncoder](#fountainencoder)
   - [FountainDecoder](#fountaindecoder)
   - [CRC32](#crc32)
   - [SHA256](#sha256)
3. [CBOR Data Model](#cbor-data-model)
   - [CborItem](#cboritem)
   - [Encoding and Decoding](#encoding-and-decoding)
   - [Semantic Tags](#semantic-tags)
4. [Registry Module](#registry-module)
   - [Architecture](#architecture)
   - [RegistryType](#registrytype)
   - [RegistryItem](#registryitem)
5. [Registry Types Reference](#registry-types-reference)
   - [CryptoHDKey (tag 303)](#cryptohdkey)
   - [CryptoKeypath (tag 304)](#cryptokeypath)
   - [CryptoCoinInfo (tag 305)](#cryptocoininfo)
   - [CryptoECKey (tag 306)](#cryptoeckey)
   - [CryptoAddress (tag 307)](#cryptoaddress)
   - [CryptoOutput (tag 308)](#cryptooutput)
   - [CryptoAccount (tag 311)](#cryptoaccount)
   - [CryptoSeed (tag 300)](#cryptoseed)
   - [CryptoBip39 (tag 301)](#cryptobip39)
   - [CryptoSskr (tag 309)](#cryptosskr)
   - [CryptoPSBT (tag 310)](#cryptopsbt)
   - [MultiKey](#multikey)
   - [ScriptExpression](#scriptexpression)
   - [Path Components](#path-components)
6. [UR Variant Types (40xxx Tags)](#ur-variant-types)
7. [PSBT Encoding Guide](#psbt-encoding-guide)
8. [PSBT Decoding Guide](#psbt-decoding-guide)
9. [Animated QR Codes (Multi-Part URs)](#animated-qr-codes-multi-part-urs)
10. [Descriptor Encoding Guide](#descriptor-encoding-guide)
11. [Complete Examples](#complete-examples)
12. [Specifications](#specifications)

---

## Overview

### What is BC-UR?

BC-UR (Blockchain Commons Uniform Resources) is a standard for encoding typed binary data into QR code-friendly strings. It supports both single QR codes and animated (multi-part) QR codes using fountain codes.

A UR string looks like:
```
ur:crypto-psbt/hdosjojkidjyzmadaenyaoaeaeaeao...
```

### What are Fountain Codes?

When data is too large for a single QR code, BC-UR uses rateless fountain codes to split it into multiple parts displayed as an animation. Properties:

- **Rateless** - Unlimited parts can be generated
- **Order-independent** - Any sufficient subset reconstructs the message
- **Redundant** - Extra parts provide error recovery for missed frames
- **Deterministic** - Same sequence number always produces the same part

Multi-part format:
```
ur:crypto-psbt/1-10/lpadaxcsencylobemohsgmoyadhd...
```

### Library Architecture

`bcur-kotlin` has two layers:

| Layer | Package | Purpose |
|-------|---------|---------|
| **Transport** | `com.gorunjinian.bcur` | UR encoding/decoding, fountain codes, bytewords, minimal CBOR for UR framing |
| **Registry** | `com.gorunjinian.bcur.registry` | BCR registry types (output descriptors, HD keys, accounts, etc.) with full CBOR schema support |

### Library Characteristics

- Pure Kotlin, zero external dependencies
- Kotlin Multiplatform: Android, JVM, iOS, macOS, watchOS
- Full encode AND decode (unlike encoder-only alternatives)
- Cross-compatible with Hummingbird (Java) and URKit (Swift)
- Complete BCR registry: BCR-2020-006, BCR-2020-007, BCR-2020-010, BCR-2020-015
- Includes pure Kotlin CRC32, SHA256, and CBOR implementations

---

## Transport Layer API

### UR

Core data class representing a Uniform Resource.

```kotlin
import com.gorunjinian.bcur.UR
```

#### Constructor

```kotlin
data class UR(
    val type: String,       // UR type (lowercase letters, digits, hyphens)
    val cborData: ByteArray // CBOR-encoded payload
)
```

| Parameter  | Type        | Description |
|------------|-------------|-------------|
| `type`     | `String`    | UR type identifier (e.g., `"crypto-psbt"`). Must be non-empty, lowercase letters/digits/hyphens only. |
| `cborData` | `ByteArray` | Raw CBOR-encoded data |

#### Instance Methods

| Method      | Returns     | Description |
|-------------|-------------|-------------|
| `encode()`  | `String`    | Encode as single-part UR string |
| `toBytes()` | `ByteArray` | Extract raw bytes from CBOR payload (unwraps CBOR byte string) |

#### Companion Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| `parse()` | `(urString: String): UR` | Parse a single-part UR string. Throws for multi-part (use `URDecoder`). |
| `fromBytes()` | `(data: ByteArray, type: String = "bytes"): UR` | Create UR by wrapping raw bytes in a CBOR byte string |
| `isValidType()` | `(type: String): Boolean` | Validate a UR type string |

#### Example

```kotlin
// From raw bytes
val ur = UR.fromBytes(psbtBytes, "crypto-psbt")

// Encode to string
val urString = ur.encode()
// "ur:crypto-psbt/hdosjojkidjyzmad..."

// Parse back
val parsed = UR.parse(urString)
val rawBytes = parsed.toBytes()

// From a registry item (structured types)
val hdKey = CryptoHDKey(false, keyData, chainCode, coinInfo, origin, children, fingerprint)
val ur = hdKey.toUR()  // "ur:crypto-hdkey/..."
```

---

### UREncoder

Encodes a UR into single or multi-part UR strings.

```kotlin
import com.gorunjinian.bcur.UREncoder
```

#### Constructor

```kotlin
UREncoder(
    ur: UR,
    maxFragmentLen: Int = 100,
    minFragmentLen: Int = 10,
    firstSeqNum: Long = 0
)
```

| Parameter        | Default | Description |
|------------------|---------|-------------|
| `ur`             | -       | The UR to encode |
| `maxFragmentLen` | `100`   | Maximum fragment size in bytes. Smaller = more parts, smaller QR codes. |
| `minFragmentLen` | `10`    | Minimum fragment size in bytes |
| `firstSeqNum`    | `0`     | Starting sequence number |

#### Properties

| Property      | Type        | Description |
|---------------|-------------|-------------|
| `seqNum`      | `Long`      | Current sequence number |
| `seqLen`      | `Int`       | Total number of pure fragments |
| `partIndexes` | `List<Int>` | Fragment indexes in the most recent part |

#### Methods

| Method           | Returns   | Description |
|------------------|-----------|-------------|
| `nextPart()`     | `String`  | Generate the next UR string. Call repeatedly for animation. |
| `isComplete()`   | `Boolean` | True after all pure parts have been generated |
| `isSinglePart()` | `Boolean` | True if data fits in one part |

#### Companion Methods

| Method     | Signature          | Description |
|------------|---------------------|-------------|
| `encode()` | `(ur: UR): String`  | Static convenience for single-part encoding |

#### Fragment Size Guidance

| PSBT Size     | Recommended `maxFragmentLen` | Notes |
|---------------|------------------------------|-------|
| < 500 bytes   | 500                          | Single QR, no animation needed |
| 500B - 2KB    | 100-200                      | Good balance of QR size and part count |
| 2KB - 10KB    | 150-250                      | Larger QR codes scan faster |
| > 10KB        | 200-400                      | May need many frames |

---

### URDecoder

Decodes single and multi-part UR strings. Supports streaming reception of animated QR codes.

```kotlin
import com.gorunjinian.bcur.URDecoder
```

#### Properties

| Property                  | Type          | Description |
|---------------------------|---------------|-------------|
| `result`                  | `Result?`     | Non-null when decoding is complete |
| `expectedPartCount`       | `Int`         | Total expected pure parts (0 until first part received) |
| `receivedPartIndexes`     | `Set<Int>`    | Indexes of parts successfully received |
| `lastPartIndexes`         | `Set<Int>?`   | Fragment indexes in the last processed part |
| `processedPartsCount`     | `Int`         | Number of unique parts processed |
| `estimatedPercentComplete`| `Double`      | Estimated progress (0.0 to 0.99) |

#### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `receivePart(string: String)` | `Boolean` | Feed a scanned UR string. Returns true if accepted. Check `result` after each call. |

#### Companion Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| `decode()` | `(string: String): UR` | Decode a single-part UR string directly |
| `decode()` | `(type: String, body: String): UR` | Decode with known type |

#### URDecoder.Result

```kotlin
class Result(
    val type: ResultType,  // SUCCESS or FAILURE
    val ur: UR?,           // The decoded UR (on SUCCESS)
    val error: String?     // Error message (on FAILURE)
)
```

---

### Bytewords

BCR-2020-012 Bytewords encoding. Maps bytes to 4-letter English words with CRC32 integrity.

```kotlin
import com.gorunjinian.bcur.Bytewords
```

#### Encoding Styles

```kotlin
enum class Style {
    STANDARD,  // "able acid also..." (space-separated)
    URI,       // "able-acid-also-..." (hyphen-separated)
    MINIMAL    // "aeadao..." (first+last letter, no separator)
}
```

BC-UR uses `MINIMAL` style internally.

#### Methods

| Method     | Signature | Description |
|------------|-----------|-------------|
| `encode()` | `(data: ByteArray, style: Style): String` | Encode with CRC32 checksum appended |
| `decode()` | `(encoded: String, style: Style): ByteArray` | Decode and validate CRC32 |

---

### Cbor (Transport)

Minimal CBOR utilities for the UR transport layer. Only supports the subset needed for UR framing: unsigned integers, byte strings, and arrays.

For full CBOR support (maps, tags, text strings, booleans) used by registry types, see [CborItem](#cboritem).

```kotlin
import com.gorunjinian.bcur.Cbor
```

#### Encoding

| Method | Signature | Description |
|--------|-----------|-------------|
| `encodeUnsignedInt()` | `(value: ULong): ByteArray` | CBOR major type 0 |
| `encodeByteString()` | `(data: ByteArray): ByteArray` | CBOR major type 2 |
| `encodeArrayHeader()` | `(count: Int): ByteArray` | CBOR major type 4 |
| `wrapInByteString()` | `(data: ByteArray): ByteArray` | Convenience: wraps raw bytes in CBOR byte string |

#### Decoding

| Method | Signature | Description |
|--------|-----------|-------------|
| `decodeUnsignedInt()` | `(data: ByteArray, offset: Int): Pair<ULong, Int>` | Returns value and next offset |
| `decodeByteString()` | `(data: ByteArray, offset: Int): Pair<ByteArray, Int>` | Returns bytes and next offset |
| `decodeArrayHeader()` | `(data: ByteArray, offset: Int): Pair<Int, Int>` | Returns count and next offset |
| `unwrapByteString()` | `(cbor: ByteArray): ByteArray` | Convenience: strips CBOR byte string header |

---

### FountainEncoder

Generates fountain-coded parts for multi-part URs. Used internally by `UREncoder`.

```kotlin
import com.gorunjinian.bcur.fountain.FountainEncoder
```

#### Constructor

```kotlin
FountainEncoder(
    message: ByteArray,
    maxFragmentLen: Int,
    minFragmentLen: Int = 10,
    firstSeqNum: Long = 0
)
```

#### Properties

| Property      | Type              | Description |
|---------------|-------------------|-------------|
| `messageLen`  | `Int`             | Original message length |
| `checksum`    | `Long`            | CRC32 of original message |
| `fragmentLen` | `Int`             | Calculated fragment size |
| `fragments`   | `List<ByteArray>` | Message partitioned into fragments |
| `seqLen`      | `Int`             | Number of pure fragments |
| `seqNum`      | `Long`            | Current sequence number |
| `partIndexes` | `List<Int>`       | Indexes in the most recent part |

#### Methods

| Method           | Returns   | Description |
|------------------|-----------|-------------|
| `nextPart()`     | `Part`    | Generate next fountain-coded part |
| `isComplete()`   | `Boolean` | True after all pure parts generated |
| `isSinglePart()` | `Boolean` | True if single fragment |

#### FountainEncoder.Part

```kotlin
data class Part(
    val seqNum: Long,
    val seqLen: Int,
    val messageLen: Int,
    val checksum: Long,
    val data: ByteArray
)
```

| Method            | Returns     | Description |
|-------------------|-------------|-------------|
| `toCborBytes()`   | `ByteArray` | Serialize to CBOR: `[seqNum, seqLen, messageLen, checksum, data]` |
| `fromCborBytes()` | `Part`      | (companion) Deserialize from CBOR bytes |

---

### FountainDecoder

Reassembles messages from fountain-coded parts. Handles both pure and mixed (XOR) fragments.

```kotlin
import com.gorunjinian.bcur.fountain.FountainDecoder
```

#### Properties

| Property                   | Type         | Description |
|----------------------------|--------------|-------------|
| `result`                   | `Result?`    | Non-null when decoding completes |
| `expectedPartCount`        | `Int`        | Total expected parts |
| `receivedPartIndexes`      | `Set<Int>`   | Indexes received so far |
| `lastPartIndexes`          | `Set<Int>?`  | Indexes in last processed part |
| `processedPartsCount`      | `Int`        | Unique parts processed |
| `estimatedPercentComplete` | `Double`     | Progress estimate (0.0 to 0.99) |

#### Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| `receivePart()` | `(encoderPart: FountainEncoder.Part): Boolean` | Process a fountain part. Returns true if accepted. |

---

### CRC32

Pure Kotlin CRC-32 implementation (IEEE 802.3 polynomial).

```kotlin
import com.gorunjinian.bcur.CRC32
```

| Method       | Signature                    | Description |
|--------------|------------------------------|-------------|
| `compute()`  | `(data: ByteArray): Long`    | Compute CRC32 checksum |
| `toBytes()`  | `(crc: Long): ByteArray`     | Convert to 4 big-endian bytes |
| `fromBytes()` | `(bytes: ByteArray): Long`  | Convert from 4 big-endian bytes |

---

### SHA256

Pure Kotlin SHA-256 implementation (FIPS 180-4).

```kotlin
import com.gorunjinian.bcur.SHA256
```

| Method   | Signature                       | Description |
|----------|---------------------------------|-------------|
| `hash()` | `(data: ByteArray): ByteArray`  | Compute SHA-256 hash (32 bytes) |

---

## CBOR Data Model

### CborItem

Full CBOR data model for structured encoding and decoding. Used by all registry types. Independent of the minimal `Cbor` transport utilities.

```kotlin
import com.gorunjinian.bcur.CborItem
```

#### Type Hierarchy

```kotlin
sealed class CborItem {
    val tags: MutableList<ULong>          // Semantic tags (outermost first)

    class UInt(val value: ULong)          // Major type 0: unsigned integer
    class NInt(val value: ULong)          // Major type 1: negative integer (-1 - value)
    class Bytes(val value: ByteArray)     // Major type 2: byte string
    class Text(val value: String)         // Major type 3: UTF-8 text string
    class Array(val items: MutableList<CborItem>)                         // Major type 4
    class Map(val entries: MutableList<Pair<CborItem, CborItem>>)         // Major type 5 (ordered)
    class Bool(val value: Boolean)        // Major type 7: true (0xF5) / false (0xF4)
    object Null                           // Major type 7: null (0xF6)
}
```

#### Constructing CBOR Structures

```kotlin
// Unsigned integer
val num = CborItem.UInt(42)

// Byte string
val bytes = CborItem.Bytes(byteArrayOf(0x01, 0x02, 0x03))

// Text string
val text = CborItem.Text("hello")

// Array
val array = CborItem.Array(CborItem.UInt(1), CborItem.UInt(2), CborItem.UInt(3))

// Map with integer keys (common in BCR registry types)
val map = CborItem.Map()
map.put(CborItem.UInt(1), CborItem.Bool(true))
map.put(CborItem.UInt(3), CborItem.Bytes(keyData))
map.put(CborItem.UInt(5), coinInfoMap)

// Map lookup by integer key
val value = map.get(3)  // Returns CborItem? for key 3
```

### Encoding and Decoding

```kotlin
// Encode any CborItem to bytes
val encoded: ByteArray = item.encode()

// Decode bytes back to a CborItem
val decoded: CborItem = CborItem.decode(encoded)

// Round-trip example
val original = CborItem.Map()
original.put(CborItem.UInt(1), CborItem.Text("test"))
val bytes = original.encode()
val restored = CborItem.decode(bytes) as CborItem.Map
val text = (restored.get(1) as CborItem.Text).value  // "test"
```

### Semantic Tags

CBOR semantic tags (major type 6) are stored as a flat list on every `CborItem`, outermost first. This naturally handles the tag chaining used by `CryptoOutput`.

```kotlin
// Single tag
val coinInfo = CborItem.Map()
coinInfo.tagged(305UL)  // Tag as crypto-coininfo

// Fluent chaining
val item = ecKey.toCbor().tagged(306UL)

// Multiple tags (used by CryptoOutput for script expressions)
// For wsh(wpkh(hdkey)): tags = [401, 404, 303]
val output = CborItem.Map()
output.tags.addAll(listOf(401UL, 404UL, 303UL))
// Encodes as: tag(401) tag(404) tag(303) map{...}

// Reading tags after decode
val decoded = CborItem.decode(bytes)
decoded.tags  // [401, 404, 303] - outermost first
```

---

## Registry Module

### Architecture

The registry module implements the BCR (Blockchain Commons Research) standards for structured UR types. Each registry type is a Kotlin class that can serialize to/from CBOR and convert to/from UR strings.

```
RegistryItem (abstract)
  ├── toCbor(): CborItem      -- Serialize to structured CBOR
  ├── toUR(): UR               -- Serialize to UR (CBOR bytes + type string)
  └── registryType: RegistryType

Companion.fromCbor(CborItem)  -- Deserialize from structured CBOR
```

Two generations of types exist:

| Generation | Tag Range | UR Type Pattern | Example |
|------------|-----------|-----------------|---------|
| **Crypto*** | 300-311 | `crypto-*` | `ur:crypto-hdkey/...` |
| **UR*** | 40300-40311 | Short names | `ur:hdkey/...` |

The UR* variants extend the Crypto* classes and override `registryType` to use the newer tag numbers. Both generations are interoperable.

### RegistryType

Enum mapping UR type strings to CBOR tag numbers and decoder functions.

```kotlin
import com.gorunjinian.bcur.registry.RegistryType
```

| Enum Value | UR Type | CBOR Tag | BCR Spec |
|------------|---------|----------|----------|
| `CRYPTO_SEED` | `crypto-seed` | 300 | BCR-2020-006 |
| `CRYPTO_BIP39` | `crypto-bip39` | 301 | BCR-2020-006 |
| `CRYPTO_HDKEY` | `crypto-hdkey` | 303 | BCR-2020-007 |
| `CRYPTO_KEYPATH` | `crypto-keypath` | 304 | BCR-2020-007 |
| `CRYPTO_COIN_INFO` | `crypto-coininfo` | 305 | BCR-2020-007 |
| `CRYPTO_ECKEY` | `crypto-eckey` | 306 | BCR-2020-007 |
| `CRYPTO_ADDRESS` | `crypto-address` | 307 | BCR-2020-007 |
| `CRYPTO_OUTPUT` | `crypto-output` | 308 | BCR-2020-010 |
| `CRYPTO_SSKR` | `crypto-sskr` | 309 | BCR-2020-006 |
| `CRYPTO_PSBT` | `crypto-psbt` | 310 | BCR-2020-006 |
| `CRYPTO_ACCOUNT` | `crypto-account` | 311 | BCR-2020-015 |
| `SEED` | `seed` | 40300 | UR variant |
| `HDKEY` | `hdkey` | 40303 | UR variant |
| `KEYPATH` | `keypath` | 40304 | UR variant |
| `COIN_INFO` | `coininfo` | 40305 | UR variant |
| `ECKEY` | `eckey` | 40306 | UR variant |
| `ADDRESS` | `address` | 40307 | UR variant |
| `OUTPUT_DESCRIPTOR` | `output-descriptor` | 40308 | UR variant |
| `SSKR` | `sskr` | 40309 | UR variant |
| `PSBT` | `psbt` | 40310 | UR variant |
| `ACCOUNT_DESCRIPTOR` | `account-descriptor` | 40311 | UR variant |

```kotlin
// Lookup by UR type string
val type = RegistryType.fromString("crypto-output")
// type.tag == 308L
// type.type == "crypto-output"
```

### RegistryItem

Abstract base class for all registry types.

```kotlin
import com.gorunjinian.bcur.registry.RegistryItem
```

| Property/Method | Type | Description |
|-----------------|------|-------------|
| `registryType` | `RegistryType` | The BCR registry type of this item |
| `toCbor()` | `CborItem` | Serialize to a structured CBOR item |
| `toUR()` | `UR` | Serialize to a UR (encodes CBOR to bytes, wraps with type string) |

#### Fingerprint Utilities

Static helpers for converting between 4-byte fingerprints and unsigned integers (replaces `java.math.BigInteger` for KMP compatibility):

```kotlin
// ByteArray -> ULong (for CBOR encoding)
val ulong = RegistryItem.fingerprintToULong(byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte()))

// ULong -> ByteArray (for CBOR decoding)
val bytes = RegistryItem.uLongToFingerprint(ulong, 4)
```

---

## Registry Types Reference

### CryptoHDKey

BCR-2020-007: Hierarchical Deterministic key. The most commonly used registry type for exporting wallet public keys via QR.

```kotlin
import com.gorunjinian.bcur.registry.CryptoHDKey
```

**CBOR Tag:** 303 | **UR Type:** `crypto-hdkey`

#### Constructors

```kotlin
// Master key (root of HD tree)
CryptoHDKey(key: ByteArray, chainCode: ByteArray)

// Derived key (with full metadata)
CryptoHDKey(
    privateKey: Boolean?,
    key: ByteArray,
    chainCode: ByteArray?,
    useInfo: CryptoCoinInfo?,
    origin: CryptoKeypath?,
    children: CryptoKeypath?,
    parentFingerprint: ByteArray?,
    name: String? = null,
    note: String? = null
)
```

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `isMaster` | `Boolean` | True for master (root) keys |
| `isPrivateKey` | `Boolean` | True if this is a private key (defaults to false) |
| `key` | `ByteArray` | Key data (33 bytes compressed public, or 32 bytes private) |
| `chainCode` | `ByteArray?` | 32-byte chain code |
| `useInfo` | `CryptoCoinInfo?` | Coin type and network (tag 305) |
| `origin` | `CryptoKeypath?` | Derivation path from master (tag 304) |
| `children` | `CryptoKeypath?` | Allowed child derivation paths (tag 304) |
| `parentFingerprint` | `ByteArray?` | 4-byte parent key fingerprint |
| `name` | `String?` | Human-readable name |
| `note` | `String?` | Human-readable note |

#### CBOR Map Keys

| Key | Field | CBOR Type |
|-----|-------|-----------|
| 1 | `isMaster` | Bool |
| 2 | `isPrivate` | Bool |
| 3 | `keyData` | Bytes |
| 4 | `chainCode` | Bytes |
| 5 | `useInfo` | Map (tagged 305) |
| 6 | `origin` | Map (tagged 304) |
| 7 | `children` | Map (tagged 304) |
| 8 | `parentFingerprint` | UInt |
| 9 | `name` | Text |
| 10 | `note` | Text |

#### Example

```kotlin
val origin = CryptoKeypath(
    listOf(
        IndexPathComponent(84, true),   // 84'
        IndexPathComponent(0, true),    // 0'
        IndexPathComponent(0, true)     // 0'
    ),
    byteArrayOf(0x12, 0x34, 0x56, 0x78)  // master fingerprint
)

val children = CryptoKeypath(
    listOf(IndexPathComponent(0, false), WildcardPathComponent(false)),
    null
)

val hdKey = CryptoHDKey(
    privateKey = false,
    key = compressedPubKey,       // 33 bytes
    chainCode = chainCode,        // 32 bytes
    useInfo = CryptoCoinInfo(CryptoCoinInfo.Type.BITCOIN, CryptoCoinInfo.Network.MAINNET),
    origin = origin,              // m/84'/0'/0'
    children = children,          // 0/*
    parentFingerprint = parentFp  // 4 bytes
)

// Encode to UR
val ur = hdKey.toUR()
val urString = ur.encode()  // "ur:crypto-hdkey/..."

// Decode from CBOR
val decoded = CryptoHDKey.fromCbor(CborItem.decode(ur.cborData))
println(decoded.origin?.path)  // "84'/0'/0'"
```

---

### CryptoKeypath

BCR-2020-007: BIP32 derivation path with source fingerprint.

```kotlin
import com.gorunjinian.bcur.registry.CryptoKeypath
```

**CBOR Tag:** 304 | **UR Type:** `crypto-keypath`

#### Constructor

```kotlin
CryptoKeypath(
    components: List<PathComponent>,
    sourceFingerprint: ByteArray?,  // 4 bytes, truncated automatically
    depth: Int? = null
)
```

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `components` | `List<PathComponent>` | Path components |
| `sourceFingerprint` | `ByteArray?` | 4-byte fingerprint of the key at the source of this path |
| `depth` | `Int?` | BIP32 depth |
| `path` | `String?` | Human-readable path string (e.g., `"84'/0'/0'"`) |

#### Path String Examples

| Path Components | `.path` Output |
|----------------|----------------|
| `[84', 0', 0']` | `"84'/0'/0'"` |
| `[0, *]` | `"0/*"` |
| `[<0;1>]` | `"<0;1>"` |

---

### CryptoCoinInfo

BCR-2020-007: Cryptocurrency type and network identifier.

```kotlin
import com.gorunjinian.bcur.registry.CryptoCoinInfo
```

**CBOR Tag:** 305 | **UR Type:** `crypto-coininfo`

#### Constructor

```kotlin
CryptoCoinInfo(type: Type?, network: Network?)
CryptoCoinInfo(type: Int?, network: Int?)
```

#### Enums

```kotlin
enum class Type(val value: Int) {
    BITCOIN(0),
    ETHEREUM(60)
}

enum class Network(val value: Int) {
    MAINNET(0),
    TESTNET(1),
    GOERLI(4)   // Ethereum only
}
```

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `resolvedType` | `Type` | Coin type (defaults to `BITCOIN` if unset) |
| `resolvedNetwork` | `Network` | Network (defaults to `MAINNET` if unset) |

---

### CryptoECKey

BCR-2020-007: Elliptic curve key.

```kotlin
import com.gorunjinian.bcur.registry.CryptoECKey
```

**CBOR Tag:** 306 | **UR Type:** `crypto-eckey`

#### Constructor

```kotlin
CryptoECKey(curve: Int?, privateKey: Boolean?, data: ByteArray)
```

| Property | Type | Description |
|----------|------|-------------|
| `resolvedCurve` | `Int` | Curve identifier (defaults to 0 = secp256k1) |
| `isPrivateKey` | `Boolean` | True if private key (defaults to false) |
| `data` | `ByteArray` | Raw key bytes |

---

### CryptoAddress

BCR-2020-007: Cryptocurrency address.

```kotlin
import com.gorunjinian.bcur.registry.CryptoAddress
```

**CBOR Tag:** 307 | **UR Type:** `crypto-address`

#### Constructor

```kotlin
CryptoAddress(info: CryptoCoinInfo?, type: Type?, data: ByteArray)
```

#### Address Types

```kotlin
enum class Type {
    P2PKH,   // Legacy
    P2SH,    // Script hash
    P2WPKH   // Native SegWit
}
```

---

### CryptoOutput

BCR-2020-010: Output descriptor with script expression tags. The primary type used for exchanging wallet descriptors between signing devices and watch-only wallets.

```kotlin
import com.gorunjinian.bcur.registry.CryptoOutput
```

**CBOR Tag:** 308 | **UR Type:** `crypto-output`

#### Constructors

```kotlin
// Single EC key
CryptoOutput(scriptExpressions: List<ScriptExpression>, ecKey: CryptoECKey)

// Single HD key
CryptoOutput(scriptExpressions: List<ScriptExpression>, hdKey: CryptoHDKey)

// Multi-signature
CryptoOutput(scriptExpressions: List<ScriptExpression>, multiKey: MultiKey)
```

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `scriptExpressions` | `List<ScriptExpression>` | Script wrappers, outermost first |
| `ecKey` | `CryptoECKey?` | EC key (if single-key EC) |
| `hdKey` | `CryptoHDKey?` | HD key (if single-key HD) |
| `multiKey` | `MultiKey?` | Multi-sig keys (if multi-key) |

#### Tag Chaining

`CryptoOutput` uses CBOR tag chaining to represent nested script expressions. For `wsh(wpkh(hdkey))`, the CBOR structure is:

```
tag(401)           -- wsh (outermost script)
  tag(404)         -- wpkh
    tag(303)       -- crypto-hdkey
      map{...}     -- the HD key data
```

#### Example: Single-sig wpkh

```kotlin
val hdKey = CryptoHDKey(false, pubkey, chainCode, coinInfo, origin, children, fingerprint)

val output = CryptoOutput(
    listOf(ScriptExpression.WITNESS_PUBLIC_KEY_HASH),  // wpkh(...)
    hdKey
)

val ur = output.toUR()
// ur.type == "crypto-output"
// Encodes as: tag(404) tag(303) map{key data...}
```

#### Example: Multi-sig wsh(sorted(...))

```kotlin
val key1 = CryptoHDKey(false, pubkey1, chain1, null, origin1, children1, fp1)
val key2 = CryptoHDKey(false, pubkey2, chain2, null, origin2, children2, fp2)
val multiKey = MultiKey(2, emptyList(), listOf(key1, key2))

val output = CryptoOutput(
    listOf(ScriptExpression.WITNESS_SCRIPT_HASH, ScriptExpression.SORTED_MULTISIG),
    multiKey
)

val ur = output.toUR()
// Encodes as: tag(401) tag(407) map{threshold, keys...}
```

---

### CryptoAccount

BCR-2020-015: Account descriptor containing a master fingerprint and multiple output descriptors.

```kotlin
import com.gorunjinian.bcur.registry.CryptoAccount
```

**CBOR Tag:** 311 | **UR Type:** `crypto-account`

#### Constructor

```kotlin
CryptoAccount(
    masterFingerprint: ByteArray,          // 4 bytes
    outputDescriptors: List<CryptoOutput>
)
```

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `masterFingerprint` | `ByteArray` | 4-byte master key fingerprint |
| `outputDescriptors` | `List<CryptoOutput>` | Output descriptors for this account |

#### Example

```kotlin
val wpkhOutput = CryptoOutput(
    listOf(ScriptExpression.WITNESS_PUBLIC_KEY_HASH),
    wpkhKey
)
val trOutput = CryptoOutput(
    listOf(ScriptExpression.TAPROOT),
    trKey
)

val account = CryptoAccount(
    masterFingerprint = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte()),
    outputDescriptors = listOf(wpkhOutput, trOutput)
)

val ur = account.toUR()  // "ur:crypto-account/..."
```

---

### CryptoSeed

BCR-2020-006: Cryptographic seed.

```kotlin
import com.gorunjinian.bcur.registry.CryptoSeed
```

**CBOR Tag:** 300 | **UR Type:** `crypto-seed`

#### Constructor

```kotlin
CryptoSeed(
    seed: ByteArray,
    birthdate: Long?,       // Days since Unix epoch (1970-01-01)
    name: String? = null,
    note: String? = null
)
```

The `birthdate` is encoded with CBOR tag 100 (days since epoch). Using `Long` instead of `java.util.Date` for KMP compatibility.

---

### CryptoBip39

BCR-2020-006: BIP39 mnemonic word list.

```kotlin
import com.gorunjinian.bcur.registry.CryptoBip39
```

**CBOR Tag:** 301 | **UR Type:** `crypto-bip39`

#### Constructor

```kotlin
CryptoBip39(words: List<String>, language: String? = null)
```

---

### CryptoSskr

BCR-2020-006: SSKR (Sharded Secret Key Reconstruction) share.

```kotlin
import com.gorunjinian.bcur.registry.CryptoSskr
```

**CBOR Tag:** 309 | **UR Type:** `crypto-sskr`

Encodes as a bare CBOR byte string (not a map). The `fromCbor` decoder strips the first byte of the payload (SSKR header normalization).

---

### CryptoPSBT

BCR-2020-006: Partially Signed Bitcoin Transaction.

```kotlin
import com.gorunjinian.bcur.registry.CryptoPSBT
```

**CBOR Tag:** 310 | **UR Type:** `crypto-psbt`

Encodes as a bare CBOR byte string. For most use cases, prefer `UR.fromBytes(psbtBytes, "crypto-psbt")` which produces compatible output.

```kotlin
// These produce equivalent UR output:
val ur1 = CryptoPSBT(psbtBytes).toUR()
val ur2 = UR.fromBytes(psbtBytes, "crypto-psbt")
```

---

### MultiKey

Multi-signature key container. Used within `CryptoOutput` for multi-sig output descriptors.

```kotlin
import com.gorunjinian.bcur.registry.MultiKey
```

Implements `CborSerializable` but NOT `RegistryItem` -- it cannot be independently serialized to a UR.

#### Constructor

```kotlin
MultiKey(
    threshold: Int,
    ecKeys: List<CryptoECKey>,
    hdKeys: List<CryptoHDKey>
)
```

One of `ecKeys` or `hdKeys` must be non-empty. Each key is tagged with its respective type tag (306 or 303) in the CBOR array.

---

### ScriptExpression

Script expression tags used by `CryptoOutput` to describe output descriptor script types.

```kotlin
import com.gorunjinian.bcur.registry.ScriptExpression
```

| Enum Value | CBOR Tag | Expression | Descriptor |
|------------|----------|------------|------------|
| `SCRIPT_HASH` | 400 | `sh` | P2SH wrapper |
| `WITNESS_SCRIPT_HASH` | 401 | `wsh` | P2WSH |
| `PUBLIC_KEY` | 402 | `pk` | Raw public key |
| `PUBLIC_KEY_HASH` | 403 | `pkh` | P2PKH (BIP44) |
| `WITNESS_PUBLIC_KEY_HASH` | 404 | `wpkh` | P2WPKH (BIP84) |
| `COMBO` | 405 | `combo` | Combo descriptor |
| `MULTISIG` | 406 | `multi` | Multi-signature |
| `SORTED_MULTISIG` | 407 | `sorted` | Sorted multi-sig |
| `ADDRESS` | 307 | `addr` | Address |
| `RAW_SCRIPT` | 408 | `raw` | Raw script |
| `TAPROOT` | 409 | `tr` | P2TR (BIP86) |
| `COSIGNER` | 410 | `cosigner` | Co-signer |

```kotlin
// Common descriptor patterns:
// wpkh(key)       -> [WITNESS_PUBLIC_KEY_HASH]
// sh(wpkh(key))   -> [SCRIPT_HASH, WITNESS_PUBLIC_KEY_HASH]
// wsh(multi(...))  -> [WITNESS_SCRIPT_HASH, MULTISIG]
// tr(key)         -> [TAPROOT]
```

---

### Path Components

BIP32 key path components used by `CryptoKeypath`.

```kotlin
import com.gorunjinian.bcur.registry.pathcomponent.*
```

All extend the sealed class `PathComponent`:

| Class | Description | Example | `toString()` |
|-------|-------------|---------|---------------|
| `IndexPathComponent(index, hardened)` | Single derivation index | BIP44 level | `"84'"`, `"0"` |
| `RangePathComponent(start, end, hardened)` | Index range | Gap limit | `"[0-99]"` |
| `WildcardPathComponent(hardened)` | Any index | Child keys | `"*"` |
| `PairPathComponent(external, internal)` | Internal/external pair | Change paths | `"<0;1>"` |

```kotlin
// m/84'/0'/0'/0/*
val components = listOf(
    IndexPathComponent(84, true),
    IndexPathComponent(0, true),
    IndexPathComponent(0, true),
    IndexPathComponent(0, false),
    WildcardPathComponent(false)
)
val keypath = CryptoKeypath(components, masterFingerprint)
keypath.path  // "84'/0'/0'/0/*"
```

The `HARDENED_BIT` (`0x80000000`) is validated in constructors -- passing an index with the bit already set throws `IllegalArgumentException`.

---

## UR Variant Types

The UR* variants use newer CBOR tags (40xxx range) and shorter UR type names. They extend the corresponding Crypto* classes and are fully interoperable.

| UR Variant | Extends | CBOR Tag | UR Type |
|------------|---------|----------|---------|
| `URSeed` | `CryptoSeed` | 40300 | `seed` |
| `URHDKey` | `CryptoHDKey` | 40303 | `hdkey` |
| `URKeypath` | `CryptoKeypath` | 40304 | `keypath` |
| `URCoinInfo` | `CryptoCoinInfo` | 40305 | `coininfo` |
| `URECKey` | `CryptoECKey` | 40306 | `eckey` |
| `URAddress` | `CryptoAddress` | 40307 | `address` |
| `URSSKR` | `CryptoSskr` | 40309 | `sskr` |
| `URPSBT` | `CryptoPSBT` | 40310 | `psbt` |
| `UROutputDescriptor` | `RegistryItem` (new type) | 40308 | `output-descriptor` |
| `URAccountDescriptor` | `RegistryItem` (new type) | 40311 | `account-descriptor` |

### URHDKey

`URHDKey` extends `CryptoHDKey` and **re-tags** inner fields with 40xxx tags:

- `useInfo`: 305 -> 40305
- `origin`: 304 -> 40304
- `children`: 304 -> 40304

```kotlin
import com.gorunjinian.bcur.registry.URHDKey

val urKey = URHDKey(false, keyData, chainCode, urCoinInfo, urOrigin, urChildren, fingerprint)
val ur = urKey.toUR()  // ur.type == "hdkey"
```

### UROutputDescriptor

A string-based output descriptor format with optional structured key metadata. Unlike `CryptoOutput` which uses CBOR tag chaining, this uses a plain text `source` string.

```kotlin
import com.gorunjinian.bcur.registry.UROutputDescriptor

val descriptor = UROutputDescriptor(
    source = "wpkh([deadbeef/84'/0'/0']xpub.../0/*)",
    keys = listOf(urHdKey),  // Optional: URHDKey, URECKey, or URAddress
    name = "My Wallet",
    note = "Primary spending"
)

val ur = descriptor.toUR()  // ur.type == "output-descriptor"
```

### URAccountDescriptor

Account-level descriptor containing a master fingerprint and a list of `UROutputDescriptor` items.

```kotlin
import com.gorunjinian.bcur.registry.URAccountDescriptor

val account = URAccountDescriptor(
    masterFingerprint = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte()),
    outputDescriptors = listOf(wpkhDescriptor, trDescriptor)
)

val ur = account.toUR()  // ur.type == "account-descriptor"
```

---

## PSBT Encoding Guide

### Step 1: Create a UR

```kotlin
import com.gorunjinian.bcur.UR

val psbtBytes: ByteArray = // ... your raw PSBT binary

// Option A: Using UR.fromBytes (wraps in CBOR byte string)
val ur = UR.fromBytes(psbtBytes, "crypto-psbt")

// Option B: Using CryptoPSBT registry type
val ur = CryptoPSBT(psbtBytes).toUR()
```

### Step 2: Encode for QR Display

**Small PSBTs (single QR code):**

```kotlin
import com.gorunjinian.bcur.UREncoder

val urString = UREncoder.encode(ur)
// Display urString as QR code
```

**Large PSBTs (animated QR codes):**

```kotlin
val encoder = UREncoder(ur, maxFragmentLen = 150)

if (encoder.isSinglePart()) {
    displayStaticQr(encoder.nextPart())
} else {
    // Animation loop at ~5 fps
    while (isActive) {
        val part = encoder.nextPart()
        displayQrCode(part)
        delay(200)
    }
}
```

---

## PSBT Decoding Guide

### Single-Part UR

```kotlin
val ur = UR.parse(scannedString)
if (ur.type == "crypto-psbt") {
    val psbtBytes = ur.toBytes()
}
```

### Multi-Part UR (Animated QR Scanning)

```kotlin
val decoder = URDecoder()

fun onQrCodeScanned(content: String) {
    decoder.receivePart(content)

    decoder.result?.let { result ->
        if (result.type == ResultType.SUCCESS) {
            val ur = result.ur!!
            if (ur.type == "crypto-psbt") {
                val psbtBytes = ur.toBytes()
                processSignedPsbt(psbtBytes)
            }
        }
    }
}
```

---

## Animated QR Codes (Multi-Part URs)

### How Fountain Codes Work

1. The message is partitioned into N fragments of equal size
2. Parts 1 through N are "pure" - each contains exactly one fragment
3. Parts N+1 and beyond are "mixed" - each is the XOR of a deterministically selected subset of fragments
4. A decoder collects parts and uses XOR reduction to recover missing fragments
5. Any sufficient combination of parts (pure or mixed) can reconstruct the message

### Displaying Animated QR Codes

```kotlin
class AnimatedQrViewModel : ViewModel() {
    private var encoder: UREncoder? = null
    private var animationJob: Job? = null

    val currentQrContent = MutableLiveData<String>()

    fun startAnimation(psbtBytes: ByteArray) {
        val ur = UR.fromBytes(psbtBytes, "crypto-psbt")
        val enc = UREncoder(ur, maxFragmentLen = 150)
        encoder = enc

        if (enc.isSinglePart()) {
            currentQrContent.value = enc.nextPart()
            return
        }

        animationJob = viewModelScope.launch {
            while (isActive) {
                currentQrContent.postValue(enc.nextPart())
                delay(200) // ~5 fps
            }
        }
    }
}
```

### Scanning Animated QR Codes

```kotlin
class QrScannerViewModel : ViewModel() {
    private val decoder = URDecoder()

    val scanProgress = MutableLiveData<String>()
    val decodedPsbt = MutableLiveData<ByteArray>()

    fun onQrCodeScanned(content: String) {
        if (!content.lowercase().startsWith("ur:")) return

        decoder.receivePart(content)

        val received = decoder.receivedPartIndexes.size
        val total = decoder.expectedPartCount
        val percent = (decoder.estimatedPercentComplete * 100).toInt()
        scanProgress.value = "$received/$total parts ($percent%)"

        decoder.result?.let { result ->
            if (result.type == ResultType.SUCCESS) {
                decodedPsbt.value = result.ur!!.toBytes()
            }
        }
    }
}
```

---

## Descriptor Encoding Guide

### Encoding a Single-Sig Descriptor

```kotlin
import com.gorunjinian.bcur.registry.*
import com.gorunjinian.bcur.registry.pathcomponent.*

// Build the key path: m/84'/0'/0'
val origin = CryptoKeypath(
    listOf(
        IndexPathComponent(84, true),
        IndexPathComponent(0, true),
        IndexPathComponent(0, true)
    ),
    masterFingerprint  // 4-byte ByteArray
)

// Child derivation: 0/*
val children = CryptoKeypath(
    listOf(IndexPathComponent(0, false), WildcardPathComponent(false)),
    null
)

// Build the HD key
val hdKey = CryptoHDKey(
    privateKey = false,
    key = compressedPubKey,
    chainCode = chainCode,
    useInfo = CryptoCoinInfo(CryptoCoinInfo.Type.BITCOIN, CryptoCoinInfo.Network.MAINNET),
    origin = origin,
    children = children,
    parentFingerprint = parentFp
)

// Wrap in wpkh() output descriptor
val output = CryptoOutput(
    listOf(ScriptExpression.WITNESS_PUBLIC_KEY_HASH),
    hdKey
)

// Encode as UR for QR display
val ur = output.toUR()
val urString = ur.encode()  // "ur:crypto-output/..."
```

### Decoding a Descriptor from QR

```kotlin
val ur = UR.parse(scannedString)

when (ur.type) {
    "crypto-output" -> {
        val output = CryptoOutput.fromCbor(CborItem.decode(ur.cborData))
        val expressions = output.scriptExpressions  // e.g., [WITNESS_PUBLIC_KEY_HASH]
        val hdKey = output.hdKey                     // the HD key with derivation path
        val path = hdKey?.origin?.path               // e.g., "84'/0'/0'"
    }
    "output-descriptor" -> {
        val descriptor = UROutputDescriptor.fromCbor(CborItem.decode(ur.cborData))
        val source = descriptor.source  // e.g., "wpkh([deadbeef/84'/0'/0']xpub.../0/*)"
    }
    "crypto-account" -> {
        val account = CryptoAccount.fromCbor(CborItem.decode(ur.cborData))
        val fingerprint = account.masterFingerprint
        val outputs = account.outputDescriptors  // List<CryptoOutput>
    }
}
```

### Encoding a Multi-Sig Descriptor

```kotlin
// Build keys for each signer
val key1 = CryptoHDKey(false, pubkey1, chain1, null, origin1, children1, fp1)
val key2 = CryptoHDKey(false, pubkey2, chain2, null, origin2, children2, fp2)
val key3 = CryptoHDKey(false, pubkey3, chain3, null, origin3, children3, fp3)

// 2-of-3 multi-sig
val multiKey = MultiKey(2, emptyList(), listOf(key1, key2, key3))

// wsh(sorted(2-of-3))
val output = CryptoOutput(
    listOf(ScriptExpression.WITNESS_SCRIPT_HASH, ScriptExpression.SORTED_MULTISIG),
    multiKey
)

val ur = output.toUR()  // "ur:crypto-output/..."
```

---

## Complete Examples

### Example 1: Full UR Round-Trip

```kotlin
import com.gorunjinian.bcur.*
import com.gorunjinian.bcur.registry.*

fun roundTripTest() {
    val originalData = ByteArray(5000) { it.toByte() }
    val ur = UR.fromBytes(originalData, "crypto-psbt")

    // Encode to multi-part
    val encoder = UREncoder(ur, maxFragmentLen = 200)

    // Decode from multi-part
    val decoder = URDecoder()
    do {
        decoder.receivePart(encoder.nextPart())
    } while (decoder.result == null)

    assert(decoder.result!!.type == ResultType.SUCCESS)
    assert(decoder.result!!.ur!!.toBytes().contentEquals(originalData))
}
```

### Example 2: Registry Type Round-Trip

```kotlin
import com.gorunjinian.bcur.CborItem
import com.gorunjinian.bcur.registry.*
import com.gorunjinian.bcur.registry.pathcomponent.*

// Build a CryptoHDKey
val origin = CryptoKeypath(
    listOf(IndexPathComponent(84, true), IndexPathComponent(0, true), IndexPathComponent(0, true)),
    byteArrayOf(0x12, 0x34, 0x56, 0x78)
)
val hdKey = CryptoHDKey(false, ByteArray(33), ByteArray(32), null, origin, null, null)

// Encode to CBOR bytes
val cborBytes = hdKey.toCbor().encode()

// Decode from CBOR bytes
val restored = CryptoHDKey.fromCbor(CborItem.decode(cborBytes))
assert(restored.origin?.path == "84'/0'/0'")

// Full UR round-trip
val ur = hdKey.toUR()
assert(ur.type == "crypto-hdkey")
val fromUr = CryptoHDKey.fromCbor(CborItem.decode(ur.cborData))
assert(fromUr.origin?.path == "84'/0'/0'")
```

### Example 3: Universal QR Format Detector

```kotlin
fun handleScannedQr(content: String) {
    when {
        content.lowercase().startsWith("ur:") -> {
            val parts = content.lowercase().substringAfter("ur:").split("/")
            if (parts.size == 3) {
                // Multi-part: feed to URDecoder
                urDecoder.receivePart(content)
            } else {
                // Single-part: decode directly
                val ur = UR.parse(content)
                handleDecodedUR(ur)
            }
        }
        content.startsWith("B\$") -> handleBbqr(content)
        content.startsWith("cHNidP8") -> handleBase64Psbt(content)
    }
}

fun handleDecodedUR(ur: UR) {
    when (ur.type) {
        "crypto-psbt" -> {
            val psbtBytes = ur.toBytes()
            processPsbt(psbtBytes)
        }
        "crypto-output" -> {
            val output = CryptoOutput.fromCbor(CborItem.decode(ur.cborData))
            processDescriptor(output)
        }
        "crypto-account" -> {
            val account = CryptoAccount.fromCbor(CborItem.decode(ur.cborData))
            processAccount(account)
        }
        "output-descriptor" -> {
            val descriptor = UROutputDescriptor.fromCbor(CborItem.decode(ur.cborData))
            processOutputDescriptor(descriptor)
        }
        "crypto-hdkey", "hdkey" -> {
            val hdKey = CryptoHDKey.fromCbor(CborItem.decode(ur.cborData))
            processHDKey(hdKey)
        }
    }
}
```

---

## Error Handling

### UR Parsing Errors

```kotlin
try {
    val ur = UR.parse(scannedString)
} catch (e: UR.InvalidURException) {
    // Invalid format, bad type, or multi-part string
}
```

### Bytewords Errors

```kotlin
try {
    val decoded = Bytewords.decode(encoded, Bytewords.Style.MINIMAL)
} catch (e: Bytewords.InvalidBytewordsException) {
    // Unknown byteword
} catch (e: Bytewords.InvalidChecksumException) {
    // CRC32 validation failed
}
```

### Registry Type Errors

```kotlin
// CBOR decoding errors
try {
    val item = CborItem.decode(bytes)
    val hdKey = CryptoHDKey.fromCbor(item)
} catch (e: IllegalArgumentException) {
    // Malformed CBOR data
} catch (e: IllegalStateException) {
    // Missing required fields (e.g., "Key data is null")
} catch (e: ClassCastException) {
    // Unexpected CBOR type (e.g., expected Map but got Bytes)
}

// RegistryType lookup
try {
    val type = RegistryType.fromString("unknown-type")
} catch (e: IllegalArgumentException) {
    // "Unknown UR registry type: unknown-type"
}

// ScriptExpression lookup
try {
    val expr = ScriptExpression.fromTagValue(999)
} catch (e: IllegalArgumentException) {
    // "Unknown script expression tag value: 999"
}
```

### Exception Summary

| Exception | When Thrown |
|-----------|------------|
| `UR.InvalidURException` | Invalid UR string, bad type, multi-part passed to `parse()` |
| `Bytewords.InvalidBytewordsException` | Unknown byteword during decode |
| `Bytewords.InvalidChecksumException` | CRC32 mismatch or data too short |
| `IllegalArgumentException` | Invalid CBOR data, unknown registry type, invalid path component, Goerli on non-Ethereum |
| `IllegalStateException` | Missing required CBOR fields during deserialization |
| `ClassCastException` | Unexpected CBOR structure (wrong major type) |

---

## Specifications

| Standard | Description |
|----------|-------------|
| [BCR-2020-005](https://github.com/BlockchainCommons/Research/blob/master/papers/bcr-2020-005-ur.md) | Uniform Resources (UR) |
| [BCR-2020-006](https://github.com/BlockchainCommons/Research/blob/master/papers/bcr-2020-006-urtypes.md) | UR Types (Seed, BIP39, SSKR, PSBT) |
| [BCR-2020-007](https://github.com/BlockchainCommons/Research/blob/master/papers/bcr-2020-007-hdkey.md) | HD Keys, Keypaths, Coin Info, EC Keys, Addresses |
| [BCR-2020-010](https://github.com/BlockchainCommons/Research/blob/master/papers/bcr-2020-010-output-desc.md) | Output Descriptors (CryptoOutput, ScriptExpressions) |
| [BCR-2020-012](https://github.com/BlockchainCommons/Research/blob/master/papers/bcr-2020-012-bytewords.md) | Bytewords |
| [BCR-2020-015](https://github.com/BlockchainCommons/Research/blob/master/papers/bcr-2020-015-account.md) | Crypto Account |
| [BCR-2024-001](https://github.com/BlockchainCommons/Research/blob/master/papers/bcr-2024-001-multipart-ur.md) | Multipart UR (MUR) |
| [RFC 8949](https://www.rfc-editor.org/rfc/rfc8949) | CBOR (Concise Binary Object Representation) |

## References

- [sparrowwallet/hummingbird](https://github.com/sparrowwallet/hummingbird) (Java reference)
- [BlockchainCommons/URKit](https://github.com/BlockchainCommons/URKit) (Swift reference)

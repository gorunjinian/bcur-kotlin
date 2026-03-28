# BC-UR (Uniform Resources) Documentation

Complete reference for `bcur-kotlin` - a pure Kotlin Multiplatform implementation of BC-UR with full encoder and decoder support.

**Package:** `com.gorunjinian.bcur`

---

## Table of Contents

1. [Overview](#overview)
2. [API Reference](#api-reference)
   - [UR](#ur)
   - [UREncoder](#urencoder)
   - [URDecoder](#urdecoder)
   - [Bytewords](#bytewords)
   - [Cbor](#cbor)
   - [FountainEncoder](#fountainencoder)
   - [FountainDecoder](#fountaindecoder)
   - [CRC32](#crc32)
   - [SHA256](#sha256)
3. [PSBT Encoding Guide](#psbt-encoding-guide)
4. [PSBT Decoding Guide](#psbt-decoding-guide)
5. [Animated QR Codes (Multi-Part URs)](#animated-qr-codes-multi-part-urs)
6. [UR String Format](#ur-string-format)
7. [CBOR Encoding Requirements](#cbor-encoding-requirements)
8. [Error Handling](#error-handling)
9. [Complete Examples](#complete-examples)

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
Where `1-10` means "part 1 of 10 total pure parts".

### Library Characteristics

- Pure Kotlin, zero external dependencies
- Kotlin Multiplatform: Android, JVM, iOS, macOS, watchOS
- Full encode AND decode (unlike encoder-only alternatives)
- Cross-compatible with hummingbird (Java) and URKit (Swift)
- Includes pure Kotlin CRC32, SHA256, and CBOR implementations

---

## API Reference

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

#### Exceptions

- `UR.InvalidURException(message: String)` - Invalid UR format, bad type, or multi-part string passed to `parse()`

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

#### Constructor

```kotlin
URDecoder()  // No parameters
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

| Method | Signature | Returns | Description |
|--------|-----------|---------|-------------|
| `receivePart()` | `(string: String): Boolean` | `Boolean` | Feed a scanned UR string. Returns true if accepted. Check `result` after each call. |

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

#### Example: Streaming Multi-Part Decode

```kotlin
val decoder = URDecoder()

fun onQrScanned(content: String) {
    if (!content.lowercase().startsWith("ur:")) return

    decoder.receivePart(content)

    // Show progress
    val percent = (decoder.estimatedPercentComplete * 100).toInt()
    val received = decoder.receivedPartIndexes.size
    val total = decoder.expectedPartCount
    showProgress("$received/$total parts ($percent%)")

    // Check for completion
    decoder.result?.let { result ->
        when (result.type) {
            ResultType.SUCCESS -> {
                val ur = result.ur!!
                handleDecodedUR(ur)
            }
            ResultType.FAILURE -> {
                showError(result.error ?: "Decoding failed")
            }
        }
    }
}
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

#### Exceptions

- `InvalidBytewordsException` - Unknown byteword
- `InvalidChecksumException` - CRC32 mismatch or data too short

---

### Cbor

Minimal CBOR utilities for UR. Only supports the subset needed: unsigned integers, byte strings, and arrays.

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

#### Part Generation Strategy

```
seqNum 1..seqLen    --> Pure parts: one fragment each
seqNum seqLen+1..   --> Mixed parts: XOR of deterministically selected fragments
```

Mixed parts provide redundancy. A decoder can recover from missed frames.

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

#### FountainDecoder.Result

```kotlin
class Result(
    val type: ResultType,    // SUCCESS or FAILURE
    val data: ByteArray?,    // Reassembled message (on SUCCESS)
    val error: String?       // Error message (on FAILURE)
)
```

#### How the Decoder Works

1. **Pure parts** (seqNum <= seqLen) contain a single fragment. These are recorded directly.
2. **Mixed parts** (seqNum > seqLen) contain XOR of multiple fragments. The decoder reduces them using known fragments until they become simple.
3. When all fragment indexes have been received, the message is reassembled and CRC32-verified.

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

## PSBT Encoding Guide

### Step 1: CBOR-Encode the PSBT

BC-UR requires CBOR-encoded data. For PSBTs, wrap the raw bytes in a CBOR byte string:

```kotlin
import com.gorunjinian.bcur.Cbor
import com.gorunjinian.bcur.UR

val psbtBytes: ByteArray = // ... your raw PSBT binary

// Option A: Using Cbor utility
val cborData = Cbor.wrapInByteString(psbtBytes)
val ur = UR("crypto-psbt", cborData)

// Option B: Using UR.fromBytes convenience
val ur = UR.fromBytes(psbtBytes, "crypto-psbt")
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
    val handler = Handler(Looper.getMainLooper())
    val runnable = object : Runnable {
        override fun run() {
            val part = encoder.nextPart()
            displayQrCode(part)
            handler.postDelayed(this, 200L)
        }
    }
    handler.post(runnable)
}
```

Continue the animation even after `isComplete()` to generate redundancy parts that help the scanner recover from missed frames.

---

## PSBT Decoding Guide

### Single-Part UR

```kotlin
import com.gorunjinian.bcur.UR

val ur = UR.parse(scannedString)
if (ur.type == "crypto-psbt") {
    val psbtBytes = ur.toBytes()
    // Use psbtBytes with your PSBT library
}
```

### Multi-Part UR (Animated QR Scanning)

```kotlin
import com.gorunjinian.bcur.URDecoder
import com.gorunjinian.bcur.ResultType

val decoder = URDecoder()

// Called by your QR scanner for each frame
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

### Universal Format Detector

```kotlin
fun handleScannedQr(content: String) {
    when {
        // BC-UR format
        content.lowercase().startsWith("ur:") -> {
            val parts = content.lowercase().substringAfter("ur:").split("/")
            if (parts.size == 3) {
                // Multi-part: feed to URDecoder
                urDecoder.receivePart(content)
            } else {
                // Single-part: decode directly
                val ur = UR.parse(content)
                handleDecodedPsbt(ur.toBytes())
            }
        }

        // BBQr format
        content.startsWith("B\$") -> {
            bbqrJoiner.addPart(content)
        }

        // Raw base64 PSBT
        content.startsWith("cHNidP8") -> {
            val psbtBytes = Base64.decode(content, Base64.DEFAULT)
            handleDecodedPsbt(psbtBytes)
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
    val progressText = MutableLiveData<String>()

    fun startPsbtAnimation(psbtBytes: ByteArray) {
        val ur = UR.fromBytes(psbtBytes, "crypto-psbt")
        val enc = UREncoder(ur, maxFragmentLen = 150)
        encoder = enc

        if (enc.isSinglePart()) {
            currentQrContent.value = enc.nextPart()
            progressText.value = "Single QR code"
            return
        }

        animationJob = viewModelScope.launch {
            while (isActive) {
                val part = enc.nextPart()
                currentQrContent.postValue(part)

                val current = if (enc.seqNum <= enc.seqLen) enc.seqNum else (enc.seqNum % enc.seqLen) + 1
                progressText.postValue("Part $current/${enc.seqLen}")

                delay(200) // ~5 fps
            }
        }
    }

    fun stopAnimation() {
        animationJob?.cancel()
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

        // Update progress
        val received = decoder.receivedPartIndexes.size
        val total = decoder.expectedPartCount
        val percent = (decoder.estimatedPercentComplete * 100).toInt()
        scanProgress.value = "$received/$total parts ($percent%)"

        // Check completion
        decoder.result?.let { result ->
            if (result.type == ResultType.SUCCESS) {
                decodedPsbt.value = result.ur!!.toBytes()
            }
        }
    }
}
```

---

## UR String Format

### Single-Part

```
ur:<type>/<bytewords-minimal-payload>
```

Example:
```
ur:crypto-psbt/hdosjojkidjyzmadaenyaoaeaeaeao...
```

### Multi-Part

```
ur:<type>/<seqNum>-<seqLen>/<bytewords-minimal-cbor-part>
```

Example:
```
ur:crypto-psbt/1-5/lpadaxcsencylobemohsgmoyadhd...
ur:crypto-psbt/2-5/lpaoaxcsencylobemohsgmoyad...
```

### Common UR Types for Bitcoin

| UR Type          | Description |
|------------------|-------------|
| `crypto-psbt`    | Partially Signed Bitcoin Transaction |
| `crypto-account` | HD account descriptor |
| `crypto-hdkey`   | HD key |
| `crypto-output`  | Output descriptor |
| `crypto-seed`    | Cryptographic seed |
| `bytes`          | Generic byte payload |

---

## CBOR Encoding Requirements

BC-UR payloads must be CBOR-encoded. For PSBTs, the encoding is a simple CBOR byte string wrapping the raw PSBT bytes.

### Manual CBOR Wrapping (if needed)

```kotlin
// The Cbor utility handles this automatically:
val cborData = Cbor.wrapInByteString(rawBytes)

// This produces a CBOR byte string (major type 2):
// For data <= 23 bytes:    [0x40+len, ...data...]
// For data <= 255 bytes:   [0x58, len, ...data...]
// For data <= 65535 bytes: [0x59, lenHi, lenLo, ...data...]
// For larger data:         [0x5A, len3, len2, len1, len0, ...data...]
```

### Unwrapping

```kotlin
val rawBytes = Cbor.unwrapByteString(cborData)
// Or via UR convenience:
val rawBytes = ur.toBytes()
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

### URDecoder (Streaming)

The `URDecoder.receivePart()` method silently returns `false` for invalid parts instead of throwing. Check `result` for the final outcome:

```kotlin
decoder.receivePart(part) // Returns false for invalid input
decoder.result?.let {
    when (it.type) {
        ResultType.SUCCESS -> { /* use it.ur!! */ }
        ResultType.FAILURE -> { /* check it.error */ }
    }
}
```

### Exception Summary

| Exception | When Thrown |
|-----------|------------|
| `UR.InvalidURException` | Invalid UR string, bad type, multi-part passed to `parse()` |
| `Bytewords.InvalidBytewordsException` | Unknown byteword during decode |
| `Bytewords.InvalidChecksumException` | CRC32 mismatch or data too short |
| `IllegalArgumentException` | Invalid CBOR data, Xoshiro zero state |

---

## Complete Examples

### Example 1: Encode PSBT for QR Display

```kotlin
import com.gorunjinian.bcur.*

fun encodePsbtForQr(psbtBytes: ByteArray): List<String> {
    val ur = UR.fromBytes(psbtBytes, "crypto-psbt")
    val encoder = UREncoder(ur, maxFragmentLen = 150)

    if (encoder.isSinglePart()) {
        return listOf(encoder.nextPart())
    }

    // Generate all pure parts plus some redundancy
    val parts = mutableListOf<String>()
    val targetParts = (encoder.seqLen * 1.5).toInt()
    repeat(targetParts) {
        parts.add(encoder.nextPart())
    }
    return parts
}
```

### Example 2: Complete Scan-to-Broadcast Flow

```kotlin
import com.gorunjinian.bcur.*

class PsbtScannerActivity : AppCompatActivity() {
    private val decoder = URDecoder()

    fun handleScannedQr(content: String) {
        when {
            content.lowercase().startsWith("ur:") -> handleUrQr(content)
            content.startsWith("B\$") -> handleBbqrCode(content)
            content.startsWith("cHNidP8") -> handleBase64Psbt(content)
        }
    }

    private fun handleUrQr(content: String) {
        decoder.receivePart(content)

        updateProgress(
            received = decoder.receivedPartIndexes.size,
            total = decoder.expectedPartCount,
            percent = decoder.estimatedPercentComplete
        )

        decoder.result?.let { result ->
            if (result.type == ResultType.SUCCESS) {
                val psbtBytes = result.ur!!.toBytes()
                finalizePsbt(psbtBytes)
            }
        }
    }

    private fun finalizePsbt(psbtBytes: ByteArray) {
        // Deserialize with your bitcoin library
        // val psbt = Psbt.deserialize(psbtBytes)
        // val tx = psbt.extractTx()
        // broadcast(tx)
    }
}
```

### Example 3: UR Round-Trip Test

```kotlin
import com.gorunjinian.bcur.*

fun roundTripTest() {
    val originalData = ByteArray(5000) { it.toByte() }
    val ur = UR.fromBytes(originalData, "crypto-psbt")

    // Encode
    val encoder = UREncoder(ur, maxFragmentLen = 200)

    // Decode
    val decoder = URDecoder()
    do {
        decoder.receivePart(encoder.nextPart())
    } while (decoder.result == null)

    assert(decoder.result!!.type == ResultType.SUCCESS)
    val decoded = decoder.result!!.ur!!.toBytes()
    assert(decoded.contentEquals(originalData))
}
```

---

## Specifications

- [BCR-2020-005: Uniform Resources (UR)](https://github.com/BlockchainCommons/Research/blob/master/papers/bcr-2020-005-ur.md)
- [BCR-2020-006: UR Type Definition for PSBTs](https://github.com/BlockchainCommons/Research/blob/master/papers/bcr-2020-006-urtypes.md)
- [BCR-2020-012: Bytewords](https://github.com/BlockchainCommons/Research/blob/master/papers/bcr-2020-012-bytewords.md)
- [BCR-2024-001: Multipart UR (MUR)](https://github.com/BlockchainCommons/Research/blob/master/papers/bcr-2024-001-multipart-ur.md)
- [RFC 8949: CBOR](https://www.rfc-editor.org/rfc/rfc8949)

## References

- [sparrowwallet/hummingbird](https://github.com/sparrowwallet/hummingbird) (Java reference)
- [BlockchainCommons/URKit](https://github.com/BlockchainCommons/URKit) (Swift reference)

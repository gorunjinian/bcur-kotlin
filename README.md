# bcur-kotlin

Pure Kotlin Multiplatform implementation of [BC-UR (Uniform Resources)](https://github.com/BlockchainCommons/Research/blob/master/papers/bcr-2020-005-ur.md) with full encoder and decoder support, including fountain codes for animated QR transmission.

## Features

- **Pure Kotlin** - Zero external dependencies, fully KMP compatible
- **Cross-Platform** - Android, iOS, macOS, watchOS, JVM
- **Full Encode/Decode** - Both `UREncoder` and `URDecoder` for single and multi-part URs
- **Fountain Codes** - Rateless fountain coding for animated QR codes with error recovery
- **BCR-2020-005** - Uniform Resources specification
- **BCR-2020-012** - Bytewords encoding
- **Hummingbird Compatible** - Verified against [sparrowwallet/hummingbird](https://github.com/sparrowwallet/hummingbird) test vectors

## Installation

### Local Module

Add `bcur-kotlin` as a module in your project:

```kotlin
// settings.gradle.kts
include(":bcur-kotlin")
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":bcur-kotlin"))
}
```

### Maven Central

```kotlin
implementation("com.gorunjinian:bcur-kotlin:1.0.1")
```

## Usage

### Encode a PSBT as a Single QR Code

```kotlin
import com.gorunjinian.bcur.UR
import com.gorunjinian.bcur.UREncoder
import com.gorunjinian.bcur.Cbor

// Wrap raw PSBT bytes in CBOR byte string
val cborData = Cbor.wrapInByteString(psbtBytes)
val ur = UR("crypto-psbt", cborData)

// Encode to UR string for QR code display
val urString = UREncoder.encode(ur)
// "ur:crypto-psbt/hdosjojkidjyzmad..."
```

### Encode a Large PSBT as Animated QR Codes

```kotlin
val ur = UR("crypto-psbt", cborData)
val encoder = UREncoder(ur, maxFragmentLen = 150)

// Generate parts in a loop for animated QR display
while (true) {
    val part = encoder.nextPart()
    displayQrCode(part) // ur:crypto-psbt/1-10/lpadaxcs...
    delay(200) // ~5 fps
}
```

### Decode a Single-Part UR

```kotlin
import com.gorunjinian.bcur.UR
import com.gorunjinian.bcur.Cbor

val ur = UR.parse("ur:crypto-psbt/hdosjojkidjyzmad...")
val psbtBytes = Cbor.unwrapByteString(ur.cborData)
```

### Decode Multi-Part URs (Animated QR Scanning)

```kotlin
import com.gorunjinian.bcur.URDecoder
import com.gorunjinian.bcur.ResultType

val decoder = URDecoder()

// Feed scanned QR parts as they arrive
fun onQrScanned(content: String) {
    decoder.receivePart(content)

    decoder.result?.let { result ->
        if (result.type == ResultType.SUCCESS) {
            val ur = result.ur!!
            val psbtBytes = Cbor.unwrapByteString(ur.cborData)
            // Use psbtBytes
        }
    }
}
```

## Architecture

```
UR (data) --> Bytewords Encoding --> Single-Part UR String

UR (data) --> Fountain Encoder --> Part 1, Part 2, ... Part N
                                       |
                                  Bytewords Encoding
                                       |
                                  Multi-Part UR Strings
```

## Modules

| Module | Description |
|--------|-------------|
| `UR` | Core Uniform Resource data class |
| `UREncoder` | Single and multi-part UR encoding |
| `URDecoder` | Single and multi-part UR decoding (streaming) |
| `FountainEncoder` | Rateless fountain code encoder |
| `FountainDecoder` | Fountain code decoder with XOR reduction |
| `Bytewords` | BCR-2020-012 byte-to-word encoding |
| `Cbor` | Minimal CBOR encode/decode utilities |
| `CRC32` | Pure Kotlin CRC-32 checksum |
| `SHA256` | Pure Kotlin SHA-256 hash |
| `Xoshiro256StarStar` | Deterministic PRNG for fountain codes |

## Roadmap

- **CBOR schema parser for structured UR types** — The current `Cbor` module handles minimal CBOR encoding (byte strings, unsigned integers, arrays) sufficient for the UR transport layer. Full CBOR schema support for [BCR-2020-010 (crypto-output)](https://github.com/BlockchainCommons/Research/blob/master/papers/bcr-2020-010-output-desc.md) and [BCR-2020-015 (crypto-account)](https://github.com/BlockchainCommons/Research/blob/master/papers/bcr-2020-015-account.md) — including tagged CBOR descriptors (tag 308 for output descriptors, tag 303 for HD keys, tag 304 for key paths, etc.) — is planned and will be ported soon.

## Specifications

- [BCR-2020-005: Uniform Resources (UR)](https://github.com/BlockchainCommons/Research/blob/master/papers/bcr-2020-005-ur.md)
- [BCR-2020-012: Bytewords](https://github.com/BlockchainCommons/Research/blob/master/papers/bcr-2020-012-bytewords.md)
- [BCR-2024-001: Multipart UR (MUR)](https://github.com/BlockchainCommons/Research/blob/master/papers/bcr-2024-001-multipart-ur.md)

## References

Ported from and verified against:
- [sparrowwallet/hummingbird](https://github.com/sparrowwallet/hummingbird) (Java)
- [BlockchainCommons/URKit](https://github.com/BlockchainCommons/URKit) (Swift)

## License

MIT License

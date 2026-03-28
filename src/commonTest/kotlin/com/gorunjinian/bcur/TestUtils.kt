package com.gorunjinian.bcur

import com.gorunjinian.bcur.fountain.Xoshiro256StarStar

object TestUtils {
    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }

    fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
        }
        return data
    }

    fun makeMessage(len: Int, seed: String): ByteArray {
        val rng = Xoshiro256StarStar(seed)
        val message = ByteArray(len)
        rng.nextData(message)
        return message
    }

    fun makeMessageUR(len: Int, seed: String): UR {
        val message = makeMessage(len, seed)
        val cbor = Cbor.wrapInByteString(message)
        return UR("bytes", cbor)
    }
}

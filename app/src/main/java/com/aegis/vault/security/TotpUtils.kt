package com.aegis.vault.security

import org.apache.commons.codec.binary.Base32
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object TotpUtils {

    /**
     * Verilen Base32 secret'ten 6 haneli TOTP kodu üretir.
     */
    fun generateTOTP(secret: String): String {
        return try {
            val base32 = Base32()
            val decodedKey = base32.decode(secret.uppercase().replace(" ", ""))
            val timeStep = System.currentTimeMillis() / 1000 / 30
            
            val data = ByteBuffer.allocate(8).putLong(timeStep).array()
            val signKey = SecretKeySpec(decodedKey, "HmacSHA1")
            val mac = Mac.getInstance("HmacSHA1")
            mac.init(signKey)
            
            val hash = mac.doFinal(data)
            val offset = hash[hash.size - 1].toInt() and 0xf
            
            val binary = (hash[offset].toInt() and 0x7f shl 24) or
                    (hash[offset + 1].toInt() and 0xff shl 16) or
                    (hash[offset + 2].toInt() and 0xff shl 8) or
                    (hash[offset + 3].toInt() and 0xff)
            
            val otp = binary % 10.0.pow(6.0).toInt()
            otp.toString().padStart(6, '0')
        } catch (e: Exception) {
            "000000"
        }
    }

    /**
     * Mevcut 30 saniyelik periyodun bitmesine kalan süreyi saniye cinsinden döner.
     */
    fun getTimeRemaining(): Int {
        val seconds = (System.currentTimeMillis() / 1000) % 30
        return (30 - seconds).toInt()
    }
}

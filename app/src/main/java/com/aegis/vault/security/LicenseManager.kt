package com.aegis.vault.security

import android.util.Base64
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

object LicenseManager {

    /**
     * Güncellendi: ECDSA (Elliptic Curve) Public Key.
     * Bu anahtar, sizin private key'iniz ile üretilen lisansları doğrular.
     */
    private const val PUBLIC_KEY_BASE64 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE3F8VMn76p9146qWrhHhEEjRcZqTd4SShA7jt9lUKNT8Uig0RCQavP457h71HDAsu6I5CF/EerrSebutQCPqLVA=="

    /**
     * Lisans anahtarını kullanıcıya özel Device ID ile doğrular.
     * ECDSA P-256 (SHA256withECDSA) algoritması kullanılır.
     */
    fun verifyLicense(deviceId: String, licenseKeyBase64: String): Boolean {
        val sanitizedKey = licenseKeyBase64.replace("\\s".toRegex(), "")

        return try {
            val parts = sanitizedKey.split(".")
            if (parts.size != 2) return false

            val payloadBase64 = parts[0]
            val signatureBase64 = parts[1]

            val publicKey = getPublicKey()
            val signature = Signature.getInstance("SHA256withECDSA")
            
            var signatureBytes = Base64.decode(signatureBase64, Base64.NO_WRAP)
            if (signatureBytes.size == 64) {
                signatureBytes = rawToDer(signatureBytes)
            }

            // Lisans verisini (JSON) çöz ve doğrula
            val decodedPayloadBytes = Base64.decode(payloadBase64, Base64.NO_WRAP)
            val decodedPayloadStr = String(decodedPayloadBytes, Charsets.UTF_8)

            signature.initVerify(publicKey)
            signature.update(decodedPayloadBytes)
            val isSignatureValid = signature.verify(signatureBytes)

            // Hem imza geçerli olmalı hem de içindeki Device ID bu cihaza ait olmalı
            isSignatureValid && decodedPayloadStr.contains(deviceId, ignoreCase = true)
        } catch (e: Exception) {
            android.util.Log.e("LicenseManager", "Doğrulama hatası")
            false
        }
    }

    private fun getPublicKey(): PublicKey {
        val keyBytes = Base64.decode(PUBLIC_KEY_BASE64, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePublic(spec)
    }

    /**
     * Raw (r,s) imzasını DER formatına çevirir.
     */
    private fun rawToDer(raw: ByteArray): ByteArray {
        if (raw.size != 64) return raw
        return try {
            val r = java.math.BigInteger(1, raw.sliceArray(0 until 32))
            val s = java.math.BigInteger(1, raw.sliceArray(32 until 64))
            
            val v = org.bouncycastle.asn1.ASN1EncodableVector()
            v.add(org.bouncycastle.asn1.ASN1Integer(r))
            v.add(org.bouncycastle.asn1.ASN1Integer(s))
            org.bouncycastle.asn1.DERSequence(v).getEncoded()
        } catch (e: Exception) {
            raw
        }
    }
}

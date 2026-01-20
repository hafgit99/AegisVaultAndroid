import { argon2id } from 'hash-wasm';
import * as Keychain from 'react-native-keychain';
import { createCipheriv, createDecipheriv, randomBytes } from 'react-native-quick-crypto';

/**
 * Aegis Vault - Security Engine for React Native / Android
 * Uses react-native-quick-crypto for AES-256-GCM encryption
 * and Argon2id (hash-wasm) for key derivation.
 * Integrates with Android Keystore via react-native-keychain.
 */

export class CryptoService {
  private static ALGORITHM = 'aes-256-gcm';

  // SECURITY UPGRADE: Increased to 20 iterations for OWASP 2024+ compliance
  public static readonly DEFAULT_ITERATIONS = 20;
  public static readonly MINIMUM_ITERATIONS = 20;

  /**
   * Benchmarks the hardware to find an iteration count that takes ~500-1000ms.
   */
  static async benchmarkIterations(targetTimeMs: number = 600): Promise<number> {
    try {
      const startTime = Date.now();
      await argon2id({
        password: 'benchmark_test_password',
        salt: new Uint8Array(16),
        iterations: 1,
        memorySize: 65536,
        parallelism: 4,
        hashLength: 32,
        outputType: 'binary',
      });
      const endTime = Date.now();
      const singleRunTime = endTime - startTime;

      let calculated = Math.floor(targetTimeMs / singleRunTime);
      if (calculated < this.MINIMUM_ITERATIONS) calculated = this.MINIMUM_ITERATIONS;
      if (calculated > 60) calculated = 60;

      console.log(`[Security] Argon2id benchmark: ${calculated} iterations (${Math.round(calculated * singleRunTime)}ms unlock time)`);
      return calculated;
    } catch (e) {
      console.warn("Benchmark failed, falling back to safe default", e);
      return this.DEFAULT_ITERATIONS;
    }
  }

  static async deriveKeyWithRaw(password: string, salt: Uint8Array, iterations: number = this.DEFAULT_ITERATIONS): Promise<{ key: Buffer; raw: Uint8Array }> {
    try {
      const hash = await argon2id({
        password: password,
        salt: salt as any,
        iterations: iterations,
        memorySize: 65536,
        parallelism: 4,
        hashLength: 32,
        outputType: 'binary',
      });

      const key = Buffer.from(hash);
      return { key, raw: hash };
    } catch (err) {
      throw new Error("Anahtar türetme başarısız: " + (err as Error).message);
    }
  }

  static async deriveKeyFromPassword(password: string, salt: Uint8Array, iterations: number = this.DEFAULT_ITERATIONS): Promise<Buffer> {
    const result = await this.deriveKeyWithRaw(password, salt, iterations);
    try {
      result.raw.fill(0);
    } catch (e) { }
    return result.key;
  }

  static async encrypt(data: string, key: Buffer): Promise<{ ciphertext: Uint8Array; iv: Uint8Array; tag: Uint8Array }> {
    const encoder = new TextEncoder();
    const dataBytes = encoder.encode(data);
    const iv = randomBytes(12);

    try {
      const cipher = createCipheriv(this.ALGORITHM, key, iv);
      const encrypted = Buffer.concat([cipher.update(Buffer.from(dataBytes)), cipher.final()]);
      const tag = cipher.getAuthTag();

      // In GCM mode, the ciphertext and tag are combined
      // We need to extract just the ciphertext (without auth tag)
      const authTagLength = 16;
      const ciphertext = encrypted.slice(0, encrypted.length - authTagLength);

      return {
        ciphertext: new Uint8Array(ciphertext),
        iv: new Uint8Array(iv),
        tag: new Uint8Array(tag)
      };
    } catch (e) {
      console.error("Encryption failed", e);
      throw new Error("ENCRYPTION_FAILED");
    }
  }

  static async decrypt(ciphertext: Uint8Array, key: Buffer, iv: Uint8Array, tag: Uint8Array): Promise<string> {
    try {
      const decipher = createDecipheriv(this.ALGORITHM, key, Buffer.from(iv));
      decipher.setAuthTag(Buffer.from(tag));

      // Combine ciphertext for decryption
      const combined = Buffer.concat([Buffer.from(ciphertext), Buffer.from(tag)]);
      const decrypted = Buffer.concat([decipher.update(combined), decipher.final()]);

      const decoder = new TextDecoder();
      return decoder.decode(decrypted);
    } catch (e) {
      console.error("[CryptoService] Decryption failed:", e);
      throw new Error("OPERATION_FAILED");
    }
  }

  static async encryptBinary(data: Uint8Array, key: Buffer): Promise<{ ciphertext: Uint8Array; iv: Uint8Array; tag: Uint8Array }> {
    const iv = randomBytes(12);

    try {
      const cipher = createCipheriv(this.ALGORITHM, key, iv);
      const encrypted = Buffer.concat([cipher.update(Buffer.from(data)), cipher.final()]);
      const tag = cipher.getAuthTag();

      const authTagLength = 16;
      const ciphertext = encrypted.slice(0, encrypted.length - authTagLength);

      return {
        ciphertext: new Uint8Array(ciphertext),
        iv: new Uint8Array(iv),
        tag: new Uint8Array(tag)
      };
    } catch (e) {
      console.error("[CryptoService] Binary Encryption failed:", e);
      throw new Error("OPERATION_FAILED");
    }
  }

  static async decryptBinary(ciphertext: Uint8Array, key: Buffer, iv: Uint8Array, tag: Uint8Array): Promise<Uint8Array> {
    try {
      const decipher = createDecipheriv(this.ALGORITHM, key, Buffer.from(iv));
      decipher.setAuthTag(Buffer.from(tag));

      const combined = Buffer.concat([Buffer.from(ciphertext), Buffer.from(tag)]);
      const decrypted = Buffer.concat([decipher.update(combined), decipher.final()]);

      return new Uint8Array(decrypted);
    } catch (e) {
      console.error("[CryptoService] Binary Decryption failed:", e);
      throw new Error("OPERATION_FAILED");
    }
  }

  static arrayBufferToBase64(buffer: ArrayBuffer | Uint8Array | ArrayBufferLike): string {
    const bytes = buffer instanceof Uint8Array ? buffer : new Uint8Array(buffer as ArrayBuffer);
    return Buffer.from(bytes).toString('base64');
  }

  static base64ToArrayBuffer(base64: string): ArrayBuffer {
    const buf = Buffer.from(base64, 'base64');
    return buf.buffer.slice(buf.byteOffset, buf.byteOffset + buf.byteLength) as ArrayBuffer;
  }

  // === Android Keystore Integration ===

  /**
   * Securely store a secret in Android Keystore
   */
  static async storeSecureKey(key: string, value: string): Promise<boolean> {
    try {
      await Keychain.setInternetCredentials(key, 'user', value, {
        accessControl: Keychain.ACCESS_CONTROL.BIOMETRY_CURRENT_SET,
        accessGroup: 'com.aegisvault',
        securityLevel: Keychain.SECURITY_LEVEL.ANY,
        storage: Keychain.STORAGE_TYPE.AFTER_FIRST_UNLOCK,
      });
      return true;
    } catch (e) {
      console.error("[CryptoService] Keystore store error:", e);
      return false;
    }
  }

  /**
   * Retrieve a secret from Android Keystore
   */
  static async getSecureKey(key: string): Promise<string | null> {
    try {
      const credentials = await Keychain.getInternetCredentials(key);
      if (credentials && credentials.password) {
        return credentials.password;
      }
      return null;
    } catch (e) {
      console.error("[CryptoService] Keystore retrieve error:", e);
      return null;
    }
  }

  /**
   * Remove a secret from Android Keystore
   */
  static async removeSecureKey(key: string): Promise<boolean> {
    try {
      await Keychain.resetInternetCredentials(key);
      return true;
    } catch (e) {
      console.error("[CryptoService] Keystore remove error:", e);
      return false;
    }
  }

  /**
   * Check if device supports biometric authentication
   */
  static async isBiometricAvailable(): Promise<boolean> {
    try {
      return await Keychain.getBiometryType() !== null;
    } catch (e) {
      return false;
    }
  }
}

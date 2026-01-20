import ReactNativeBiometrics, { BiometryType } from 'react-native-biometrics';
import * as Keychain from 'react-native-keychain';
import { CryptoService } from './cryptoService';

/**
 * Aegis Vault - Biometric & Security Key Authentication Service for React Native
 * Uses react-native-biometrics for platform biometrics (fingerprint, face recognition)
 * and react-native-keychain for Android Keystore integration
 */

export interface BiometricConfig {
  deviceId: string;
  wrappedKey: string;
  wrapperIv: string;
  wrapperTag: string;
}

class BiometricService {
  private static rnBiometrics = new ReactNativeBiometrics({
    allowDeviceCredentials: true,
  });

  private static STORAGE_KEY = 'aegis_biometric_config';
  private static DEVICE_ID_KEY = 'aegis_device_id';

  /**
   * Check if biometric authentication is available on this device
   */
  static async isAvailable(): Promise<boolean> {
    try {
      const { available } = await this.rnBiometrics.isSensorAvailable();
      return available;
    } catch (e) {
      console.error('[BiometricService] Availability check error:', e);
      return false;
    }
  }

  /**
   * Get the type of biometric authentication available
   */
  static async getBiometryType(): Promise<BiometryType | null> {
    try {
      const { available, biometryType } = await this.rnBiometrics.isSensorAvailable();
      if (available) {
        return biometryType || null;
      }
      return null;
    } catch (e) {
      console.error('[BiometricService] Biometry type check error:', e);
      return null;
    }
  }

  /**
   * Check if biometric unlock is enabled
   */
  static isEnabled(): boolean {
    const configStr = Keychain.getGenericPassword({
      service: this.STORAGE_KEY,
    });
    return !!configStr;
  }

  /**
   * Enable biometric unlock for the vault
   * @param rawMasterKey - The raw master key bytes to wrap with biometric protection
   */
  static async enableBiometrics(rawMasterKey: Uint8Array): Promise<void> {
    const isAvailable = await this.isAvailable();
    if (!isAvailable) {
      throw new Error('BIOMETRIC_NOT_SUPPORTED');
    }

    try {
      // Generate a unique device ID for this biometric enrollment
      const deviceId = this.generateDeviceId();
      await Keychain.setGenericPassword(deviceId, 'device_id', {
        service: this.DEVICE_ID_KEY,
      });

      // Create a wrapper key derived from the biometric prompt result
      // The biometric prompt will be used to authenticate the user
      const { success } = await this.rnBiometrics.simplePrompt({
        promptMessage: 'Enable biometric unlock for Aegis Vault',
        cancelButtonText: 'Cancel',
      });

      if (!success) {
        throw new Error('BIOMETRIC_CANCELED');
      }

      // Generate a random wrapper secret
      const wrapperSecret = CryptoService.arrayBufferToBase64(
        Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15)
      );

      // Derive a wrapper key from the secret
      const wrapperKeyBuffer = Buffer.from(wrapperSecret, 'base64');

      // Encrypt the master key with the wrapper key
      const masterKeyB64 = CryptoService.arrayBufferToBase64(rawMasterKey);
      const { ciphertext, iv, tag } = await CryptoService.encrypt(
        masterKeyB64,
        wrapperKeyBuffer
      );

      // Store the wrapper secret in Android Keystore
      await CryptoService.storeSecureKey(
        `${this.STORAGE_KEY}_wrapper`,
        wrapperSecret
      );

      // Store the biometric config
      const config: BiometricConfig = {
        deviceId,
        wrappedKey: CryptoService.arrayBufferToBase64(ciphertext),
        wrapperIv: CryptoService.arrayBufferToBase64(iv),
        wrapperTag: CryptoService.arrayBufferToBase64(tag),
      };

      await Keychain.setGenericPassword(
        JSON.stringify(config),
        this.STORAGE_KEY
      );

      console.log('[BiometricService] Biometric unlock enabled successfully');
    } catch (e: any) {
      if (e.name === 'BiometricPromptError' || e.message === 'BIOMETRIC_CANCELED') {
        throw new Error('BIOMETRIC_CANCELED');
      }
      console.error('[BiometricService] Enable error:', e);
      throw e;
    }
  }

  /**
   * Unlock the vault using biometric authentication
   * @returns The master key (raw bytes) if biometric authentication succeeds
   */
  static async unlock(): Promise<{ key: Buffer; raw: Uint8Array } | null> {
    const configStr = await Keychain.getGenericPassword({ service: this.STORAGE_KEY });
    if (!configStr || typeof configStr !== 'string' && !configStr.password) {
      return null;
    }

    const configPassword = typeof configStr === 'string' ? configStr : configStr.password;
    const config: BiometricConfig = JSON.parse(configPassword);

    // Verify biometric authentication
    const { success } = await this.rnBiometrics.simplePrompt({
      promptMessage: 'Unlock Aegis Vault',
      cancelButtonText: 'Cancel',
    });

    if (!success) {
      return null;
    }

    try {
      // Retrieve the wrapper secret from Android Keystore
      const wrapperSecret = await CryptoService.getSecureKey(
        `${this.STORAGE_KEY}_wrapper`
      );

      if (!wrapperSecret) {
        console.error('[BiometricService] Wrapper secret not found in Keystore');
        return null;
      }

      const wrapperKey = Buffer.from(wrapperSecret, 'base64');

      // Decrypt the wrapped master key
      const wrappedData = CryptoService.base64ToArrayBuffer(config.wrappedKey);
      const iv = CryptoService.base64ToArrayBuffer(config.wrapperIv);
      const tag = CryptoService.base64ToArrayBuffer(config.wrapperTag);

      const decryptedRawKeyB64 = await CryptoService.decrypt(
        new Uint8Array(wrappedData),
        wrapperKey,
        new Uint8Array(iv),
        new Uint8Array(tag)
      );

      const rawKey = CryptoService.base64ToArrayBuffer(decryptedRawKeyB64);

      // Create a Buffer-based key (not CryptoKey like in desktop)
      const key = Buffer.from(rawKey);

      return { key, raw: new Uint8Array(rawKey) };
    } catch (e) {
      console.error('[BiometricService] Unlock error:', e);
      return null;
    }
  }

  /**
   * Verify user biometric authentication (for sensitive operations)
   */
  static async verifyUser(promptMessage: string = 'Authenticate to continue'): Promise<boolean> {
    const isAvailable = await this.isAvailable();
    if (!isAvailable) {
      return true; // Fallback if hardware not available
    }

    const configStr = await Keychain.getGenericPassword({ service: this.STORAGE_KEY });
    if (!configStr) {
      return true; // Biometrics not enabled by user
    }

    try {
      const { success } = await this.rnBiometrics.simplePrompt({
        promptMessage,
        cancelButtonText: 'Cancel',
      });
      return success;
    } catch (e) {
      console.error('[BiometricService] Verification error:', e);
      return false;
    }
  }

  /**
   * Disable biometric unlock
   */
  static async disable(): Promise<void> {
    try {
      // Remove the biometric config
      await Keychain.resetGenericPassword({ service: this.STORAGE_KEY });

      // Remove the wrapper secret from Keystore
      await CryptoService.removeSecureKey(`${this.STORAGE_KEY}_wrapper`);

      // Remove the device ID
      await Keychain.resetGenericPassword({ service: this.DEVICE_ID_KEY });

      console.log('[BiometricService] Biometric unlock disabled');
    } catch (e) {
      console.error('[BiometricService] Disable error:', e);
    }
  }

  /**
   * Check if the biometric setup is still valid (same device)
   */
  static async isValid(): Promise<boolean> {
    try {
      const configStr = await Keychain.getGenericPassword({ service: this.STORAGE_KEY });
      if (!configStr || typeof configStr !== 'string' && !configStr.password) {
        return false;
      }

      const configPassword = typeof configStr === 'string' ? configStr : configStr.password;
      const config: BiometricConfig = JSON.parse(configPassword);

      // Check if device ID matches
      const deviceIdCreds = await Keychain.getGenericPassword({ service: this.DEVICE_ID_KEY });
      if (!deviceIdCreds || typeof deviceIdCreds !== 'string' && !deviceIdCreds.password) {
        return false;
      }

      const storedDeviceId = typeof deviceIdCreds === 'string' ? deviceIdCreds : deviceIdCreds.password;
      return storedDeviceId === config.deviceId;
    } catch (e) {
      console.error('[BiometricService] Validation error:', e);
      return false;
    }
  }

  /**
   * Generate a unique device ID for this biometric enrollment
   */
  private static generateDeviceId(): string {
    const timestamp = Date.now().toString(36);
    const random = Math.random().toString(36).substring(2, 15);
    return `device_${timestamp}_${random}`;
  }

  /**
   * Get biometry type name for display
   */
  static async getBiometryTypeName(): Promise<string> {
    const biometryType = await this.getBiometryType();

    switch (biometryType) {
      case BiometryType.TouchID:
        return 'Touch ID';
      case BiometryType.FaceID:
        return 'Face ID';
      case BiometryType.Biometrics:
        return 'Biometric Authentication';
      default:
        return 'Biometric';
    }
  }
}

export { BiometricService };

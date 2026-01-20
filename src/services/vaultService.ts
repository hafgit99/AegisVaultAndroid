import { randomBytes } from 'react-native-quick-crypto';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { VaultEntry, SensitiveData, Category, Folder } from '../types';
import { CryptoService } from './cryptoService';
import { databaseService } from './databaseService';

const MASTER_METADATA_KEY = 'aegis_vault_metadata';
const MASTER_VERIFIER_KEY = 'aegis_vault_verifier';
const VALIDATOR_TEXT = 'AEGIS_VAULT_ACTIVE_SESSION_VALIDATOR';

/**
 * Generate a UUID v4
 */
function generateUUID(): string {
  const bytes = randomBytes(16);
  bytes[6] = (bytes[6]! & 0x0f) | 0x40; // Version 4
  bytes[8] = (bytes[8]! & 0x3f) | 0x80; // Variant 10

  const hex = Array.from(bytes)
    .map(b => b.toString(16).padStart(2, '0'))
    .join('');

  return [
    hex.slice(0, 8),
    hex.slice(8, 12),
    hex.slice(12, 16),
    hex.slice(16, 20),
    hex.slice(20, 32),
  ].join('-');
}

/**
 * Calculate password strength (simplified version for React Native)
 */
function calculateStrength(password: string): number {
  if (!password) return 0;

  let score = 0;

  // Length
  if (password.length >= 8) score += 20;
  if (password.length >= 12) score += 20;

  // Complexity
  if (/[a-z]/.test(password)) score += 15;
  if (/[A-Z]/.test(password)) score += 15;
  if (/[0-9]/.test(password)) score += 15;
  if (/[^a-zA-Z0-9]/.test(password)) score += 15;

  return Math.min(score, 100);
}

export class VaultService {
  /**
   * Check if vault is initialized
   */
  static async isInitialized(): Promise<boolean> {
    try {
      const metadata = await AsyncStorage.getItem(MASTER_METADATA_KEY);
      return !!metadata;
    } catch (e) {
      return false;
    }
  }

  /**
   * Get the salt from storage
   */
  static async getSalt(): Promise<Uint8Array> {
    const metadata = await AsyncStorage.getItem(MASTER_METADATA_KEY);
    if (!metadata) throw new Error('Vault not setup');

    try {
      const { salt: saltB64 } = JSON.parse(metadata);
      return new Uint8Array(CryptoService.base64ToArrayBuffer(saltB64));
    } catch (e) {
      throw new Error('Vault metadata corrupted');
    }
  }

  /**
   * Setup a new vault with the given password
   */
  static async setup(password: string): Promise<{ key: Buffer; raw: Uint8Array }> {
    const salt = randomBytes(16);
    const saltB64 = CryptoService.arrayBufferToBase64(salt);

    try {
      // Benchmark hardware for optimal security
      const iterations = await CryptoService.benchmarkIterations();
      const { key, raw } = await CryptoService.deriveKeyWithRaw(password, salt, iterations);

      // Create verifier
      const verifier = await this.createVerifier(key);
      const verifierBlob = {
        payload: CryptoService.arrayBufferToBase64(verifier.ciphertext),
        iv: CryptoService.arrayBufferToBase64(verifier.iv),
        tag: CryptoService.arrayBufferToBase64(verifier.tag),
      };

      // Store metadata
      const metadata = {
        salt: saltB64,
        iterations: iterations,
        version: 5,
        createdAt: Date.now(),
      };

      await AsyncStorage.setItem(MASTER_METADATA_KEY, JSON.stringify(metadata));
      await AsyncStorage.setItem(MASTER_VERIFIER_KEY, JSON.stringify(verifierBlob));

      // Initialize the database
      const masterKeyHex = Buffer.from(raw).toString('hex');
      await databaseService.init(masterKeyHex);

      return { key, raw };
    } catch (error) {
      console.error('Setup error:', error);
      throw error;
    }
  }

  /**
   * Derive master key from password
   */
  static async deriveMasterKey(password: string): Promise<{ key: Buffer; raw: Uint8Array; duress: boolean }> {
    const metadataStr = await AsyncStorage.getItem(MASTER_METADATA_KEY);
    if (!metadataStr) throw new Error('Vault not setup');

    let salt: Uint8Array;
    let iterations = CryptoService.DEFAULT_ITERATIONS;

    try {
      const metadata = JSON.parse(metadataStr);
      salt = new Uint8Array(CryptoService.base64ToArrayBuffer(metadata.salt));
      if (metadata.iterations) {
        iterations = metadata.iterations;
      }
    } catch (e) {
      throw new Error('Vault metadata corrupted');
    }

    const { key, raw } = await CryptoService.deriveKeyWithRaw(password, salt, iterations);

    // Try main verifier
    const verifierStr = await AsyncStorage.getItem(MASTER_VERIFIER_KEY);
    if (verifierStr) {
      try {
        const verifier = JSON.parse(verifierStr);
        const decrypted = await this.decryptWithMasterKey(key, verifier);

        if (decrypted === VALIDATOR_TEXT) {
          // Initialize database with master key
          const masterKeyHex = Buffer.from(raw).toString('hex');
          await databaseService.init(masterKeyHex);
          return { key, raw, duress: false };
        }
      } catch (e) {
        // Continue to error
      }
    }

    throw new Error('WRONG_PASSWORD');
  }

  private static async decryptWithMasterKey(key: Buffer, verifier: any): Promise<string> {
    const encryptedBuffer = new Uint8Array(CryptoService.base64ToArrayBuffer(verifier.payload));
    const tagBuffer = new Uint8Array(CryptoService.base64ToArrayBuffer(verifier.tag));
    const ivBuffer = new Uint8Array(CryptoService.base64ToArrayBuffer(verifier.iv));

    return await CryptoService.decrypt(encryptedBuffer, key, ivBuffer, tagBuffer);
  }

  private static async createVerifier(key: Buffer): Promise<{ ciphertext: Uint8Array; iv: Uint8Array; tag: Uint8Array }> {
    const encoder = new TextEncoder();
    const dataBytes = encoder.encode(VALIDATOR_TEXT);

    return await CryptoService.encryptBinary(dataBytes, key);
  }

  /**
   * Save an entry to the vault
   */
  static async saveEntry(
    plainEntry: Partial<VaultEntry> & { sensitive: SensitiveData; title?: string; username?: string },
    masterKey: Buffer
  ): Promise<VaultEntry> {
    const sensitiveCopy = { ...plainEntry.sensitive };
    let encryptedFile: Uint8Array | undefined;
    let fileIv: Uint8Array | undefined;
    let fileTag: Uint8Array | undefined;

    // Handle file attachments
    if (sensitiveCopy.fileBlob instanceof Uint8Array) {
      const fileResult = await CryptoService.encryptBinary(sensitiveCopy.fileBlob, masterKey);
      encryptedFile = fileResult.ciphertext;
      fileIv = fileResult.iv;
      fileTag = fileResult.tag;
      delete sensitiveCopy.fileBlob;
    }

    // Create full package for encryption
    const fullPackage = {
      title: plainEntry.title || (plainEntry.category === Category.FILE ? `Secure-Asset-${generateUUID().slice(0, 8)}` : 'Unnamed Entry'),
      username: plainEntry.username || '',
      category: plainEntry.category || Category.LOGIN,
      folderId: plainEntry.folderId,
      updatedAt: Date.now(),
      isFavorite: plainEntry.isFavorite,
      fileSize: plainEntry.fileSize,
      deletedAt: (plainEntry as any).deletedAt,
      sensitive: sensitiveCopy,
    };

    const packageJson = JSON.stringify(fullPackage);
    const { ciphertext, iv, tag } = await CryptoService.encrypt(packageJson, masterKey);

    const securityScore = plainEntry.category === Category.PASSKEY ? 100 : calculateStrength(plainEntry.sensitive.password || '');

    const entry: VaultEntry = {
      id: plainEntry.id || generateUUID(),
      encryptedTitle: new Uint8Array(0),
      titleIv: new Uint8Array(0),
      titleTag: new Uint8Array(0),
      encryptedUsername: new Uint8Array(0),
      usernameIv: new Uint8Array(0),
      usernameTag: new Uint8Array(0),
      encryptedMetadata: new Uint8Array(0),
      metadataIv: new Uint8Array(0),
      metadataTag: new Uint8Array(0),
      encryptedData: ciphertext,
      iv: iv,
      tag: tag,
      category: fullPackage.category,
      updatedAt: fullPackage.updatedAt,
      isFavorite: fullPackage.isFavorite || false,
      folderId: fullPackage.folderId,
      deletedAt: fullPackage.deletedAt,
      securityScore,
      fileSize: plainEntry.fileSize,
      encryptedFile,
      fileIv,
      fileTag,
    };

    // Save to database
    await databaseService.saveEntry({
      id: entry.id,
      category: entry.category,
      folderId: entry.folderId,
      payload: CryptoService.arrayBufferToBase64(entry.encryptedData),
      iv: CryptoService.arrayBufferToBase64(entry.iv),
      tag: CryptoService.arrayBufferToBase64(entry.tag),
      isFavorite: entry.isFavorite || false,
      updatedAt: entry.updatedAt,
    });

    return {
      ...entry,
      title: fullPackage.title,
      username: fullPackage.username,
    } as VaultEntry;
  }

  /**
   * Load all entries from the vault
   */
  static async loadAllEntries(): Promise<VaultEntry[]> {
    const rows = await databaseService.getAllEntries();

    return rows.map((row) => ({
      id: row.id,
      category: row.category as Category,
      folderId: row.folder_id,
      encryptedData: new Uint8Array(CryptoService.base64ToArrayBuffer(row.payload)),
      iv: new Uint8Array(CryptoService.base64ToArrayBuffer(row.iv)),
      tag: new Uint8Array(CryptoService.base64ToArrayBuffer(row.tag)),
      isFavorite: !!row.is_favorite,
      updatedAt: row.updated_at,
      // Empty arrays for compatibility
      encryptedTitle: new Uint8Array(0),
      titleIv: new Uint8Array(0),
      titleTag: new Uint8Array(0),
      encryptedUsername: new Uint8Array(0),
      usernameIv: new Uint8Array(0),
      usernameTag: new Uint8Array(0),
      encryptedMetadata: new Uint8Array(0),
      metadataIv: new Uint8Array(0),
      metadataTag: new Uint8Array(0),
    }));
  }

  /**
   * Decrypt entry data
   */
  static async decryptEntry(entry: VaultEntry, masterKey: Buffer): Promise<SensitiveData> {
    try {
      const decryptedJson = await CryptoService.decrypt(entry.encryptedData, masterKey, entry.iv, entry.tag);
      const parsed = JSON.parse(decryptedJson);

      // Handle Full Encryption Package (v4+)
      let sensitive: SensitiveData;
      if (parsed.sensitive) {
        sensitive = parsed.sensitive;
      } else {
        sensitive = parsed; // Legacy format
      }

      // Handle separate binary file
      if (entry.encryptedFile && entry.fileIv && entry.fileTag) {
        const decryptedFile = await CryptoService.decryptBinary(entry.encryptedFile, masterKey, entry.fileIv, entry.fileTag);
        sensitive.fileBlob = decryptedFile;
      }

      return sensitive;
    } catch (e) {
      console.error('Decryption Error:', e);
      throw new Error('Decryption failed');
    }
  }

  /**
   * Decrypt entry metadata
   */
  static async decryptEntryMetadata(entry: VaultEntry, masterKey: Buffer): Promise<{
    title: string;
    username: string;
    category?: Category;
    folderId?: string;
    updatedAt?: number;
    isFavorite?: boolean;
    deletedAt?: number;
    fileSize?: number;
  }> {
    try {
      // Handle Full Encryption Package (v4+)
      if (!entry.encryptedTitle || entry.encryptedTitle.length === 0) {
        const packageJson = await CryptoService.decrypt(entry.encryptedData, masterKey, entry.iv, entry.tag);
        const fullPackage = JSON.parse(packageJson);
        return {
          title: fullPackage.title || 'Unnamed Entry',
          username: fullPackage.username || '',
          category: fullPackage.category,
          folderId: fullPackage.folderId,
          updatedAt: fullPackage.updatedAt,
          isFavorite: fullPackage.isFavorite,
          deletedAt: fullPackage.deletedAt,
          fileSize: fullPackage.fileSize,
        };
      }

      // Legacy Decryption
      return {
        title: 'Unnamed Entry',
        username: '',
        category: entry.category,
        folderId: entry.folderId,
        updatedAt: entry.updatedAt,
        isFavorite: entry.isFavorite,
        deletedAt: entry.deletedAt,
        fileSize: entry.fileSize,
      };
    } catch (e) {
      console.error('Metadata Decryption Error for entry', entry.id, ':', e);
      return {
        title: '[Decryption Error]',
        username: '[Decryption Error]',
        category: entry.category,
        folderId: entry.folderId,
        updatedAt: entry.updatedAt,
        isFavorite: entry.isFavorite,
        deletedAt: entry.deletedAt,
        fileSize: entry.fileSize,
      };
    }
  }

  /**
   * Update entry metadata
   */
  static async updateEntryMetadata(
    id: string,
    changes: { isFavorite?: boolean; deletedAt?: number | undefined; folderId?: string | undefined },
    masterKey: Buffer
  ): Promise<void> {
    // For now, we need to load the entry, re-encrypt with new metadata, and save
    // This is a simplified implementation
    const entries = await this.loadAllEntries();
    const entry = entries.find(e => e.id === id);

    if (!entry) throw new Error('Entry not found');

    // Decrypt to get current data
    const currentMeta = await this.decryptEntryMetadata(entry, masterKey);
    const sensitive = await this.decryptEntry(entry, masterKey);

    // Create updated entry
    const updatedEntry: Partial<VaultEntry> & { sensitive: SensitiveData; title?: string; username?: string } = {
      id: entry.id,
      title: currentMeta.title,
      username: currentMeta.username,
      category: currentMeta.category,
      sensitive: sensitive,
      folderId: changes.folderId !== undefined ? changes.folderId : currentMeta.folderId,
      isFavorite: changes.isFavorite !== undefined ? changes.isFavorite : currentMeta.isFavorite,
      deletedAt: changes.deletedAt !== undefined ? changes.deletedAt : currentMeta.deletedAt,
      fileSize: currentMeta.fileSize,
    };

    await this.saveEntry(updatedEntry, masterKey);
  }

  /**
   * Delete an entry
   */
  static async deleteEntry(id: string): Promise<void> {
    await databaseService.deleteEntry(id);
  }

  /**
   * Bulk import entries
   */
  static async bulkImport(
    items: (Partial<VaultEntry> & { sensitive: SensitiveData; title?: string; username?: string })[],
    masterKey: Buffer
  ): Promise<void> {
    const encryptedEntries = await Promise.all(
      items.map(async (plainEntry) => {
        try {
          const displayTitle = plainEntry.title || 'Unnamed Entry';
          const displayUsername = plainEntry.username || '';

          const sensitiveCopy = { ...plainEntry.sensitive };
          let encryptedFile: Uint8Array | undefined;
          let fileIv: Uint8Array | undefined;
          let fileTag: Uint8Array | undefined;

          const fullPackage = {
            title: displayTitle,
            username: displayUsername,
            category: plainEntry.category || Category.LOGIN,
            folderId: plainEntry.folderId,
            updatedAt: Date.now(),
            isFavorite: plainEntry.isFavorite || false,
            fileSize: plainEntry.fileSize || 0,
            deletedAt: (plainEntry as any).deletedAt,
            sensitive: sensitiveCopy,
          };

          const packageJson = JSON.stringify(fullPackage);
          const { ciphertext, iv, tag } = await CryptoService.encrypt(packageJson, masterKey);

          return {
            id: plainEntry.id || generateUUID(),
            category: fullPackage.category,
            folderId: fullPackage.folderId,
            payload: CryptoService.arrayBufferToBase64(ciphertext),
            iv: CryptoService.arrayBufferToBase64(iv),
            tag: CryptoService.arrayBufferToBase64(tag),
            isFavorite: fullPackage.isFavorite ? 1 : 0,
            updatedAt: fullPackage.updatedAt,
          };
        } catch (e) {
          console.error('Bulk Import: Failed to process entry', e);
          return null;
        }
      })
    );

    const validEntries = encryptedEntries.filter((e): e is NonNullable<typeof e> => e !== null);

    if (validEntries.length > 0) {
      await databaseService.bulkSaveEntries(validEntries);
    }
  }

  /**
   * Deduplicate vault entries
   */
  static async deduplicateVault(masterKey: Buffer): Promise<{ deletedCount: number }> {
    try {
      const allEntries = await this.loadAllEntries();
      const entryMap = new Map<string, VaultEntry>();
      const toDelete: string[] = [];

      const norm = (s: string) =>
        (s || '')
          .toLowerCase()
          .trim()
          .normalize('NFD')
          .replace(/[\u0300-\u036f]/g, '')
          .replace(/[\u200B-\u200D\uFEFF]/g, '');

      for (const entry of allEntries) {
        try {
          const meta = await this.decryptEntryMetadata(entry, masterKey);
          const key = `${norm(meta.title)}|${norm(meta.username)}|${meta.category}`;

          const existing = entryMap.get(key);
          if (existing) {
            const existingMeta = await this.decryptEntryMetadata(existing, masterKey);

            const keepNew =
              (meta.isFavorite && !existingMeta.isFavorite) ||
              ((meta.updatedAt || 0) > (existingMeta.updatedAt || 0) && meta.isFavorite === existingMeta.isFavorite);

            if (keepNew) {
              toDelete.push(existing.id);
              entryMap.set(key, entry);
            } else {
              toDelete.push(entry.id);
            }
          } else {
            entryMap.set(key, entry);
          }
        } catch (e) {
          console.error('Deduplication error', e);
        }
      }

      if (toDelete.length > 0) {
        console.log(`[Deduplication] ${toDelete.length} duplicates found. Cleaning up...`);
        for (const id of toDelete) {
          await databaseService.deleteEntry(id);
        }
        console.log(`[Deduplication] Cleaned ${toDelete.length} entries.`);
      }

      return { deletedCount: toDelete.length };
    } catch (e) {
      console.error('Deduplication failed', e);
      throw e;
    }
  }

  /**
   * Clear all vault data
   */
  static async clearVault(): Promise<void> {
    await databaseService.clear();
    await AsyncStorage.multiRemove([MASTER_METADATA_KEY, MASTER_VERIFIER_KEY]);
  }
}

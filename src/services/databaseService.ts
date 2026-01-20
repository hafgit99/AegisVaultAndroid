import { openDatabase, SQLiteDatabase } from 'react-native-quick-sqlite';

/**
 * Aegis Vault - Database Service for React Native / Android
 * Uses react-native-quick-sqlite with SQLCipher encryption
 */

export interface DatabaseEntry {
  id: string;
  category: string;
  folder_id?: string;
  payload: string; // Base64 encoded
  iv: string;
  tag: string;
  is_favorite: number;
  updated_at: number;
}

export interface DatabaseFolder {
  id: string;
  name: string;
  updated_at: number;
}

export interface DatabaseConfig {
  key: string;
  value: string;
}

class DatabaseService {
  private db: SQLiteDatabase | null = null;
  private dbName = 'vault.db';
  private dbVersion = '1.0';

  /**
   * Initialize the encrypted SQLite database
   * @param masterKeyHex - Hex-encoded master key for SQLCipher encryption
   */
  async init(masterKeyHex: string): Promise<void> {
    if (this.db) return;

    try {
      // Open database with SQLCipher encryption
      this.db = openDatabase({
        name: this.dbName,
        location: 'default',
        key: masterKeyHex, // SQLCipher encryption key
      });

      // Enable WAL mode for better concurrency
      await this.db.execute('PRAGMA journal_mode = WAL');
      await this.db.execute('PRAGMA synchronous = NORMAL');

      // Initialize tables
      await this.createTables();

      console.log('[Database] SQLite/SQLCipher initialized at', this.dbName);
    } catch (e) {
      console.error('[Database] Initialization error:', e);
      throw new Error('DATABASE_INIT_FAILED');
    }
  }

  private async createTables(): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    await this.db.execute(`
      CREATE TABLE IF NOT EXISTS entries (
        id TEXT PRIMARY KEY,
        category TEXT,
        folder_id TEXT,
        payload TEXT,
        iv TEXT,
        tag TEXT,
        is_favorite INTEGER DEFAULT 0,
        updated_at INTEGER
      );
    `);

    await this.db.execute(`
      CREATE TABLE IF NOT EXISTS folders (
        id TEXT PRIMARY KEY,
        name TEXT,
        updated_at INTEGER
      );
    `);

    await this.db.execute(`
      CREATE TABLE IF NOT EXISTS config (
        key TEXT PRIMARY KEY,
        value TEXT
      );
    `);

    // Create indexes for better query performance
    await this.db.execute('CREATE INDEX IF NOT EXISTS idx_entries_category ON entries(category);');
    await this.db.execute('CREATE INDEX IF NOT EXISTS idx_entries_folder_id ON entries(folder_id);');
    await this.db.execute('CREATE INDEX IF NOT EXISTS idx_entries_updated_at ON entries(updated_at);');
  }

  // === Entries ===

  async saveEntry(entry: {
    id: string;
    category: string;
    folderId?: string;
    payload: string; // Base64 encoded
    iv: string;
    tag: string;
    isFavorite?: boolean;
    updatedAt?: number;
  }): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    const params = [
      entry.id,
      entry.category,
      entry.folderId || null,
      entry.payload,
      entry.iv,
      entry.tag,
      entry.isFavorite ? 1 : 0,
      entry.updatedAt || Date.now(),
    ];

    await this.db.execute(
      `INSERT OR REPLACE INTO entries (id, category, folder_id, payload, iv, tag, is_favorite, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      params
    );
  }

  async deleteEntry(id: string): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    await this.db.execute('DELETE FROM entries WHERE id = ?', [id]);
  }

  async getEntry(id: string): Promise<DatabaseEntry | null> {
    if (!this.db) throw new Error('Database not initialized');

    const results = await this.db.execute('SELECT * FROM entries WHERE id = ?', [id]);
    if (results.rows.length === 0) return null;

    const row = results.rows[0];
    return {
      id: row.id,
      category: row.category,
      folder_id: row.folder_id,
      payload: row.payload,
      iv: row.iv,
      tag: row.tag,
      is_favorite: row.is_favorite,
      updated_at: row.updated_at,
    };
  }

  async getAllEntries(): Promise<DatabaseEntry[]> {
    if (!this.db) throw new Error('Database not initialized');

    const results = await this.db.execute('SELECT * FROM entries ORDER BY updated_at DESC');

    return results.rows.map(row => ({
      id: row.id,
      category: row.category,
      folder_id: row.folder_id,
      payload: row.payload,
      iv: row.iv,
      tag: row.tag,
      is_favorite: row.is_favorite,
      updated_at: row.updated_at,
    }));
  }

  async bulkSaveEntries(entries: Array<{
    id: string;
    category: string;
    folderId?: string;
    payload: string;
    iv: string;
    tag: string;
    isFavorite?: boolean;
    updatedAt?: number;
  }>): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    // Use transaction for bulk operations
    await this.db.transaction(async (tx) => {
      for (const entry of entries) {
        await tx.execute(
          `INSERT OR REPLACE INTO entries (id, category, folder_id, payload, iv, tag, is_favorite, updated_at)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
          [
            entry.id,
            entry.category,
            entry.folderId || null,
            entry.payload,
            entry.iv,
            entry.tag,
            entry.isFavorite ? 1 : 0,
            entry.updatedAt || Date.now(),
          ]
        );
      }
    });
  }

  // === Folders ===

  async saveFolder(folder: {
    id: string;
    name: string;
    updatedAt?: number;
  }): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    await this.db.execute(
      `INSERT OR REPLACE INTO folders (id, name, updated_at) VALUES (?, ?, ?)`,
      [folder.id, folder.name, folder.updatedAt || Date.now()]
    );
  }

  async deleteFolder(id: string): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    await this.db.execute('DELETE FROM folders WHERE id = ?', [id]);
  }

  async getAllFolders(): Promise<DatabaseFolder[]> {
    if (!this.db) throw new Error('Database not initialized');

    const results = await this.db.execute('SELECT * FROM folders ORDER BY name ASC');

    return results.rows.map(row => ({
      id: row.id,
      name: row.name,
      updated_at: row.updated_at,
    }));
  }

  // === Config ===

  async setConfig(key: string, value: string): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    await this.db.execute(
      'INSERT OR REPLACE INTO config (key, value) VALUES (?, ?)',
      [key, value]
    );
  }

  async getConfig(key: string): Promise<string | null> {
    if (!this.db) throw new Error('Database not initialized');

    const results = await this.db.execute('SELECT value FROM config WHERE key = ?', [key]);
    if (results.rows.length === 0) return null;

    return results.rows[0].value;
  }

  async deleteConfig(key: string): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    await this.db.execute('DELETE FROM config WHERE key = ?', [key]);
  }

  // === Database Management ===

  async close(): Promise<void> {
    if (this.db) {
      await this.db.close();
      this.db = null;
    }
  }

  async clear(): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    await this.db.execute('DELETE FROM entries');
    await this.db.execute('DELETE FROM folders');
    await this.db.execute('DELETE FROM config');
  }

  async exportDatabase(): Promise<string> {
    if (!this.db) throw new Error('Database not initialized');

    // Get all data
    const entries = await this.getAllEntries();
    const folders = await this.getAllFolders();
    const configResults = await this.db.execute('SELECT * FROM config');
    const configs = configResults.rows.map(row => ({
      key: row.key,
      value: row.value,
    }));

    return JSON.stringify({
      version: this.dbVersion,
      exportedAt: Date.now(),
      entries,
      folders,
      configs,
    });
  }

  async importDatabase(backupData: string, masterKeyHex: string): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    try {
      const data = JSON.parse(backupData);

      if (!data.version || !data.entries || !data.folders) {
        throw new Error('INVALID_BACKUP_FORMAT');
      }

      // Clear existing data
      await this.clear();

      // Import entries
      if (data.entries.length > 0) {
        await this.bulkSaveEntries(data.entries);
      }

      // Import folders
      for (const folder of data.folders) {
        await this.saveFolder(folder);
      }

      // Import configs
      for (const config of data.configs || []) {
        await this.setConfig(config.key, config.value);
      }

      console.log('[Database] Import completed successfully');
    } catch (e) {
      console.error('[Database] Import error:', e);
      throw new Error('DATABASE_IMPORT_FAILED');
    }
  }

  isInitialized(): boolean {
    return this.db !== null;
  }
}

export const databaseService = new DatabaseService();

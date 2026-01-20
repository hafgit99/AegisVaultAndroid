import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { VaultEntry, SensitiveData, Folder } from '../types';
import { VaultService } from '../services/vaultService';
import { useAuth } from './AuthContext';

interface VaultContextType {
  entries: VaultEntry[];
  folders: (Folder & { name: string })[];
  loadEntries: () => Promise<void>;
  saveEntry: (plain: Partial<VaultEntry> & { sensitive: SensitiveData }) => Promise<void>;
  deleteEntry: (id: string) => Promise<void>;
  restoreEntry: (id: string) => Promise<void>;
  permanentDelete: (id: string) => Promise<void>;
  decryptData: (entry: VaultEntry) => Promise<SensitiveData>;
  toggleFavorite: (id: string) => Promise<void>;
  createFolder: (name: string, color: string, icon: string, parentId?: string) => Promise<void>;
  unlock: (password: string) => Promise<void>;
  setup: (password: string) => Promise<void>;
  resetVault: () => Promise<void>;
  lock: () => Promise<void>;
  deduplicateVault: () => Promise<{ deletedCount: number }>;
  isInitialized: boolean;
  isLoading: boolean;
}

const VaultContext = createContext<VaultContextType | undefined>(undefined);

export const VaultProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [entries, setEntries] = useState<VaultEntry[]>([]);
  const [folders, setFolders] = useState<(Folder & { name: string })[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const { masterKey, setKey, logout } = useAuth();

  const handleLock = useCallback(async () => {
    // Clear data
    setEntries([]);
    setFolders([]);

    // Logout from auth context
    await logout();
  }, [logout]);

  const loadEntries = useCallback(async () => {
    setIsLoading(true);

    if (masterKey) {
      try {
        const data = await VaultService.loadAllEntries();

        if (masterKey) {
          const decryptedData = await Promise.all(
            data.map(async (entry) => {
              try {
                const metadata = await VaultService.decryptEntryMetadata(entry, masterKey);
                return {
                  ...entry,
                  title: metadata.title,
                  username: metadata.username,
                  category: metadata.category || entry.category,
                  folderId: metadata.folderId,
                  isFavorite: metadata.isFavorite ?? entry.isFavorite,
                  deletedAt: metadata.deletedAt,
                  fileSize: metadata.fileSize ?? entry.fileSize,
                };
              } catch (e) {
                console.error('[VaultContext] Failed to decrypt entry metadata:', entry.id, e);
                return {
                  ...entry,
                  title: '[Decryption Error]',
                  username: '[Decryption Error]',
                  category: entry.category,
                  folderId: undefined,
                  isFavorite: false,
                  deletedAt: undefined,
                  fileSize: entry.fileSize,
                };
              }
            })
          );

          console.log(`[VaultContext] Successfully decrypted ${decryptedData.length} entries`);
          setEntries(decryptedData.sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0)));

          // TODO: Load folders when folder service is implemented
          setFolders([]);
        } else {
          setEntries(data.sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0)));
        }
      } catch (e) {
        console.error('[VaultContext] Failed to load entries:', e);
        setEntries([]);
      }
    }

    setIsLoading(false);
  }, [masterKey]);

  useEffect(() => {
    if (masterKey) {
      loadEntries();
    }
  }, [masterKey, loadEntries]);

  const unlock = useCallback(async (password: string) => {
    const { key, raw } = await VaultService.deriveMasterKey(password);
    await setKey(key, raw);
  }, [setKey]);

  const setup = useCallback(async (password: string) => {
    const { key, raw } = await VaultService.setup(password);
    await setKey(key, raw);
    setEntries([]);
    setFolders([]);
  }, [setKey]);

  const saveEntry = useCallback(async (plain: Partial<VaultEntry> & { sensitive: SensitiveData }) => {
    if (!masterKey) throw new Error('Vault locked');
    await VaultService.saveEntry(plain, masterKey);
    await loadEntries();
  }, [masterKey, loadEntries]);

  const deleteEntry = useCallback(async (id: string) => {
    if (!masterKey) throw new Error('Vault locked');
    await VaultService.updateEntryMetadata(id, { deletedAt: Date.now() }, masterKey);
    await loadEntries();
  }, [masterKey, loadEntries]);

  const restoreEntry = useCallback(async (id: string) => {
    if (!masterKey) throw new Error('Vault locked');
    await VaultService.updateEntryMetadata(id, { deletedAt: undefined }, masterKey);
    await loadEntries();
  }, [masterKey, loadEntries]);

  const permanentDelete = useCallback(async (id: string) => {
    await VaultService.deleteEntry(id);
    await loadEntries();
  }, [loadEntries]);

  const decryptData = useCallback(async (entry: VaultEntry) => {
    if (!masterKey) throw new Error('Vault locked');
    return await VaultService.decryptEntry(entry, masterKey);
  }, [masterKey]);

  const toggleFavorite = useCallback(async (id: string) => {
    if (!masterKey) throw new Error('Vault locked');
    const entry = entries.find(e => e.id === id);
    if (!entry) return;
    await VaultService.updateEntryMetadata(id, { isFavorite: !entry.isFavorite }, masterKey);
    await loadEntries();
  }, [entries, masterKey, loadEntries]);

  const createFolder = useCallback(async (name: string, color: string, icon: string, parentId?: string) => {
    if (!masterKey) return;
    // TODO: Implement folder creation when folder service is ready
    await loadEntries();
  }, [masterKey, loadEntries]);

  const resetVault = async () => {
    await VaultService.clearVault();
    await loadEntries();
  };

  const deduplicateVault = useCallback(async () => {
    if (!masterKey) throw new Error('Vault locked');
    const result = await VaultService.deduplicateVault(masterKey);
    await loadEntries();
    return result;
  }, [masterKey, loadEntries]);

  const checkInitialized = useCallback(async () => {
    return await VaultService.isInitialized();
  }, []);

  const [isInitialized, setIsInitialized] = React.useState(false);

  useEffect(() => {
    checkInitialized().then(setIsInitialized);
  }, [checkInitialized]);

  return (
    <VaultContext.Provider
      value={{
        entries,
        folders,
        loadEntries,
        saveEntry,
        deleteEntry,
        restoreEntry,
        permanentDelete,
        decryptData,
        toggleFavorite,
        createFolder,
        unlock,
        setup,
        resetVault,
        lock: handleLock,
        deduplicateVault,
        isLoading,
        isInitialized,
      }}
    >
      {children}
    </VaultContext.Provider>
  );
};

export const useVault = () => {
  const context = useContext(VaultContext);
  if (!context) throw new Error('useVault must be used within VaultProvider');
  return context;
};

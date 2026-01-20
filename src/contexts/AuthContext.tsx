import React, { createContext, useContext, useState, useCallback, useRef } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';

/**
 * Aegis Vault - Authentication Context for React Native
 * Manages master key state and authentication status
 */

interface AuthContextType {
  masterKey: Buffer | null;
  setKey: (key: Buffer | null, rawKey?: Uint8Array) => Promise<void>;
  isAuthenticated: boolean;
  logout: () => Promise<void>;
  deriving: boolean;
  setDeriving: (val: boolean) => void;
  isVerifying2FA: boolean;
  setVerifying2FA: (val: boolean) => void;
  tempMasterKey: Buffer | null;
  setTempMasterKey: (key: Buffer | null, rawKey?: Uint8Array) => void;
  finalize2FA: () => Promise<void>;
  withMasterKeyRaw: <T>(callback: (raw: Uint8Array) => Promise<T>) => Promise<T>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const STORAGE_KEYS = {
  SALT: 'aegis_master_salt',
  ITERATIONS: 'aegis_iterations',
  VERIFIER: 'aegis_verifier',
};

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [masterKey, setMasterKeyState] = useState<Buffer | null>(null);
  const [tempMasterKey, setTempMasterKeyState] = useState<Buffer | null>(null);
  const [tempRawKey, setTempRawKey] = useState<Uint8Array | undefined>(undefined);
  const [deriving, setDeriving] = useState(false);
  const [isVerifying2FA, setVerifying2FA] = useState(false);

  // SECURITY: Raw key is kept in ref, not state, to prevent React DevTools exposure
  const rawMasterKeyRef = useRef<Uint8Array | null>(null);

  const setTempMasterKey = (key: Buffer | null, rawKey?: Uint8Array) => {
    setTempMasterKeyState(key);
    if (key) {
      if (rawKey) setTempRawKey(rawKey);
    } else {
      setTempRawKey(undefined);
    }
  };

  const finalize2FA = async () => {
    if (tempMasterKey && tempRawKey) {
      await setKey(tempMasterKey, tempRawKey);
      // Clean up temp state
      setTempMasterKeyState(null);
      setTempRawKey(undefined);
      setVerifying2FA(false);
    } else {
      throw new Error('2FA_FINALIZE_NO_TEMP_KEY');
    }
  };

  const setKey = async (key: Buffer | null, rawKey?: Uint8Array) => {
    setMasterKeyState(key);

    if (key && rawKey) {
      // Store in ref (heap) but protected by closure access
      rawMasterKeyRef.current = rawKey;

      // For React Native, we can store a verification token in AsyncStorage
      // This is used to verify the key is still valid on app restart
      try {
        const verifier = generateVerifier(rawKey);
        await AsyncStorage.setItem(STORAGE_KEYS.VERIFIER, verifier);
      } catch (e) {
        console.error('[AuthContext] Failed to store verifier:', e);
      }
    } else if (!key) {
      // Clear
      if (rawMasterKeyRef.current) {
        rawMasterKeyRef.current.fill(0);
        rawMasterKeyRef.current = null;
      }
      try {
        await AsyncStorage.removeItem(STORAGE_KEYS.VERIFIER);
      } catch (e) {
        console.error('[AuthContext] Failed to clear verifier:', e);
      }
    }
  };

  // SECURITY: Accessor for raw key operations
  const withMasterKeyRaw = async <T,>(callback: (raw: Uint8Array) => Promise<T>): Promise<T> => {
    if (!rawMasterKeyRef.current) {
      throw new Error('MASTER_KEY_NOT_AVAILABLE');
    }
    return callback(rawMasterKeyRef.current);
  };

  const logout = useCallback(async () => {
    // Clear states
    setMasterKeyState(null);
    setTempMasterKeyState(null);
    setTempRawKey(undefined);
    setDeriving(false);
    setVerifying2FA(false);

    // Secure wipe
    if (rawMasterKeyRef.current) {
      rawMasterKeyRef.current.fill(0);
      rawMasterKeyRef.current = null;
    }

    // Clear verifier from AsyncStorage
    try {
      await AsyncStorage.removeItem(STORAGE_KEYS.VERIFIER);
    } catch (e) {
      console.error('[AuthContext] Failed to clear verifier on logout:', e);
    }
  }, []);

  const isAuthenticated = !!masterKey;

  return (
    <AuthContext.Provider
      value={{
        masterKey,
        setKey,
        isAuthenticated,
        logout,
        deriving,
        setDeriving,
        isVerifying2FA,
        setVerifying2FA,
        tempMasterKey,
        setTempMasterKey,
        finalize2FA,
        withMasterKeyRaw,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
};

/**
 * Generate a simple verifier from the raw key
 * This is used to verify the key is still valid on app restart
 */
function generateVerifier(rawKey: Uint8Array): string {
  // Simple hash of first few bytes
  let hash = 0;
  const len = Math.min(rawKey.length, 32);
  for (let i = 0; i < len; i++) {
    const char = rawKey[i];
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash; // Convert to 32bit integer
  }
  return hash.toString(16);
}

/**
 * Storage helpers for salt and iterations
 */
export const AuthStorage = {
  async saveSalt(salt: Uint8Array): Promise<void> {
    const saltB64 = Buffer.from(salt).toString('base64');
    await AsyncStorage.setItem(STORAGE_KEYS.SALT, saltB64);
  },

  async getSalt(): Promise<Uint8Array | null> {
    try {
      const saltB64 = await AsyncStorage.getItem(STORAGE_KEYS.SALT);
      if (!saltB64) return null;
      return Buffer.from(saltB64, 'base64');
    } catch (e) {
      return null;
    }
  },

  async saveIterations(iterations: number): Promise<void> {
    await AsyncStorage.setItem(STORAGE_KEYS.ITERATIONS, iterations.toString());
  },

  async getIterations(): Promise<number> {
    try {
      const iterStr = await AsyncStorage.getItem(STORAGE_KEYS.ITERATIONS);
      return iterStr ? parseInt(iterStr, 10) : 20; // Default 20 iterations
    } catch (e) {
      return 20;
    }
  },

  async clear(): Promise<void> {
    await AsyncStorage.multiRemove([
      STORAGE_KEYS.SALT,
      STORAGE_KEYS.ITERATIONS,
      STORAGE_KEYS.VERIFIER,
    ]);
  },
};

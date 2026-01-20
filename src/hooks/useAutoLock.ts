import { useEffect, useRef } from 'react';
import { AppState, AppStateStatus } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useAuth } from '../contexts/AuthContext';

const AUTO_LOCK_KEY = 'aegis_auto_lock_minutes';
const DEFAULT_AUTO_LOCK = 15; // minutes

/**
 * Hook to automatically lock the vault after inactivity
 * or when the app goes to background
 */
export function useAutoLock() {
  const { logout, isAuthenticated } = useAuth();
  const appState = useRef(AppState.currentState);
  const backgroundTimeRef = useRef<number>(0);

  useEffect(() => {
    const subscription = AppState.addEventListener('change', handleAppStateChange);

    // Setup inactivity timer
    const inactivityTimer = setInterval(checkInactivity, 60000); // Check every minute

    return () => {
      subscription.remove();
      clearInterval(inactivityTimer);
    };
  }, [isAuthenticated]);

  const handleAppStateChange = async (nextAppState: AppStateStatus) => {
    if (appState.current.match(/inactive|background/) && nextAppState === 'active') {
      // App coming from background to foreground
      const backgroundTime = Date.now() - backgroundTimeRef.current;
      const autoLockMinutes = await getAutoLockMinutes();
      const autoLockMs = autoLockMinutes * 60 * 1000;

      if (backgroundTime > autoLockMs && isAuthenticated) {
        await logout();
      }
    } else if (nextAppState.match(/inactive|background/)) {
      // App going to background
      backgroundTimeRef.current = Date.now();
    }

    appState.current = nextAppState;
  };

  const checkInactivity = async () => {
    // Check for inactivity (user interaction)
    // This would need to be integrated with touch tracking
    // For now, we rely on app state changes
  };

  const getAutoLockMinutes = async (): Promise<number> => {
    try {
      const minutes = await AsyncStorage.getItem(AUTO_LOCK_KEY);
      return minutes ? parseInt(minutes, 10) : DEFAULT_AUTO_LOCK;
    } catch (e) {
      return DEFAULT_AUTO_LOCK;
    }
  };

  return {
    setAutoLock: async (minutes: number) => {
      await AsyncStorage.setItem(AUTO_LOCK_KEY, minutes.toString());
    },
    getAutoLock: getAutoLockMinutes,
  };
}

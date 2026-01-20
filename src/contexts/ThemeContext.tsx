import React, { createContext, useContext, useState, useEffect } from 'react';
import { useColorScheme } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

type Theme = 'dark' | 'light';
type Colors = {
  background: string;
  foreground: string;
  card: string;
  cardForeground: string;
  primary: string;
  primaryForeground: string;
  secondary: string;
  secondaryForeground: string;
  muted: string;
  mutedForeground: string;
  accent: string;
  accentForeground: string;
  destructive: string;
  destructiveForeground: string;
  border: string;
  input: string;
  ring: string;
};

const lightColors: Colors = {
  background: '#ffffff',
  foreground: '#0a0a0a',
  card: '#ffffff',
  cardForeground: '#0a0a0a',
  primary: '#3b82f6',
  primaryForeground: '#ffffff',
  secondary: '#f1f5f9',
  secondaryForeground: '#0f172a',
  muted: '#f1f5f9',
  mutedForeground: '#64748b',
  accent: '#3b82f6',
  accentForeground: '#ffffff',
  destructive: '#ef4444',
  destructiveForeground: '#ffffff',
  border: '#e2e8f0',
  input: '#e2e8f0',
  ring: '#3b82f6',
};

const darkColors: Colors = {
  background: '#0a0a0a',
  foreground: '#fafafa',
  card: '#1a1a1a',
  cardForeground: '#fafafa',
  primary: '#3b82f6',
  primaryForeground: '#ffffff',
  secondary: '#1e293b',
  secondaryForeground: '#f8fafc',
  muted: '#1e293b',
  mutedForeground: '#94a3b8',
  accent: '#3b82f6',
  accentForeground: '#ffffff',
  destructive: '#ef4444',
  destructiveForeground: '#ffffff',
  border: '#27272a',
  input: '#27272a',
  ring: '#3b82f6',
};

interface ThemeContextType {
  theme: Theme;
  colors: Colors;
  setTheme: (t: Theme) => void;
  toggleTheme: () => void;
  isSystem: boolean;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

const THEME_STORAGE_KEY = 'aegis_theme';

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const systemColorScheme = useColorScheme();
  const [theme, setThemeState] = useState<Theme>(() => {
    // Default to system theme
    return systemColorScheme === 'light' ? 'light' : 'dark';
  });
  const [isSystem, setIsSystem] = useState(true);

  // Load saved theme on mount
  useEffect(() => {
    AsyncStorage.getItem(THEME_STORAGE_KEY).then((saved) => {
      if (saved) {
        if (saved === 'system') {
          setIsSystem(true);
          setThemeState(systemColorScheme === 'light' ? 'light' : 'dark');
        } else {
          setIsSystem(false);
          setThemeState(saved as Theme);
        }
      }
    });
  }, []);

  const setTheme = async (t: Theme) => {
    setThemeState(t);
    setIsSystem(false);
    await AsyncStorage.setItem(THEME_STORAGE_KEY, t);
  };

  const setSystemTheme = async () => {
    setIsSystem(true);
    const systemTheme = systemColorScheme === 'light' ? 'light' : 'dark';
    setThemeState(systemTheme);
    await AsyncStorage.setItem(THEME_STORAGE_KEY, 'system');
  };

  const toggleTheme = async () => {
    if (isSystem) {
      // If using system, switch to manual opposite
      const opposite = theme === 'dark' ? 'light' : 'dark';
      setTheme(opposite);
    } else {
      // Toggle manually
      const next = theme === 'dark' ? 'light' : 'dark';
      setTheme(next);
    }
  };

  // Update theme when system theme changes (only if using system)
  useEffect(() => {
    if (isSystem) {
      const systemTheme = systemColorScheme === 'light' ? 'light' : 'dark';
      setThemeState(systemTheme);
    }
  }, [systemColorScheme, isSystem]);

  const colors = theme === 'light' ? lightColors : darkColors;

  return (
    <ThemeContext.Provider value={{ theme, colors, setTheme, toggleTheme, isSystem }}>
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) throw new Error('useTheme must be used within ThemeProvider');
  return context;
};

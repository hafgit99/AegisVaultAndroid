/**
 * Aegis Vault - Secure Password Manager for Android
 * React Native implementation
 */

import React from 'react';
import { StatusBar, StyleSheet, View, LogBox } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { AuthProvider } from './src/contexts/AuthContext';
import { VaultProvider } from './src/contexts/VaultContext';
import { ThemeProvider } from './src/contexts/ThemeContext';
import { LanguageProvider } from './src/contexts/LanguageContext';
import AppNavigator from './src/navigation/AppNavigator';

// Ignore warnings for now
LogBox.ignoreLogs([
  'new NativeEventEmitter',
  'Require cycle',
  'Non-serializable values were found in the navigation state',
]);

function App() {
  return (
    <GestureHandlerRootView style={styles.container}>
      <SafeAreaProvider>
        <StatusBar barStyle="light-content" backgroundColor="#0a0a0a" />
        <ThemeProvider>
          <LanguageProvider>
            <AuthProvider>
              <VaultProvider>
                <AppContent />
              </VaultProvider>
            </AuthProvider>
          </LanguageProvider>
        </ThemeProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}

function AppContent() {
  return (
    <View style={styles.container}>
      <AppNavigator />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0a0a0a',
  },
});

export default App;

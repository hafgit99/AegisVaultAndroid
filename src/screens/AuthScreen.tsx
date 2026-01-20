import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  Alert,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  ActivityIndicator,
} from 'react-native';
import { useVault } from '../contexts/VaultContext';
import { useLanguage } from '../contexts/LanguageContext';
import { BiometricService } from '../services/biometricService';

interface Props {
  isSetup: boolean;
}

export default function AuthScreen({ isSetup }: Props) {
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [biometricAvailable, setBiometricAvailable] = useState(false);

  const { unlock, setup } = useVault();
  const { t } = useLanguage();

  React.useEffect(() => {
    checkBiometric();
  }, []);

  const checkBiometric = async () => {
    const available = await BiometricService.isAvailable();
    setBiometricAvailable(available);
  };

  const handleBiometricUnlock = async () => {
    try {
      const result = await BiometricService.unlock();
      if (result) {
        const { key, raw } = result;
        // TODO: Set the key in auth context
        setLoading(false);
      }
    } catch (e) {
      Alert.alert('Error', 'Biometric authentication failed');
    }
  };

  const handleSubmit = async () => {
    if (!password) {
      Alert.alert('Error', t.password);
      return;
    }

    setLoading(true);

    try {
      if (isSetup) {
        // Setup new vault
        if (password.length < 8) {
          Alert.alert('Error', t.weak_password);
          setLoading(false);
          return;
        }

        if (password !== confirmPassword) {
          Alert.alert('Error', t.password_mismatch);
          setLoading(false);
          return;
        }

        await setup(password);
      } else {
        // Unlock existing vault
        try {
          await unlock(password);
        } catch (e: any) {
          if (e.message === 'WRONG_PASSWORD') {
            Alert.alert('Error', t.incorrect_password);
          } else {
            Alert.alert('Error', t.error);
          }
        }
      }
    } catch (e) {
      Alert.alert('Error', t.error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <ScrollView
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.header}>
          <Text style={styles.title}>{t.app_name}</Text>
          <Text style={styles.subtitle}>
            {isSetup ? t.vault_setup_title : t.unlock}
          </Text>
        </View>

        <View style={styles.form}>
          {isSetup && (
            <Text style={styles.description}>{t.vault_setup_desc}</Text>
          )}

          <View style={styles.inputContainer}>
            <Text style={styles.label}>{t.password}</Text>
            <TextInput
              style={styles.input}
              value={password}
              onChangeText={setPassword}
              secureTextEntry
              placeholder="Enter your password"
              placeholderTextColor="#64748b"
              autoCapitalize="none"
              autoCorrect={false}
            />
          </View>

          {isSetup && (
            <View style={styles.inputContainer}>
              <Text style={styles.label}>{t.confirm_password}</Text>
              <TextInput
                style={styles.input}
                value={confirmPassword}
                onChangeText={setConfirmPassword}
                secureTextEntry
                placeholder="Confirm your password"
                placeholderTextColor="#64748b"
                autoCapitalize="none"
                autoCorrect={false}
              />
            </View>
          )}

          <TouchableOpacity
            style={[styles.button, loading && styles.buttonDisabled]}
            onPress={handleSubmit}
            disabled={loading}
          >
            {loading ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <Text style={styles.buttonText}>
                {isSetup ? t.setup : t.unlock}
              </Text>
            )}
          </TouchableOpacity>

          {!isSetup && biometricAvailable && (
            <TouchableOpacity
              style={styles.biometricButton}
              onPress={handleBiometricUnlock}
            >
              <Text style={styles.biometricButtonText}>{t.biometric_unlock}</Text>
            </TouchableOpacity>
          )}
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0a0a0a',
  },
  scrollContent: {
    flexGrow: 1,
    justifyContent: 'center',
    padding: 24,
  },
  header: {
    alignItems: 'center',
    marginBottom: 40,
  },
  title: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#fff',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 18,
    color: '#94a3b8',
  },
  description: {
    fontSize: 14,
    color: '#94a3b8',
    textAlign: 'center',
    marginBottom: 24,
    lineHeight: 20,
  },
  form: {
    width: '100%',
  },
  inputContainer: {
    marginBottom: 20,
  },
  label: {
    fontSize: 14,
    fontWeight: '500',
    color: '#e2e8f0',
    marginBottom: 8,
  },
  input: {
    backgroundColor: '#1a1a1a',
    borderColor: '#27272a',
    borderWidth: 1,
    borderRadius: 8,
    padding: 16,
    fontSize: 16,
    color: '#fff',
  },
  button: {
    backgroundColor: '#3b82f6',
    borderRadius: 8,
    padding: 16,
    alignItems: 'center',
    marginTop: 8,
  },
  buttonDisabled: {
    opacity: 0.5,
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  biometricButton: {
    backgroundColor: 'transparent',
    borderRadius: 8,
    padding: 16,
    alignItems: 'center',
    marginTop: 16,
    borderColor: '#3b82f6',
    borderWidth: 1,
  },
  biometricButtonText: {
    color: '#3b82f6',
    fontSize: 16,
    fontWeight: '600',
  },
});

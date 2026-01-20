import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Switch,
  Alert,
} from 'react-native';
import { useAuth } from '../contexts/AuthContext';
import { useLanguage } from '../contexts/LanguageContext';
import { useTheme } from '../contexts/ThemeContext';

export default function SettingsScreen() {
  const { logout } = useAuth();
  const { t, lang, setLang } = useLanguage();
  const { theme, toggleTheme } = useTheme();

  const handleLogout = () => {
    Alert.alert(t.logout, 'Are you sure you want to lock the vault?', [
      { text: t.cancel, style: 'cancel' },
      {
        text: t.logout,
        style: 'destructive',
        onPress: () => logout(),
      },
    ]);
  };

  return (
    <ScrollView style={styles.container}>
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Appearance</Text>

        <TouchableOpacity style={styles.settingItem} onPress={toggleTheme}>
          <View style={styles.settingLeft}>
            <Text style={styles.settingLabel}>{t.theme}</Text>
            <Text style={styles.settingValue}>
              {theme === 'dark' ? t.dark_mode : t.light_mode}
            </Text>
          </View>
        </TouchableOpacity>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Language</Text>

        <TouchableOpacity
          style={styles.settingItem}
          onPress={() => setLang(lang === 'en' ? 'tr' : 'en')}
        >
          <View style={styles.settingLeft}>
            <Text style={styles.settingLabel}>{t.language}</Text>
            <Text style={styles.settingValue}>{lang === 'en' ? 'English' : 'Türkçe'}</Text>
          </View>
        </TouchableOpacity>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>About</Text>

        <View style={styles.settingItem}>
          <View style={styles.settingLeft}>
            <Text style={styles.settingLabel}>{t.version}</Text>
            <Text style={styles.settingValue}>2.3.0</Text>
          </View>
        </View>
      </View>

      <TouchableOpacity style={styles.logoutButton} onPress={handleLogout}>
        <Text style={styles.logoutButtonText}>{t.logout}</Text>
      </TouchableOpacity>

      <Text style={styles.footer}>
        Aegis Vault - Secure Password Manager{'\n'}
        Encrypted with Argon2id + AES-256-GCM
      </Text>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0a0a0a',
  },
  section: {
    marginTop: 24,
    paddingHorizontal: 16,
  },
  sectionTitle: {
    fontSize: 13,
    fontWeight: '600',
    color: '#64748b',
    textTransform: 'uppercase',
    marginBottom: 8,
    marginLeft: 4,
  },
  settingItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#1a1a1a',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderRadius: 8,
    marginBottom: 8,
  },
  settingLeft: {
    flex: 1,
  },
  settingLabel: {
    fontSize: 16,
    color: '#e2e8f0',
    marginBottom: 2,
  },
  settingValue: {
    fontSize: 14,
    color: '#94a3b8',
  },
  logoutButton: {
    backgroundColor: '#ef4444',
    marginHorizontal: 16,
    marginTop: 32,
    padding: 16,
    borderRadius: 8,
    alignItems: 'center',
  },
  logoutButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  footer: {
    fontSize: 12,
    color: '#64748b',
    textAlign: 'center',
    marginTop: 32,
    marginBottom: 24,
    lineHeight: 18,
  },
});

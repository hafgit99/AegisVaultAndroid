import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TextInput,
  TouchableOpacity,
  Alert,
} from 'react-native';
import { useVault } from '../contexts/VaultContext';
import { useLanguage } from '../contexts/LanguageContext';
import { useTheme } from '../contexts/ThemeContext';
import { Category, SensitiveData } from '../types';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { StackNavigationProp } from '@react-navigation/stack';

type NavigationProp = StackNavigationProp<{ EntryForm: { entryId?: string } }>;

export default function EntryFormScreen() {
  const navigation = useNavigation<NavigationProp>();
  const route = useRoute<RouteProp<{ EntryForm: { entryId?: string } }, 'EntryForm'>>();
  const { entryId } = route.params;

  const { saveEntry, entries, decryptData } = useVault();
  const { t } = useLanguage();
  const { colors } = useTheme();

  const [title, setTitle] = React.useState('');
  const [username, setUsername] = React.useState('');
  const [password, setPassword] = React.useState('');
  const [website, setWebsite] = React.useState('');
  const [notes, setNotes] = React.useState('');
  const [category, setCategory] = React.useState<Category>(Category.LOGIN);
  const [loading, setLoading] = React.useState(false);

  React.useEffect(() => {
    if (entryId) {
      loadEntry(entryId);
    }
  }, [entryId]);

  const loadEntry = async (id: string) => {
    try {
      const entry = entries.find(e => e.id === id);
      if (entry) {
        const sensitive = await decryptData(entry);
        setTitle(entry.title || '');
        setUsername(entry.username || '');
        setPassword(sensitive.password || '');
        setWebsite(sensitive.url || '');
        setNotes(sensitive.notes || '');
        setCategory(entry.category || Category.LOGIN);
      }
    } catch (e) {
      Alert.alert('Error', 'Failed to load entry');
    }
  };

  const handleSave = async () => {
    if (!title) {
      Alert.alert('Error', 'Title is required');
      return;
    }

    setLoading(true);

    try {
      const sensitive: SensitiveData = {
        password,
        url: website,
        notes,
      };

      await saveEntry({
        id: entryId,
        title,
        username,
        category,
        sensitive,
      });

      navigation.goBack();
    } catch (e) {
      Alert.alert('Error', 'Failed to save entry');
    } finally {
      setLoading(false);
    }
  };

  return (
    <ScrollView style={[styles.container, { backgroundColor: colors.background }]}>
      <View style={styles.form}>
        <View style={styles.inputGroup}>
          <Text style={[styles.label, { color: colors.foreground }]}>{t.title}</Text>
          <TextInput
            style={[styles.input, { backgroundColor: colors.input, color: colors.foreground }]}
            value={title}
            onChangeText={setTitle}
            placeholder="Entry title"
            placeholderTextColor={colors.mutedForeground}
          />
        </View>

        <View style={styles.inputGroup}>
          <Text style={[styles.label, { color: colors.foreground }]}>{t.username}</Text>
          <TextInput
            style={[styles.input, { backgroundColor: colors.input, color: colors.foreground }]}
            value={username}
            onChangeText={setUsername}
            placeholder="Username or email"
            placeholderTextColor={colors.mutedForeground}
            autoCapitalize="none"
          />
        </View>

        <View style={styles.inputGroup}>
          <Text style={[styles.label, { color: colors.foreground }]}>{t.password}</Text>
          <TextInput
            style={[styles.input, { backgroundColor: colors.input, color: colors.foreground }]}
            value={password}
            onChangeText={setPassword}
            placeholder="Password"
            placeholderTextColor={colors.mutedForeground}
            secureTextEntry
          />
        </View>

        <View style={styles.inputGroup}>
          <Text style={[styles.label, { color: colors.foreground }]}>{t.website}</Text>
          <TextInput
            style={[styles.input, { backgroundColor: colors.input, color: colors.foreground }]}
            value={website}
            onChangeText={setWebsite}
            placeholder="https://example.com"
            placeholderTextColor={colors.mutedForeground}
            autoCapitalize="none"
            keyboardType="url"
          />
        </View>

        <View style={styles.inputGroup}>
          <Text style={[styles.label, { color: colors.foreground }]}>{t.notes}</Text>
          <TextInput
            style={[styles.textArea, { backgroundColor: colors.input, color: colors.foreground }]}
            value={notes}
            onChangeText={setNotes}
            placeholder="Additional notes"
            placeholderTextColor={colors.mutedForeground}
            multiline
            numberOfLines={4}
            textAlignVertical="top"
          />
        </View>

        <TouchableOpacity
          style={[styles.saveButton, { backgroundColor: colors.primary }]}
          onPress={handleSave}
          disabled={loading}
        >
          <Text style={styles.saveButtonText}>{t.save}</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.cancelButton, { borderColor: colors.border }]}
          onPress={() => navigation.goBack()}
        >
          <Text style={[styles.cancelButtonText, { color: colors.foreground }]}>{t.cancel}</Text>
        </TouchableOpacity>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  form: {
    padding: 16,
  },
  inputGroup: {
    marginBottom: 20,
  },
  label: {
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 8,
  },
  input: {
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
  },
  textArea: {
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    minHeight: 100,
  },
  saveButton: {
    borderRadius: 8,
    padding: 16,
    alignItems: 'center',
    marginTop: 8,
  },
  saveButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  cancelButton: {
    borderRadius: 8,
    padding: 16,
    alignItems: 'center',
    marginTop: 12,
    borderWidth: 1,
  },
  cancelButtonText: {
    fontSize: 16,
    fontWeight: '600',
  },
});

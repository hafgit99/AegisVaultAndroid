import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  TextInput,
  Alert,
} from 'react-native';
import { useVault } from '../contexts/VaultContext';
import { useAuth } from '../contexts/AuthContext';
import { useLanguage } from '../contexts/LanguageContext';
import { useTheme } from '../contexts/ThemeContext';

export default function DashboardScreen() {
  const { entries, folders, deleteEntry, lock } = useVault();
  const { logout } = useAuth();
  const { t } = useLanguage();
  const { colors } = useTheme();
  const [searchQuery, setSearchQuery] = React.useState('');

  const filteredEntries = entries.filter(
    (entry) =>
      !entry.deletedAt &&
      (searchQuery === '' ||
        entry.title?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        entry.username?.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  const handleDelete = (id: string, title: string) => {
    Alert.alert(t.delete_entry_title, `${t.delete_confirmation} "${title}"?`, [
      { text: t.cancel, style: 'cancel' },
      {
        text: t.delete,
        style: 'destructive',
        onPress: () => deleteEntry(id),
      },
    ]);
  };

  const handleLock = () => {
    logout();
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      {/* Header */}
      <View style={[styles.header, { backgroundColor: colors.card, borderBottomColor: colors.border }]}>
        <Text style={[styles.headerTitle, { color: colors.foreground }]}>
          {t.entries}
        </Text>
        <TouchableOpacity style={styles.lockButton} onPress={handleLock}>
          <Text style={styles.lockButtonText}>🔒</Text>
        </TouchableOpacity>
      </View>

      {/* Search */}
      <View style={styles.searchContainer}>
        <TextInput
          style={[styles.searchInput, { backgroundColor: colors.input, color: colors.foreground }]}
          value={searchQuery}
          onChangeText={setSearchQuery}
          placeholder={t.search}
          placeholderTextColor={colors.mutedForeground}
        />
      </View>

      {/* Entries List */}
      <ScrollView style={styles.content}>
        {filteredEntries.length === 0 ? (
          <View style={styles.emptyState}>
            <Text style={[styles.emptyText, { color: colors.mutedForeground }]}>
              {searchQuery ? t.no_entries : t.add_entry}
            </Text>
          </View>
        ) : (
          filteredEntries.map((entry) => (
            <TouchableOpacity
              key={entry.id}
              style={[styles.entryCard, { backgroundColor: colors.card, borderColor: colors.border }]}
              onPress={() => {
                // Navigate to entry detail
              }}
            >
              <View style={styles.entryHeader}>
                <Text style={[styles.entryTitle, { color: colors.cardForeground }]}>
                  {entry.title || 'Unnamed'}
                </Text>
                {entry.isFavorite && <Text style={styles.favoriteIcon}>⭐</Text>}
              </View>
              <Text style={[styles.entryUsername, { color: colors.mutedForeground }]}>
                {entry.username || 'No username'}
              </Text>
              <View style={styles.entryFooter}>
                <Text style={[styles.entryCategory, { color: colors.mutedForeground }]}>
                  {entry.category}
                </Text>
                <TouchableOpacity
                  style={styles.deleteButton}
                  onPress={() => handleDelete(entry.id, entry.title || 'Entry')}
                >
                  <Text style={styles.deleteButtonText}>🗑️</Text>
                </TouchableOpacity>
              </View>
            </TouchableOpacity>
          ))
        )}
      </ScrollView>

      {/* Add Entry FAB */}
      <TouchableOpacity
        style={[styles.fab, { backgroundColor: colors.primary }]}
        onPress={() => {
          // Navigate to entry form
        }}
      >
        <Text style={styles.fabText}>+</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 16,
    borderBottomWidth: 1,
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: 'bold',
  },
  lockButton: {
    padding: 8,
  },
  lockButtonText: {
    fontSize: 20,
  },
  searchContainer: {
    padding: 16,
  },
  searchInput: {
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
  },
  content: {
    flex: 1,
    paddingHorizontal: 16,
  },
  emptyState: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 60,
  },
  emptyText: {
    fontSize: 16,
  },
  entryCard: {
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
  },
  entryHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  entryTitle: {
    fontSize: 16,
    fontWeight: '600',
    flex: 1,
  },
  favoriteIcon: {
    fontSize: 14,
  },
  entryUsername: {
    fontSize: 14,
    marginBottom: 8,
  },
  entryFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  entryCategory: {
    fontSize: 12,
  },
  deleteButton: {
    padding: 4,
  },
  deleteButtonText: {
    fontSize: 16,
  },
  fab: {
    position: 'absolute',
    bottom: 24,
    right: 24,
    width: 56,
    height: 56,
    borderRadius: 28,
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 5,
  },
  fabText: {
    fontSize: 32,
    color: '#fff',
    fontWeight: '300',
  },
});

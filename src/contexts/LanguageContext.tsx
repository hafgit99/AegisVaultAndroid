import React, { createContext, useContext, useState, useEffect } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import I18n from 'i18n-js';
import { getLocales } from 'react-native-localize';

// Translations
export type Language = 'en' | 'tr';

const translations = {
  en: {
    app_name: 'Aegis Vault',
    unlock: 'Unlock',
    setup: 'Setup Vault',
    password: 'Password',
    confirm_password: 'Confirm Password',
    biometric_unlock: 'Biometric Unlock',
    settings: 'Settings',
    add_entry: 'Add Entry',
    search: 'Search',
    entries: 'Entries',
    folders: 'Folders',
    favorites: 'Favorites',
    security: 'Security',
    backup: 'Backup',
    about: 'About',
    version: 'Version',
    logout: 'Lock Vault',
    delete: 'Delete',
    edit: 'Edit',
    save: 'Save',
    cancel: 'Cancel',
    confirm: 'Confirm',
    error: 'Error',
    success: 'Success',
    loading: 'Loading...',
    no_entries: 'No entries found',
    no_folders: 'No folders found',
    create_folder: 'Create Folder',
    folder_name: 'Folder Name',
    category: 'Category',
    login: 'Login',
    credit_card: 'Credit Card',
    secure_note: 'Secure Note',
    secure_file: 'Secure File',
    crypto_wallet: 'Crypto Wallet',
    passkey: 'Passkey',
    title: 'Title',
    username: 'Username',
    website: 'Website',
    notes: 'Notes',
    favorite: 'Favorite',
    auto_lock: 'Auto Lock',
    auto_lock_after: 'Auto Lock After',
    minutes: 'minutes',
    theme: 'Theme',
    language: 'Language',
    dark_mode: 'Dark Mode',
    light_mode: 'Light Mode',
    system_mode: 'System Default',
    biometric_prompt: 'Authenticate to access your vault',
    incorrect_password: 'Incorrect password',
    vault_locked: 'Vault Locked',
    vault_setup_title: 'Create Your Vault',
    vault_setup_desc: 'Choose a strong password to encrypt your vault. This cannot be recovered.',
    weak_password: 'Password is too weak',
    password_mismatch: 'Passwords do not match',
    recovery_words_title: 'Recovery Words',
    recovery_words_desc: 'Save these words in a safe place. They are your only way to recover your vault.',
    show_recovery_words: 'Show Recovery Words',
    hide_recovery_words: 'Hide Recovery Words',
    copy_to_clipboard: 'Copy to Clipboard',
    copied: 'Copied!',
    delete_confirmation: 'Are you sure you want to delete this item?',
    delete_entry_title: 'Delete Entry',
    delete_folder_title: 'Delete Folder',
    empty_fields: 'Please fill in all required fields',
  },
  tr: {
    app_name: 'Aegis Vault',
    unlock: 'Kilit Aç',
    setup: 'Kasa Kur',
    password: 'Şifre',
    confirm_password: 'Şifre Tekrarı',
    biometric_unlock: 'Biyometrik Kilit Açma',
    settings: 'Ayarlar',
    add_entry: 'Kayıt Ekle',
    search: 'Ara',
    entries: 'Kayıtlar',
    folders: 'Klasörler',
    favorites: 'Favoriler',
    security: 'Güvenlik',
    backup: 'Yedekleme',
    about: 'Hakkında',
    version: 'Sürüm',
    logout: 'Kasayı Kilitle',
    delete: 'Sil',
    edit: 'Düzenle',
    save: 'Kaydet',
    cancel: 'İptal',
    confirm: 'Onayla',
    error: 'Hata',
    success: 'Başarılı',
    loading: 'Yükleniyor...',
    no_entries: 'Kayıt bulunamadı',
    no_folders: 'Klasör bulunamadı',
    create_folder: 'Klasör Oluştur',
    folder_name: 'Klasör Adı',
    category: 'Kategori',
    login: 'Giriş',
    credit_card: 'Kredi Kartı',
    secure_note: 'Güvenli Not',
    secure_file: 'Güvenli Dosya',
    crypto_wallet: 'Kripto Cüzdan',
    passkey: 'Geçiş Anahtarı',
    title: 'Başlık',
    username: 'Kullanıcı Adı',
    website: 'Web Sitesi',
    notes: 'Notlar',
    favorite: 'Favori',
    auto_lock: 'Otomatik Kilitle',
    auto_lock_after: 'Otomatik Kilitleme Süresi',
    minutes: 'dakika',
    theme: 'Tema',
    language: 'Dil',
    dark_mode: 'Karanlık Mod',
    light_mode: 'Aydınlık Mod',
    system_mode: 'Sistem Varsayılanı',
    biometric_prompt: 'Kasanıza erişmek için kimliğinizi doğrulayın',
    incorrect_password: 'Hatalı şifre',
    vault_locked: 'Kasa Kilitli',
    vault_setup_title: 'Kasanızı Oluşturun',
    vault_setup_desc: 'Kasanızı şifrelemek için güçlü bir şifre seçin. Bu şifre geri alınamaz.',
    weak_password: 'Şifre çok zayıf',
    password_mismatch: 'Şifreler eşleşmiyor',
    recovery_words_title: 'Kurtarma Kelimeleri',
    recovery_words_desc: 'Bu kelimeleri güvenli bir yerde saklayın. Kasanızı kurtarmanın tek yolu bunlardır.',
    show_recovery_words: 'Kurtarma Kelimelerini Göster',
    hide_recovery_words: 'Kurtarma Kelimelerini Gizle',
    copy_to_clipboard: 'Panoya Kopyala',
    copied: 'Kopyalandı!',
    delete_confirmation: 'Bu öğeyi silmek istediğinizden emin misiniz?',
    delete_entry_title: 'Kaydı Sil',
    delete_folder_title: 'Klasörü Sil',
    empty_fields: 'Lütfen tüm zorunlu alanları doldurun',
  },
};

interface LanguageContextType {
  lang: Language;
  setLang: (l: Language) => void;
  t: (key: keyof typeof translations['en']) => string;
}

const LanguageContext = createContext<LanguageContextType | undefined>(undefined);

const LANG_STORAGE_KEY = 'aegis_lang';

export const LanguageProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [lang, setLangState] = useState<Language>('en');

  // Load saved language on mount
  useEffect(() => {
    AsyncStorage.getItem(LANG_STORAGE_KEY).then((saved) => {
      if (saved && (saved === 'en' || saved === 'tr')) {
        setLangState(saved as Language);
        I18n.locale = saved;
      }
    });
  }, []);

  const setLang = async (l: Language) => {
    setLangState(l);
    I18n.locale = l;
    await AsyncStorage.setItem(LANG_STORAGE_KEY, l);
  };

  const t = (key: keyof typeof translations['en']): string => {
    return translations[lang][key] || translations['en'][key];
  };

  return (
    <LanguageContext.Provider value={{ lang, setLang, t }}>
      {children}
    </LanguageContext.Provider>
  );
};

export const useLanguage = () => {
  const context = useContext(LanguageContext);
  if (!context) throw new Error('useLanguage must be used within LanguageProvider');
  return context;
};

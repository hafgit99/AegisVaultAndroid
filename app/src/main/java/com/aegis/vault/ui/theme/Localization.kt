package com.aegis.vault.ui.theme

import androidx.compose.runtime.compositionLocalOf

abstract class Strings {
    abstract val appName: String
    abstract val vault: String
    abstract val settings: String
    abstract val premium: String
    abstract val audit: String
    abstract val searchPlaceholder: String
    abstract val addEntry: String
    abstract val editEntry: String
    abstract val title: String
    abstract val username: String
    abstract val password: String
    abstract val website: String
    abstract val totpSecret: String
    abstract val attachment: String
    abstract val save: String
    abstract val update: String
    abstract val delete: String
    abstract val cancel: String
    abstract val ok: String
    abstract val lock: String
    abstract val unlock: String
    abstract val masterPassword: String
    abstract val confirmPassword: String
    abstract val recoveryPhrase: String
    abstract val setupVault: String
    abstract val setupDescription: String
    abstract val recoveryDescription: String
    abstract val savedStart: String
    abstract val passwordRequirements: String
    abstract val reqMinLength: String
    abstract val reqUpperLower: String
    abstract val reqDigit: String
    abstract val reqSpecial: String
    abstract val passwordMismatch: String
    abstract val autoLock: String
    abstract val autoLockSub: String
    abstract val language: String
    abstract val languageSub: String
    abstract val securityAudit: String
    abstract val securityAuditSub: String
    abstract val generator: String
    abstract val generatorSub: String
    abstract val trialExpired: String
    abstract val trialExpiredSub: String
    abstract val enterLicense: String
    abstract val activateLicense: String
    abstract val invalidLicense: String
    abstract val copyId: String
    abstract val idCopied: String
    abstract val passwordCopied: String
    abstract val securityScore: String
    abstract val weak: String
    abstract val reused: String
    abstract val secure: String
    abstract val analyzing: String
    abstract val backup: String
    abstract val import: String
    abstract val export: String
    abstract val reset: String
    abstract val resetWarning: String
    abstract val resetConfirm: String
    abstract val changePassword: String
    abstract val oldPassword: String
    abstract val newPassword: String
    abstract val backupPasswordTitle: String
    abstract val encryptAndSave: String
    abstract val backupSuccess: String
    abstract val backupError: String
    abstract val importSuccess: String
    abstract val importError: String
    abstract val exportJson: String
    abstract val exportCsv: String
    abstract val importJson: String
    abstract val exportJsonSub: String
    abstract val exportCsvSub: String
    abstract val importJsonSub: String
    abstract val auditScoreDesc: String
    abstract val titlePlaceholder: String
    abstract val usernamePlaceholder: String
    abstract val passwordNew: String
    abstract val totpPlaceholder: String
    abstract val websitePlaceholder: String
    abstract val attachmentPlaceholder: String
    abstract val supportedFormats: String
    abstract val removeFile: String
    abstract val totpTitle: String
    abstract val secondsSuffix: String
    abstract val attachmentDecryptError: String
    abstract val securityAnalysis: String
    abstract val weakCaps: String
    abstract val mediumCaps: String
    abstract val strongCaps: String
    abstract val excellentCaps: String
    abstract val unknownCaps: String
    abstract val usernameCopied: String
    abstract val clipboardCleaned: String
    abstract val decryptError: String
    abstract val unlocking: String
    abstract val vaultLocked: String
    abstract val enterMasterPassword: String
    abstract val show: String
    abstract val hide: String
    abstract val tooManyAttempts: String
    abstract val wrongPassword: String
    abstract val remainingAttempts: String
    abstract val unlockWithPassword: String
    abstract val useBiometric: String
    abstract val minuteSuffix: String
    abstract val premiumDescription: String
    abstract val premiumSubtitle: String
    abstract val deviceIdLabel: String
    abstract val cryptoPayments: String
    abstract val cryptoDescription: String
    abstract val addressCopied: String
    abstract val back: String
    abstract val premiumAudit: String
    abstract val premiumAuditDesc: String
    abstract val weakPasswordsCaps: String
    abstract val reusedPasswordsCaps: String
    abstract val easyToGuess: String
    abstract val alsoUsedInOtherAccounts: String
    abstract val refresh: String
    abstract val length: String
    abstract val includeUppercase: String
    abstract val includeNumbers: String
    abstract val includeSymbols: String
    abstract val useThisPassword: String
    abstract val exportJsonSuccess: String
    abstract val exportCsvSuccess: String
    abstract val importCount: String
    abstract val importSource: String
    abstract val importSourceDesc: String
    abstract val aegisJsonRecommended: String
    abstract val reencrypting: String
    abstract val reencryptingDesc: String
    abstract val newPasswordReq: String
    abstract val confirmNewPasswordLabel: String
    abstract val passwordChangedSuccess: String
    abstract val wrongOldPassword: String
    abstract val passwordMinLengthError: String
    abstract val resetDatabaseTitle: String
    abstract val resetDatabaseWarning: String
    abstract val immediately: String
    abstract val minute1: String
    abstract val minutes5: String
    abstract val minutes15: String
    abstract val never: String
    abstract val freeTrial: String
    abstract val lifetimeLicenseActive: String
    abstract val trialDaysRemaining: String
    abstract val general: String
    abstract val security: String
    abstract val reencryptTitle: String
    abstract val reencryptSub: String
    abstract val securityAuditItemSub: String
    abstract val exportEncryptedJson: String
    abstract val exportEncryptedJsonSub: String
    abstract val dangerousArea: String
    abstract val resetDatabaseItem: String
    abstract val resetDatabaseItemSub: String
    abstract val continueCaps: String
    abstract val creatingSecureKey: String
    abstract val computingArgon2: String
}

object StringsTR : Strings() {
    override val appName = "AEGIS VAULT"
    override val vault = "Kasa"
    override val settings = "Ayarlar"
    override val premium = "Premium"
    override val audit = "Denetim"
    override val searchPlaceholder = "Kasanızda arayın..."
    override val addEntry = "Yeni Kayıt Ekle"
    override val editEntry = "Kaydı Düzenle"
    override val title = "Başlık"
    override val username = "Kullanıcı Adı / E-posta"
    override val password = "Şifre"
    override val website = "Web Sitesi"
    override val totpSecret = "2FA Secret (TOTP)"
    override val attachment = "Güvenli Dosya Eki"
    override val save = "KAYDET"
    override val update = "GÜNCELLE"
    override val delete = "SİL"
    override val cancel = "İPTAL"
    override val ok = "TAMAM"
    override val lock = "Kilitle"
    override val unlock = "Kilidi Aç"
    override val masterPassword = "Ana Şifre"
    override val confirmPassword = "Şifreyi Onayla"
    override val recoveryPhrase = "Kurtarma Kelimeleri"
    override val setupVault = "Kasayı Kur"
    override val setupDescription = "Lütfen güçlü bir Ana Şifre belirleyin. Bu şifre kasanızın tek anahtarıdır."
    override val recoveryDescription = "Ana şifrenizi unutursanız bu 12 kelime kasanıza erişim sağlamanın tek yoludur. Not edin ve ASLA paylaşmayın."
    override val savedStart = "KAYDETTİM, BAŞLAT"
    override val passwordRequirements = "Şifre Gereksinimleri"
    override val reqMinLength = "En az 12 karakter"
    override val reqUpperLower = "En az bir büyük ve bir küçük harf"
    override val reqDigit = "En az bir rakam"
    override val reqSpecial = "En az bir özel karakter (!@#$..)"
    override val passwordMismatch = "Şifreler eşleşmiyor"
    override val autoLock = "Otomatik Kilitleme"
    override val autoLockSub = "Kasa ne zaman kilitlensin?"
    override val language = "Dil / Language"
    override val languageSub = "Uygulama dilini değiştirin"
    override val securityAudit = "Güvenlik Denetimi"
    override val securityAuditSub = "Kasanızın sağlık durumunu görün"
    override val generator = "Şifre Üretici"
    override val generatorSub = "Güçlü şifreler oluşturun"
    override val trialExpired = "Deneme Süresi Doldu!"
    override val trialExpiredSub = "Kasanıza erişmek için lütfen Premium lisans alınız."
    override val enterLicense = "Lisans Anahtarını Girin"
    override val activateLicense = "LİSANSI ETKİNLEŞTİR"
    override val invalidLicense = "Geçersiz Lisans Anahtarı!"
    override val copyId = "ID Kopyala"
    override val idCopied = "ID Kopyalandı"
    override val passwordCopied = "Şifre kopyalandı"
    override val securityScore = "Güvenlik Puanı"
    override val weak = "Zayıf"
    override val reused = "Tekrar"
    override val secure = "Güvenli"
    override val analyzing = "Kasanız analiz ediliyor..."
    override val backup = "Yedekle ve İçe Aktar"
    override val import = "Verileri İçe Aktar"
    override val export = "Verileri Dışa Aktar (CSV/JSON)"
    override val reset = "Veri Tabanını Sıfırla"
    override val resetWarning = "BU İŞLEM TÜM VERİLERİNİZİ KALICI OLARAK SİLECEKTİR!"
    override val resetConfirm = "Eminim, Her Şeyi Sil"
    override val changePassword = "Ana Şifreyi Değiştir"
    override val oldPassword = "Eski Şifre"
    override val newPassword = "Yeni Şifre"
    override val backupPasswordTitle = "Yedek Şifresi Belirleyin"
    override val encryptAndSave = "ŞİFRELE VE KAYDET"
    override val backupSuccess = "Yedek başarıyla oluşturuldu"
    override val backupError = "Yedekleme hatası"
    override val importSuccess = "Veriler başarıyla içe aktarıldı"
    override val importError = "İçe aktarma hatası"
    override val exportJson = "JSON Olarak Dışa Aktar"
    override val exportCsv = "CSV Olarak Dışa Aktar"
    override val importJson = "JSON Dosyasından İçe Aktar"
    override val exportJsonSub = "Uyumlu Aegis JSON yedeği"
    override val exportCsvSub = "Şifresiz tablo formatı"
    override val importJsonSub = "Daha önce aldığınız yedek"
    override val auditScoreDesc = "Güvenlik Puanınız"
    override val titlePlaceholder = "Başlık (Örn: Google)"
    override val usernamePlaceholder = "Kullanıcı Adı / E-posta"
    override val passwordNew = "Yeni Şifre (Gerekliyse)"
    override val totpPlaceholder = "2FA Secret (TOTP için)"
    override val websitePlaceholder = "Web Sitesi (Opsiyonel)"
    override val attachmentPlaceholder = "Dosya ekle (max 15 MB)"
    override val supportedFormats = "Desteklenen formatlar: PDF, ZIP, RAR, JPG, PNG, TXT, DOC, XLS ve dahası"
    override val removeFile = "Dosyayı Kaldır"
    override val totpTitle = "2FA DOĞRULAMA KODU"
    override val secondsSuffix = "sn"
    override val attachmentDecryptError = "Dosya şifresi çözülemedi!"
    override val securityAnalysis = "Güvenlik Analizi"
    override val weakCaps = "ZAYIF"
    override val mediumCaps = "ORTA"
    override val strongCaps = "GÜÇLÜ"
    override val excellentCaps = "MÜKEMMEL"
    override val unknownCaps = "BİLİNMİYOR"
    override val usernameCopied = "Kullanıcı adı kopyalandı"
    override val clipboardCleaned = "Pano güvenlik için temizlendi"
    override val decryptError = "Hata: Şifre çözülemedi"
    override val unlocking = "Anahtar Çözülüyor..."
    override val vaultLocked = "Kasa Kilitli"
    override val enterMasterPassword = "Ana Şifreyi Girin"
    override val show = "Göster"
    override val hide = "Gizle"
    override val tooManyAttempts = "Çok fazla başarısız deneme. Lütfen bekleyin:"
    override val wrongPassword = "Hatalı şifre"
    override val remainingAttempts = "Kalan deneme"
    override val unlockWithPassword = "ŞİFRE İLE AÇ"
    override val useBiometric = "Biyometrik Kullan"
    override val minuteSuffix = "dk"
    override val premiumDescription = "Premium ile Sınırsız Güvenlik"
    override val premiumSubtitle = "Tek seferlik 15€ ödeme ile ömür boyu kullanım ve tüm cihazlarda donanım kilidi desteği."
    override val deviceIdLabel = "Cihaz Kimliğiniz (Device ID)"
    override val cryptoPayments = "Kripto Ödeme Adresleri"
    override val cryptoDescription = "Lütfen 15€ karşılığı kripto gönderip TXID ve Device ID'nizi sales@hetech-me.space adresine mail atınız."
    override val addressCopied = "Adresi Kopyalandı"
    override val back = "Geri"
    override val premiumAudit = "Premium Denetimi"
    override val premiumAuditDesc = "Hangi şifrelerinizin zayıf olduğunu ve hangilerinin sızdırıldığını görmek için Premium'a yükseltin."
    override val weakPasswordsCaps = "ZAYIF ŞİFRELER"
    override val reusedPasswordsCaps = "AYNI KULLANILAN ŞİFRELER"
    override val easyToGuess = "Kolay tahmin edilebilir"
    override val alsoUsedInOtherAccounts = "Başka hesaplarda da kullanılıyor"
    override val refresh = "Yenile"
    override val length = "Uzunluk"
    override val includeUppercase = "Büyük Harfler (A-Z)"
    override val includeNumbers = "Rakamlar (0-9)"
    override val includeSymbols = "Semboller (!@#$)"
    override val useThisPassword = "BU ŞİFREYİ KULLAN"
    override val exportJsonSuccess = "Kasa JSON olarak aktarıldı"
    override val exportCsvSuccess = "Kasa CSV olarak aktarıldı"
    override val importCount = "kayıt içeri aktarıldı"
    override val importSource = "İçeri Aktarma Kaynağı"
    override val importSourceDesc = "Lütfen yedeğin formatını seçin:"
    override val aegisJsonRecommended = "Aegis JSON (Önerilen)"
    override val reencrypting = "Tüm veriler yeniden şifreleniyor..."
    override val reencryptingDesc = "Bu işlem tüm verilerinizi yeni bir anahtarla yeniden şifreleyecektir."
    override val newPasswordReq = "Yeni Şifre (Min 12 kar.)"
    override val confirmNewPasswordLabel = "Yeni Şifreyi Onayla"
    override val passwordChangedSuccess = "Ana şifre başarıyla güncellendi"
    override val wrongOldPassword = "Hata: Eski şifre yanlış!"
    override val passwordMinLengthError = "Şifre en az 12 karakter olmalıdır!"
    override val resetDatabaseTitle = "Veritabanını Sıfırla?"
    override val resetDatabaseWarning = "Tüm şifreleriniz kalıcı olarak silinecektir. Bu işlem geri alınamaz."
    override val immediately = "Hemen"
    override val minute1 = "1 Dakika"
    override val minutes5 = "5 Dakika"
    override val minutes15 = "15 Dakika"
    override val never = "Hiçbir zaman"
    override val freeTrial = "Ücretsiz Deneme Sürümü"
    override val lifetimeLicenseActive = "Ömür boyu lisans aktif"
    override val trialDaysRemaining = "Deneme süresinin bitmesine %d gün kaldı"
    override val general = "Genel"
    override val security = "Güvenlik"
    override val reencryptTitle = "Şifre/Giriş Değiştirme"
    override val reencryptSub = "Ana şifrenizi güncelleyin"
    override val securityAuditItemSub = "Kasanızın genel sağlık durumunu analiz edin"
    override val exportEncryptedJson = "Şifreli JSON Olarak Dışa Aktar"
    override val exportEncryptedJsonSub = "Ekstra şifre ile korunan yedek"
    override val dangerousArea = "Tehlikeli Alan"
    override val resetDatabaseItem = "Kasayı Tamamen Sıfırla"
    override val resetDatabaseItemSub = "Tüm verileri siler ve uygulamayı sıfırlar"
    override val continueCaps = "İLERLE"
    override val creatingSecureKey = "Güvenli anahtar oluşturuluyor..."
    override val computingArgon2 = "Argon2id hesaplanıyor, lütfen bekleyin."
}

object StringsEN : Strings() {
    override val appName = "AEGIS VAULT"
    override val vault = "Vault"
    override val settings = "Settings"
    override val premium = "Premium"
    override val audit = "Audit"
    override val searchPlaceholder = "Search in vault..."
    override val addEntry = "Add New Entry"
    override val editEntry = "Edit Entry"
    override val title = "Title"
    override val username = "Username / Email"
    override val password = "Password"
    override val website = "Website"
    override val totpSecret = "2FA Secret (TOTP)"
    override val attachment = "Secure File Attachment"
    override val save = "SAVE"
    override val update = "UPDATE"
    override val delete = "DELETE"
    override val cancel = "CANCEL"
    override val ok = "OK"
    override val lock = "Lock"
    override val unlock = "Unlock"
    override val masterPassword = "Master Password"
    override val confirmPassword = "Confirm Password"
    override val recoveryPhrase = "Recovery Phrase"
    override val setupVault = "Setup Vault"
    override val setupDescription = "Please set a strong Master Password. This password is the only key to your vault."
    override val recoveryDescription = "If you forget your master password, these 12 words are the only way to access your vault. Keep them safe and NEVER share them."
    override val savedStart = "I SAVED IT, START"
    override val passwordRequirements = "Password Requirements"
    override val reqMinLength = "At least 12 characters"
    override val reqUpperLower = "At least one uppercase and one lowercase letter"
    override val reqDigit = "At least one digit"
    override val reqSpecial = "At least one special character (!@#$..)"
    override val passwordMismatch = "Passwords do not match"
    override val autoLock = "Auto Lock"
    override val autoLockSub = "When should the vault lock?"
    override val language = "Language / Dil"
    override val languageSub = "Change the application language"
    override val securityAudit = "Security Audit"
    override val securityAuditSub = "See your vault's health status"
    override val generator = "Password Generator"
    override val generatorSub = "Create strong passwords"
    override val trialExpired = "Trial Expired!"
    override val trialExpiredSub = "Please purchase a Premium license to access your vault."
    override val enterLicense = "Enter License Key"
    override val activateLicense = "ACTIVATE LICENSE"
    override val invalidLicense = "Invalid License Key!"
    override val copyId = "Copy ID"
    override val idCopied = "ID Copied"
    override val passwordCopied = "Password copied"
    override val securityScore = "Security Score"
    override val weak = "Weak"
    override val reused = "Reused"
    override val secure = "Secure"
    override val analyzing = "Analyzing your vault..."
    override val backup = "Backup & Import"
    override val import = "Import Data"
    override val export = "Export Data (CSV/JSON)"
    override val reset = "Reset Database"
    override val resetWarning = "THIS ACTION WILL PERMANENTLY DELETE ALL YOUR DATA!"
    override val resetConfirm = "I'm sure, Delete Everything"
    override val changePassword = "Change Master Password"
    override val oldPassword = "Old Password"
    override val newPassword = "New Password"
    override val backupPasswordTitle = "Set Backup Password"
    override val encryptAndSave = "ENCRYPT AND SAVE"
    override val backupSuccess = "Backup created successfully"
    override val backupError = "Backup error"
    override val importSuccess = "Data imported successfully"
    override val importError = "Import error"
    override val exportJson = "Export as JSON"
    override val exportCsv = "Export as CSV"
    override val importJson = "Import from JSON File"
    override val exportJsonSub = "Compatible Aegis JSON backup"
    override val exportCsvSub = "Unencrypted table format"
    override val importJsonSub = "Previous backup file"
    override val auditScoreDesc = "Your Security Score"
    override val titlePlaceholder = "Title (Ex: Google)"
    override val usernamePlaceholder = "Username / Email"
    override val passwordNew = "New Password (Optional)"
    override val totpPlaceholder = "2FA Secret (for TOTP)"
    override val websitePlaceholder = "Website (Optional)"
    override val attachmentPlaceholder = "Add file (max 15 MB)"
    override val supportedFormats = "Supported formats: PDF, ZIP, RAR, JPG, PNG, TXT, DOC, XLS and more"
    override val removeFile = "Remove File"
    override val totpTitle = "2FA VERIFICATION CODE"
    override val secondsSuffix = "s"
    override val attachmentDecryptError = "File decryption failed!"
    override val securityAnalysis = "Security Analysis"
    override val weakCaps = "WEAK"
    override val mediumCaps = "MEDIUM"
    override val strongCaps = "STRONG"
    override val excellentCaps = "EXCELLENT"
    override val unknownCaps = "UNKNOWN"
    override val usernameCopied = "Username copied"
    override val clipboardCleaned = "Clipboard cleaned for security"
    override val decryptError = "Error: Decryption failed"
    override val unlocking = "Decrypting Key..."
    override val vaultLocked = "Vault Locked"
    override val enterMasterPassword = "Enter Master Password"
    override val show = "Show"
    override val hide = "Hide"
    override val tooManyAttempts = "Too many failed attempts. Please wait:"
    override val wrongPassword = "Wrong password"
    override val remainingAttempts = "Attempts remaining"
    override val unlockWithPassword = "UNLOCK WITH PASSWORD"
    override val useBiometric = "Use Biometric"
    override val minuteSuffix = "m"
    override val premiumDescription = "Unlimited Security with Premium"
    override val premiumSubtitle = "Lifetime use with a one-time 15€ payment and hardware lock support on all devices."
    override val deviceIdLabel = "Your Device ID"
    override val cryptoPayments = "Crypto Payment Addresses"
    override val cryptoDescription = "Please send 15€ worth of crypto and email your TXID and Device ID to sales@hetech-me.space."
    override val addressCopied = "Address Copied"
    override val back = "Back"
    override val premiumAudit = "Premium Audit"
    override val premiumAuditDesc = "Upgrade to Premium to see which of your passwords are weak and which ones have been leaked."
    override val weakPasswordsCaps = "WEAK PASSWORDS"
    override val reusedPasswordsCaps = "REUSED PASSWORDS"
    override val easyToGuess = "Easy to guess"
    override val alsoUsedInOtherAccounts = "Also used in other accounts"
    override val refresh = "Refresh"
    override val length = "Length"
    override val includeUppercase = "Uppercase Letters (A-Z)"
    override val includeNumbers = "Numbers (0-9)"
    override val includeSymbols = "Symbols (!@#$)"
    override val useThisPassword = "USE THIS PASSWORD"
    override val exportJsonSuccess = "Vault exported as JSON"
    override val exportCsvSuccess = "Vault exported as CSV"
    override val importCount = "entries imported"
    override val importSource = "Import Source"
    override val importSourceDesc = "Import Source"
    override val aegisJsonRecommended = "Aegis JSON (Recommended)"
    override val reencrypting = "Re-encrypting all data..."
    override val reencryptingDesc = "This action will re-encrypt all your data with a new key."
    override val newPasswordReq = "New Password (Min 12 chars)"
    override val confirmNewPasswordLabel = "Confirm New Password"
    override val passwordChangedSuccess = "Master password updated successfully"
    override val wrongOldPassword = "Error: Wrong old password!"
    override val passwordMinLengthError = "Password must be at least 12 characters!"
    override val resetDatabaseTitle = "Reset Database?"
    override val resetDatabaseWarning = "All your passwords will be permanently deleted. This cannot be undone."
    override val immediately = "Immediately"
    override val minute1 = "1 Minute"
    override val minutes5 = "5 Minutes"
    override val minutes15 = "15 Minutes"
    override val never = "Never"
    override val freeTrial = "Free Trial Version"
    override val lifetimeLicenseActive = "Lifetime license active"
    override val trialDaysRemaining = "%d days left in trial"
    override val general = "General"
    override val security = "Security"
    override val reencryptTitle = "Change Password/Login"
    override val reencryptSub = "Update your master password"
    override val securityAuditItemSub = "Analyze your vault's overall health"
    override val exportEncryptedJson = "Export as Encrypted JSON"
    override val exportEncryptedJsonSub = "Backup protected with an extra password"
    override val dangerousArea = "Dangerous Area"
    override val resetDatabaseItem = "Completely Reset Vault"
    override val resetDatabaseItemSub = "Deletes all data and resets the app"
    override val continueCaps = "CONTINUE"
    override val creatingSecureKey = "Creating Secure Key..."
    override val computingArgon2 = "Computing Argon2..."
}

val LocalStrings = compositionLocalOf<Strings> { StringsTR }

# 🛡️ Aegis Vault - Next Gen Password Manager

![Version](https://img.shields.io/badge/version-1.1.0-blue.svg) ![Security](https://img.shields.io/badge/security-AES256--GCM-green.svg) ![Platform](https://img.shields.io/badge/platform-Android-brightgreen.svg) ![License](https://img.shields.io/badge/license-MIT-orange.svg)

**Aegis Vault**, modern encryption standards and a user-friendly design come together to provide the ultimate secure storage for your passwords, notes, and files. Developed with **Kotlin** and **Jetpack Compose**, it prioritizes **Local-First** privacy architecture.

> **Your Data, Your Control.** We use a Zero-Knowledge architecture, meaning we cannot access your data even if we wanted to. Everything is encrypted on your device.

---

## 🔥 Key Features

### 🔒 Military-Grade Encryption
*   **AES-256-GCM:** Used for encrypting all vault entries and attachments.
*   **Argon2id:** State-of-the-art Key Derivation Function (KDF) to protect your master password against brute-force attacks.
*   **Android KeyStore:** Hardware-backed storage for cryptographic keys.

### 📱 Modern & Secure UI
*   **Jetpack Compose:** Built with Google's latest UI toolkit for smooth and reliable performance.
*   **Biometric Login:** Unlock your vault instantly using Fingerprint or Face ID.
*   **Auto-Lock:** Automatically locks the app after a specified period of inactivity.
*   **Screenshot Prevention:** Blocks screen recording and screenshots within the app (`FLAG_SECURE`).

### 🚀 Advanced Tools
*   **Security Audit:** Analyzes your vault to find weak, reused, or compromised passwords.
*   **Password Generator:** Creates strong, unrecognizable passwords with customizable criteria.
*   **Secure Attachments:** Store sensitive files (IDs, documents) securely within the vault.
*   **Import/Export:**
    *   Easy migration from **Bitwarden** and **LastPass**.
    *   Secure Encrypted JSON backup & restore.
    *   CSV export for flexibility.

### 🌍 Multi-Language Support
*   English 🇺🇸
*   Turkish 🇹🇷
*   *(Dynamic language switching supported)*

---

## 💎 Premium Features
Unlock the full potential of Aegis Vault:
*   **Unlimited Entries:** Store as many passwords as you need.
*   **Hardware Security Module (HSM):** Enhanced key protection tailored to your device.
*   **Detailed Audit Report:** See exactly which passwords are putting you at risk.
*   **Lifecycle License:** One-time purchase, forever yours.

---

## 🛠️ Technology Stack

*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose (Material3)
*   **Architecture:** MVVM (Model-View-ViewModel)
*   **Cryptography:**
    *   `androidx.security:security-crypto`
    *   `javax.crypto` (AES/GCM/NoPadding)
    *   Argon2id implementation
*   **Local Data:** Room Database (SQLCipher support ready)

---

## 📸 Screenshots

| Login Screen | Vault | Security Audit | Settings |
|:---:|:---:|:---:|:---:|
| *(Add Screenshot)* | *(Add Screenshot)* | *(Add Screenshot)* | *(Add Screenshot)* |

---

## 📥 Installation

1.  Download the latest APK from the [Releases](https://github.com/hafgit99/AegisVaultAndroid/releases) section.
2.  Install on your Android device.
3.  Set a strong Master Password (this is the only key to your vault, **do not forget it!**).
4.  Write down your **Recovery Phrase**.

---

## 🤝 Contribution

Contributions are welcome!
1.  Fork the repository.
2.  Create a feature branch (`git checkout -b feature/AmazingFeature`).
3.  Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4.  Push to the branch (`git push origin feature/AmazingFeature`).
5.  Open a Pull Request.

---

## 📞 Contact & Support

For support, bug reports, or premium license inquiries:
📧 **sales@hetech-me.space**

---

*Verified Secure by Aegis Security Team.*

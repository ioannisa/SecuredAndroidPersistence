# Android Secure Persist Library
## Leverage your Preferences in Android
*by Ioannis Anifantakis*

Android Secure Persist Library is an Android library written purely in Kotlin. It is designed to provide secure and efficient storage of preferences in Android applications. By leveraging the Android KeyStore and modern encryption techniques, SecurePersist ensures that sensitive data is stored safely, protecting it from unauthorized access.

This library simplifies the process of encrypting and decrypting preferences using `SharedPreferences` and `DataStore`, making it easy for developers to implement secure storage solutions.

## Features
* **Secure Preferences Management:** Easily encrypt and decrypt preferences using `SharedPreferences` and `DataStore`.
* **Property Delegation:** Use Kotlin property delegation for seamless integration of encrypted preferences.
* **Raw Data Encryption:** Directly encrypt and decrypt raw data with EncryptionManager for additional flexibility.
* **Asynchronous Operations:** Efficiently handle preferences with non-blocking operations using DataStore.

## Why Use SecurePersist?
* **Security:** Protect sensitive data with robust encryption techniques.
* **Ease of Use:** Simplifies the process of managing encrypted preferences with a user-friendly API.
* **Versatility:** Supports a variety of data types and integrates seamlessly with existing Android components.
* **Performance:** Ensures non-blocking operations for a smooth user experience.

## Installation

There are two ways to install this library to your project

### Install by adding directly the module to your project
The directory `secure-persist` contains the module of this library, so you can
1. copy it to your root folder of the project (same as you see it in this repo folders)
2. at the bottom of your `settings.gradle` you tell android to treat the folder you copied as a module
```kotlin
rootProject.name = "My App Name"
include(":app")
include(":secure-persist") // <-- add this line so android knows this folder is a module
```

### Install as a Jitpack library to your dependencies

1. Add this to your dependencies
```kotlin
implementation("com.github.ioannisa:SecuredAndroidPersistence:1.0.5")
```

2. Add Jitpack as a dependencies repository in your `settings.gradle` (or at `app:build.gradle` for older Android projects) in order for this library to be able to download
```kotlin
repositories {
    google()
    mavenCentral()
    maven(url = "https://jitpack.io") // <-- add this line
}
```

## Usage

#### Provide SecurePersist library using Hilt
```kotlin
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import eu.anifantakis.lib.securepersist.encryption.EncryptionManager
import eu.anifantakis.lib.securepersist.PersistManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EncryptionModule {

    @Provides
    @Singleton
    fun provideEncryptedPersistence(@ApplicationContext context: Context): PersistManager = PersistManager(context, "myKeyAlias")

    @Provides
    @Singleton
    fun provideEncryptedManager(): EncryptionManager = EncryptionManager("myKeyAlias")
}
```

### PersistManager
`PersistManager` is the core component of SecurePersist. It manages encrypted preferences using both SharedPreferences and DataStore leverages the **EncryptionManager's** cryptographic algorithms.

#### Initialization directly without DI
```kotlin
// create a PersistManager instance (optionally give keyAlias)
val persistManager = PersistManager(context, "your_key_alias")
```

### SharedPreferences Encryption
Android Secure Persist offers a zero-configuration approach for encrypting and decrypting SharedPreferences. This means you can easily secure your SharedPreferences without additional setup.

#### SharedPreferences Example
```kotlin
// Encrypt and save a preference
persistManager.encryptSharedPreference("key1", "secureValue")

// Decrypt and retrieve a preference
val value: String = persistManager.decryptSharedPreference("key1", "defaultValue")

// Delete a preference
persistManager.deleteSharedPreference("key1")
```

#### SharedPreferences Property Delegation

This library allows you to use Kotlin property delegation for encrypted SharedPreferences, providing a clean and intuitive way to handle your encrypted preferences.

**Use encrypted SharedPreferences as normal variables using property delegation**

In this first example you can set the key of the sharedPreference manually
```kotlin
// create
var secureString by persistManager.preference("secureString", "default")
 
secureString = "newSecureValue"
val storedValue = secureString
```

But with Property Delegation this is the preferred way, where you don't neeed to specify key, the variable name will be the key-name behind the scenes.
```kotlin
// create
var secureString by persistManager.preference("default")

secureString = "newSecureValue"
val storedValue = secureString
```

### DataStore Encryption
Unlike SharedPreferences, DataStore does not natively support encryption. SecurePersist provides the missing functionality to securely handle DataStore preferences, ensuring your data is encrypted with the same zero-configuration approach.

#### DataStore Example
```kotlin
// Save a non-encrypted preference
persistManager.putDataStorePreference("key2", 123)

// Retrieve a non-encrypted preference
val number: Int = persistManager.getDataStorePreference("key2", 0)

// Encrypt and save a preference
persistManager.encryptDataStorePreference("key3", true)

// Decrypt and retrieve a preference
val flag: Boolean = persistManager.decryptDataStorePreference("key3", false)

// Delete a preference
persistManager.deleteDataStorePreference("key2")
```

### EncryptionManager
`EncryptionManager` provides additional functionality for encrypting and decrypting raw data.

#### Encryption Details
* **Algorithm:** AES (Advanced Encryption Standard)
* **Mode:** GCM (Galois/Counter Mode)
* **Padding:** No Padding (GCM handles it internally)
* **Key Management:** Managed by Android KeyStore
* **Key Strength:** 256-bit keys for strong encryption

AES in GCM mode is an authenticated encryption algorithm that provides both data confidentiality and integrity. This makes it highly secure and suitable for sensitive data. Using the Android KeyStore for key management adds an extra layer of security by storing keys in a secure, hardware-backed environment.

#### Encrypting and Decrypting Raw Data

```kotlin
val encryptionManager = EncryptionManager("your_key_alias")

// Encrypt data
val encryptedData = encryptionManager.encryptData("plainText")

// Decrypt data
val decryptedData = encryptionManager.decryptData(encryptedData)
val plainText = String(decryptedData, Charsets.UTF_8)

// Encrypt a value and encode it to a Base64 string
val encryptedValue = encryptionManager.encryptValue("valueToEncrypt")

// Decrypt a Base64 encoded string and return the original value
val decryptedValue = encryptionManager.decryptValue(encryptedValue, "defaultValue")
```

## Contributing
Contributions are welcome! Please open an issue or submit a pull request on GitHub.

## License
This project is licensed under the MIT License




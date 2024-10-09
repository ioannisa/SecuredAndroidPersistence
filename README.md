# Android Secure Persist Library
## Leverage your Persistence and Encryption in Android
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
3. At your App-Module's `build.gradle` file dependencies, add the module that your project contains
```kotlin
implementation(project(":secure-persist"))
```

### Install as a Jitpack library to your dependencies

[![](https://jitpack.io/v/ioannisa/SecuredAndroidPersistence.svg)](https://jitpack.io/#ioannisa/SecuredAndroidPersistence)

1. Add this to your dependencies
```kotlin
implementation("com.github.ioannisa:SecuredAndroidPersistence:1.0.12")
```

2. Add Jitpack as a dependencies repository in your `settings.gradle` (or at Project's `build.gradle` for older Android projects) in order for this library to be able to download
```kotlin
repositories {
    google()
    mavenCentral()
    maven(url = "https://jitpack.io") // <-- add this line
}
```

## Usage

#### Provide PersistManager and EncryptionManager using Hilt

Do it quickly, do it with Hilt

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

    /**
     * Provide a PersistManager that allows for out-of-the-box
     * 1. Encrypted SharedPreferences with property delegation
     * 2. with and Encrypted DataStore
     */
    @Provides
    @Singleton
    fun provideEncryptedPersistence(@ApplicationContext context: Context): PersistManager =
        PersistManager(context, "myKeyAlias")

    // EncryptionManager is being used internally by PersistManager.
    // So if all you want is encrypted Persistence, you don't have to provide EncryptionManager

    /**
     * Provide an EncryptionManager only if you want to allow for
     * advanced encryption/decryption of raw values
     */
    @Provides
    @Singleton
    fun provideEncryptedManager(@ApplicationContext context: Context): EncryptionManager {
        return EncryptionManager(context, "myKeyAlias")
}
```

Or do it with Koin

```kotlin
import eu.anifantakis.lib.securepersist.PersistManager
import eu.anifantakis.lib.securepersist.encryption.EncryptionManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val encryptionModule = module {

    /**
     * Provide a PersistManager that allows for out-of-the-box
     * 1. Encrypted SharedPreferences with property delegation
     * 2. with an Encrypted DataStore
     */
    single { 
        PersistManager(androidContext(), "myKeyAlias")
    }

    // EncryptionManager is being used internally by PersistManager.
    // So if all you want is encrypted Persistence, you don't have to provide EncryptionManager

    /**
     * Provide an EncryptionManager only if you want to allow for
     * advanced encryption/decryption of raw values
     */
    single<EncryptionManager> {
        EncryptionManager(androidContext(), "myKeyAlias")
    }
}
```


### PersistManager
`PersistManager` is the core component of SecurePersist. It manages encrypted preferences using both SharedPreferences and DataStore leverages the **EncryptionManager's** cryptographic algorithms.

#### Initialization

During initialization of persist manager, it also creates an instance of its own EncryptionManager to manage encryption and decryption of persist data.  If you don't need to encrypt and decrypt external data, other than SharedPreferences and DataStore Preferences, then you don't need to make an EncryptionManager instance of its own.

```kotlin
// create a PersistManager instance with custom KeyStore alias
val persistManager = PersistManager(context, "your_key_alias")

// create a PersistManager instance with "keyAlias" as default KeyStore alias
val persistManager = PersistManager(context)
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

It allows you to save your encryption key and pass it to a server, and thus also allows to pass during construction or with a setter such a key to use.  If you don't pass an external key, the library will create a custom key and push it to the KeyStore so it can be used as long as you don't uninstall your app.

Currently, the EncryptionManager will encrypt and decrypt the following types:
* `Boolean`
* `Int`
* `Float`
* `Long`
* `String`

for any other type it will throw an `IllegalArgumentException("Unsupported type")` exception.

So the `EncryptionManager` currently accepts the types that are also accepted on `SharedPreferences` and `DataStore`.

### Initialization

#### You can initialize using the `KeyStore`
```kotlin
val encryptionManager = EncryptionManager.withKeyStore("your_key_alias")
```

#### You can initialize using the `External Key`

First, generate an external key:
```kotlin
val externalKey = EncryptionManager.generateExternalKey()
```

Then, use the generated key to create an instance of `EncryptionManager`:
```kotlin
val encryptionManager = EncryptionManager.withExternalKey(externalKey)
```

#### Chaining Initialization
You can also initialize using a method chaining approach. This allows you to configure the `EncryptionManager` with both a key from the Android KeyStore and an external key if needed.
```kotlin
val encryptionManager = EncryptionManager
    .withKeyStore("myKeyAlias")
    .withExternalKey(EncryptionManager.generateExternalKey())
```

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

#### Encrypting and Decrypting Raw Data with an External Key

One important feature is the ability generate an external key, which you can then pass to the library.

By doing so you can safe-keep that key at some server in order to be able to make use of it when needed in the future.

```kotlin
// Generate an external key
val externalKey = EncryptionManager.generateExternalKey()

// Create an EncryptionManager instance with the external key
val encryptionManager = EncryptionManager
    .withKeyStore("myKeyAlias")
    .withExternalKey(externalKey)
```

Also you can supply that key at runtime
```kotlin
// Generate an external key
val externalKey = EncryptionManager.generateExternalKey()

// Create an EncryptionManager instance
val encryptionManager = EncryptionManager
    .withKeyStore("myKeyAlias")

// now that will replace the default key
encryptionManager.setExternalKey(externalKey)
```

You can supply an external also only for a specific entryption/decryption in Static context, leaving the default key for everything else
```kotlin
// Create an EncryptionManager instance
val encryptionManager = EncryptionManager
    .withKeyStore("myKeyAlias")

// Generate an external key
val externalKey = EncryptionManager.generateExternalKey()

// we will now use that key only for the specified encryptions/decryptions

// Encrypt a value and encode it to a Base64 string with custom key
val encryptedValue1 = EncryptionManager.encryptValue("valueToEncrypt", secretKey = externalKey)
// Encrypt a value and encode it to a Base64 string with default key
val encryptedValue2 = encryptionManager.encryptValue("valueToEncrypt")

// Decrypt a Base64 encoded string and return the original value with custom key
val decryptedValue1 = EncryptionManager.decryptValue(encryptedValue, "defaultValue", secretKey = externalKey)
// Decrypt a Base64 encoded string and return the original value with default key
val decryptedValue2 = encryptionManager.decryptValue(encryptedValue, "defaultValue")
```

#### Storing the externalKey (SecretKey) to some other medium
If you have generated a key and want to securely transmit it to some API or retrieve it, then this library provides two convenience static methods for `encoding` and `decoding` that key to a string so you can easily transfer it.

```kotlin
// generate a key
val originalKey = EncryptionManager.generateExternalKey()

// encrypt your data with that external key
val encryptedData = EncryptionManager.encryptData("Hello, Secure World!", originalKey)

// create a string that contains the encoded key (maybe then send it to some server)
val encodedKey: String = EncryptionManager.encodeSecretKey(originalKey)

// create another key from that encoded string (maybe you got that as a string from a server)
val decodedKey: SecretKey = EncryptionManager.decodeSecretKey(encodedKey)

// as you can see, you can decode the encrypted data using the key that was reconstructed from the encoded string
val decryptedText = EncryptionManager.decryptData(encryptedData, decodedKey)
```

## Testing
You can find extended tests inside the `androidTest` folder for both the PersistManager and the Encryption manager to have even more examples of their usage.

## Contributing
Contributions are welcome! Please open an issue or submit a pull request on GitHub.

## License
This project is licensed under the MIT License




# Android Secure Persist Library
## Leverage your Persistence and Encryption in Android
*by Ioannis Anifantakis*

Android Secure Persist Library is a pure Kotlin library designed to provide secure and efficient storage of preferences, complex data types, and files in Android applications. By leveraging the Android KeyStore and modern encryption techniques, SecurePersist ensures that sensitive data —including complex objects and files— is stored safely, protecting it from unauthorized access.

This library simplifies the process of encrypting and decrypting preferences using `SharedPreferences` and `DataStore`, supports serialization of complex data types, and provides robust file encryption capabilities, making it easy for developers to implement comprehensive secure storage solutions.





## Features
* **Secure Preferences Management:** Easily encrypt and decrypt preferences using `SharedPreferences` and `DataStore`.
* **Support for Complex Data Types:** Serialize and securely store complex objects, including custom classes and collections.
* **File Encryption and Decryption:** Securely encrypt and decrypt files, ensuring sensitive data remains protected even when stored externally.
* **Property Delegation:** Use Kotlin property delegation for seamless integration of encrypted preferences.
* **Raw Data Encryption:** Directly encrypt and decrypt raw data and files with `EncryptionManager` for additional flexibility.
* **Asynchronous Operations:** Efficiently handle preferences with non-blocking operations using DataStore.
* **External Key Management:** Use custom external keys for scenarios requiring cross-device data decryption or remote key retrieval.





## Why Use SecurePersist?
* **Security:** Protect sensitive data with robust encryption techniques, including complex objects and files.
* **Ease of Use:** Simplifies the process of managing encrypted preferences and data with a user-friendly API.
* **Versatility:** Supports a variety of data types, including primitives, complex objects, and files, integrating seamlessly with existing Android components.
* **Performance:** Ensures non-blocking operations for a smooth user experience.
* **Flexibility:** Allows for external key management, enabling secure data storage and retrieval across devices or from remote servers.





## Installation

There are two ways to install this library to your project

### Option 1: Add the Module Directly to Your Project
The directory `secure-persist` contains the module of this library, so you can
1. Copy the secure-persist directory from this repository into the root folder of your project.
2. Include the Module at the bottom of your `settings.gradle` 
```kotlin
rootProject.name = "My App Name"
include(":app")
include(":secure-persist") // <-- add this line so android knows this folder is a module
```
3. At your App-Module's `build.gradle` file dependencies, add the module that your project contains
```kotlin
implementation(project(":secure-persist"))
```

### Option 2: Use JitPack to Add as a Dependency

[![](https://jitpack.io/v/ioannisa/SecuredAndroidPersistence.svg)](https://jitpack.io/#ioannisa/SecuredAndroidPersistence)

1. Add this to your dependencies
```kotlin
implementation("com.github.ioannisa:SecuredAndroidPersistence:1.0.13")
```

2. Add Jitpack as a dependencies repository in your `settings.gradle` (or at Project's `build.gradle` for older Android projects) so this library is able to download
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
    fun provideEncryptedManager(@ApplicationContext context: Context): EncryptionManager =
        EncryptionManager(context, "myKeyAlias")
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
`PersistManager` is the core component of SecurePersist. It manages encrypted preferences using both `SharedPreferences` and `DataStore`, leveraging the `EncryptionManager`'s cryptographic algorithms. It now supports serialization and encryption of complex data types, including custom objects and collections.


#### Initialization
When initializing `PersistManager`, it creates an instance of its own `EncryptionManager` to handle encryption and decryption of persistent data. If you don't need to encrypt and decrypt external data beyond preferences, you don't need a separate `EncryptionManager` instance.




```kotlin
// Create a PersistManager instance with a custom KeyStore alias
val persistManager = PersistManager(context, "your_key_alias")

// Create a PersistManager instance with the default KeyStore alias ("keyAlias")
val persistManager = PersistManager(context)

```

### SharedPreferences Encryption
SecurePersist offers a zero-configuration approach for encrypting and decrypting SharedPreferences, now including complex data types.


#### Securely Storing and Retrieving Primitive Types with SharedPreferences
```kotlin
// Encrypt and save a preference
persistManager.encryptSharedPreference("key1", "secureValue")

// Decrypt and retrieve a preference
val value: String = persistManager.decryptSharedPreference("key1", "defaultValue")

// Delete a preference
persistManager.deleteSharedPreference("key1")
```


#### Securely Storing and Retrieving Primitive Types with DataStore

SecurePersist extends encryption capabilities to `DataStore`, supporting both primitive and complex data types. `DataStore` provides asynchronous, non-blocking operations, making it suitable for handling preferences without affecting the main thread.

Storing and Retrieving Primitive Types with DataStore
Since `DataStore` operations are suspend functions, you need to call them within a coroutine or another suspend function.

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Encrypt and save a preference
CoroutineScope(Dispatchers.IO).launch {
    persistManager.encryptDataStorePreference("key1", "secureValue")
}

// Decrypt and retrieve a preference
CoroutineScope(Dispatchers.IO).launch {
    val value: String = persistManager.decryptDataStorePreference("key1", "defaultValue")
    println("Retrieved value: $value")
}

// Delete a preference
CoroutineScope(Dispatchers.IO).launch {
    persistManager.deleteDataStorePreference("key1")
}
```

#### Note on Coroutines
* **Dispatchers.IO:** Used for IO-bound operations.
* **CoroutineScope:** Manages the lifecycle of coroutines. Ensure you handle coroutine scopes appropriately to avoid memory leaks.


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

Currently, the EncryptionManager will encrypt and decrypt **FILES** and the following types:
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
val encryptionManager = EncryptionManager("your_key_alias")
```

#### You can initialize using the `External Key`

First, generate an external key:
```kotlin
val externalKey = EncryptionManager.generateExternalKey()
```

Then, use the generated key to create an instance of `EncryptionManager`:
```kotlin
val encryptionManager = EncryptionManager(externalKey)
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
val encryptionManager = EncryptionManager(externalKey)
```

Also you can supply that key at runtime
```kotlin
// Generate an external key
val externalKey = EncryptionManager.generateExternalKey()

// Create an EncryptionManager instance
val encryptionManager = EncryptionManager("myKeyAlias")
```

You can supply an external also only for a specific entryption/decryption in Static context, leaving the default key for everything else
```kotlin
// Create an EncryptionManager instance
val encryptionManager = EncryptionManager("myKeyAlias")

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

## FILES Encryption/Decription ##
```kotlin
// Encrypt the file from file system
encryptionManager.encryptFile(testFile, encryptedFileName)

// Decrypt the file
val decryptedContent: ByteArray = encryptionManager.decryptFile(encryptedFileName)
val decryptedText = String(decryptedContent)
```

## Testing
You can find extended tests inside the `androidTest` folder for both the PersistManager and the Encryption manager to have even more examples of their usage.

## Contributing
Contributions are welcome! Please open an issue or submit a pull request on GitHub.

## License
This project is licensed under the MIT License




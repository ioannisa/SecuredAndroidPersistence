# Android Secure Persist Library
## Leverage your Persistence and Encryption in Android
*by Ioannis Anifantakis*

In modern mobile apps, safeguarding user data is crucial. Whether it's user credentials, API tokens, or any sensitive information, SecurePersist allows developers to implement robust encryption and persistence with minimal effort. Imagine needing to securely store users' authentication tokens in a finance app—this library will help you do that efficiently while keeping the process simple.

Android Secure Persist Library is a pure Kotlin library designed to provide secure and efficient storage of preferences, complex data types, and files in Android applications. By leveraging the Android KeyStore and modern encryption techniques, SecurePersist ensures that sensitive data—including complex objects and files—is stored safely, protecting it from unauthorized access.


This library simplifies the process of encrypting and decrypting preferences using `SharedPreferences` and `DataStore`, supports serialization of complex data types, and provides robust file encryption capabilities, making it easy for developers to implement comprehensive secure storage solutions.

## Features
* **Secure Preferences Management:** Easily encrypt and decrypt preferences using `SharedPreferences` and `DataStore`.
* **Support for Complex Data Types:** Serialize and securely store complex objects, including custom classes and collections.
* **File Encryption and Decryption:** Securely encrypt and decrypt files, ensuring sensitive data remains protected even when stored externally.
* **Property Delegation:** Use Kotlin property delegation for seamless integration of encrypted preferences.
* **Annotation Support:** Utilize `@SharedPref` and `@DataStorePref` annotations for convenient preference management.
* **Raw Data Encryption:** Directly encrypt and decrypt raw data and files with `EncryptionManager` for additional flexibility.
* **Asynchronous Operations:** Efficiently handle preferences with non-blocking operations using DataStore.
* **External Key Management:** Use custom external keys for scenarios requiring cross-device data decryption or remote key retrieval.

## Why Use SecurePersist?
* **Security:** Protect sensitive data with robust encryption techniques, including complex objects and files.
* **Ease of Use:** Simplifies the process of managing encrypted preferences and data with a user-friendly API.
* **Versatility:** Supports a variety of data types, including primitives, complex objects, and files, integrating seamlessly with existing Android components.
* **Performance:** Ensures non-blocking operations for a smooth user experience.
* **Flexibility:** Allows for external key management, enabling secure data storage and retrieval across devices or from remote servers.

---

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
implementation("com.github.ioannisa:SecuredAndroidPersistence:2.2.0-beta1")
```

2. Add Jitpack as a dependencies repository in your `settings.gradle` (or at Project's `build.gradle` for older Android projects) so this library is able to download
```kotlin
repositories {
    google()
    mavenCentral()
    maven(url = "https://jitpack.io") // <-- add this line
}
```

---

# PersistManager

`PersistManager` is the core component of SecurePersist. It manages encrypted preferences using both `SharedPreferences` and `DataStore`, leveraging the `EncryptionManager`'s cryptographic algorithms.  It supports serialization and encryption of complex data types, including custom objects and collections.

In version 2.0 of the library, `PersistManager` has undergone a major refactor, introducing several breaking changes compared to previous versions. This documentation focuses on the latest `PersistManager` approaches.

### Initialization
When initializing `PersistManager`, it creates an instance of its own `EncryptionManager` to handle encryption and decryption of persistent data. If you don't need to encrypt and decrypt external data beyond preferences, you don't need a separate `EncryptionManager` instance.

When initializing the `PersistManager`, you can optionally define a KeyStore alias. If you don't provide one, a default alias (`"keyAlias"`) will be created for you. This `keyAlias` is used by the KeyStore as an identifier for the pool of keys you store, so even if you don't define a `keyAlias`, it is totally fine.

```kotlin
// Create a PersistManager instance with a custom KeyStore alias
val persistManager = PersistManager(context, "your_key_alias")

// Create a PersistManager instance with the default KeyStore alias ("keyAlias")
val persistManager = PersistManager(context)
```

---

## Preference Management with PersistManage

`PersistManager` provides a unified and flexible way to handle encrypted preferences using both `SharedPreferences` and `DataStore`. It offers property delegation and supports property annotations for even more convenient preference management.

### Property Annotations
You can use the following annotations to specify where and how your preferences should be stored:

* `@SharedPref`: Indicates that the property should be stored in `SharedPreferences`.
* `@DataStorePref`: Indicates that the property should be stored in `DataStore`. By default, DataStore preferences are encrypted.

```kotlin
@SharedPref
var mySharedPref by persistManager.annotatedPreference("default value")

@DataStorePref
var myDataStorePref by persistManager.annotatedPreference("default value")
```

### Using `annotatedPreference` property delegate
The `annotatedPreferenc`e function creates a preference delegate that handles properties annotated with `@SharedPref` or `@DataStorePref`. This function provides a unified way to create preferences that can be stored either in SharedPreferences or DataStore, based on the annotation used on the property.

**Important:**

* This function can only be used for property declarations at the class level due to reflection restrictions.
* For use within function bodies, use the `preference` function instead.

**Usage:**
**1. With SharedPreferences:**
```kotlin
// assuming key as the variable name
@SharedPref
var myKey by persistManager.annotatedPreference("default value")

// or with a custom key
@SharedPref(key = "myKey")
var myVariable by persistManager.annotatedPreference("default value")
```

**2. With DataStore:**
```kotlin
// assuming key as the variable name and assuming encryption
@DataStorePref
var myKey by persistManager.annotatedPreference("default value")

// or with a custom key and disabling encryption
@DataStorePref(key = "customKey", encrypted = false)
var myVariable by persistManager.annotatedPreference("default value")
```

**Notes:**
* If no key is specified in the annotation, the property name is used as the key.
* DataStore preferences are encrypted by default. Use `encrypted = false` to store in plain text.


### Using `preference` property delegate
The `preference` function allows you to create a preference delegate without the need for property annotations. This is suitable for use within function bodies or when annotations cannot be used.

**Usage:**
**1. Using SharedPreferences:**
```kotlin
// assuming key to be the variable name (myKey) 
// assuming storage to be SharedPreferences
var myKey by persistManager.preference( "default value")

// declaring the key to be "myKey"
// assuming storage to be SharedPreferences
var myPref by persistManager.preference("default value", "myKey")

// declaring the key to be "myKey"
// declaring Storage to SharedPreferences
var myPref by persistManager.preference(
    defaultValue = "default value",
    key = "myKey",
    storage = PersistManager.Storage.SHARED_PREFERENCES
)
```

**1. Using DataStore Preferences:**
```kotlin
// declaring the key to be "myKey"
// declaring Storage to Encrypted DataStore Preferences
var myPref by persistManager.preference(
    defaultValue = "default value",
    key = "myKey",
    storage = PersistManager.Storage.DATA_STORE_ENCRYPTED
)

// declaring the key to be "myKey"
// declaring Storage to Unencrypted DataStore Preferences
var myPref by persistManager.preference(
    defaultValue = "default value",
    key = "myKey",
    storage = PersistManager.Storage.DATA_STORE
)
```

**Notes:**
* If key is null or empty, the property name will be used as the key.
* When using DataStore, you can specify whether the data should be encrypted by choosing the appropriate StorageType.

#### `Storage` Enum
The `Storage` enum represents the types of storage available for preferences:
* `SHARED_PREFERENCES`: Store preference in `SharedPreferences`.
* `DATA_STORE_ENCRYPTED`: Store preference in encrypted `DataStore`.
* `DATA_STORE`: Store preference in unencrypted `DataStore`.

## PersistManager - SharedPreferences Encryption without delegation
`SecurePersist` offers a zero-configuration approach for encrypting and decrypting `SharedPreferences`, now including complex data types.

#### Securely utilizing SharedPreferences directly
```kotlin
// Encrypt and save a preference
persistManager.sharedPrefs.put("key1", "secureValue")

// Decrypt and retrieve a preference
val value: String = persistManager.sharedPrefs.get("key1", "defaultValue")

// Delete a preference
persistManager.sharedPrefs.delete("key1")
```

#### Handling complex data types
```kotlin
data class AuthInfo(
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresIn: Long = 0L
)

// Create encrypted shared preference and store it with an initial value
var authInfo by persistManager.preference(AuthInfo())

// Update authInfo as if it was a normal variable
authInfo = AuthInfo(
    accessToken = "token123",
    refreshToken = "refresh123",
    expiresIn = 3600L
)

// Retrieve the encrypted shared preference
println(authInfo)

// Equivalent to accessing the preference directly
println(persistManager.sharedPrefs.get("authInfo", AuthInfo()))
```

To handle `Double` and complex data types, the library uses serialization via the `gson` library, while standard types supported by `SharedPreferences` avoid serialization.

| Data Type | Supported Directly | Handled via gson Serialization |
|-----------|:------------------:|:------------------------------:|
| Boolean   | Yes                | No                             |
| Int       | Yes                | No                             |
| Float     | Yes                | No                             |
| Long      | Yes                | No                             |
| Double    | No                 | Yes                            |
| String    | Yes                | No                            |
| Custom Objects (e.g., Data Classes) | No | Yes                  |

## PersistManager - DataStore Preferences Encryption without delegation

Using `DataStore preferences` with this library is similar to accessing SharedPreferences. You can make direct use of `DataStore` or use property delegation. DataStore operations can be performed using coroutines (as DataStore naturally operates) or without coroutines, with the library internally managing the coroutine calls on your behalf.

**Note:** For DataStore preferences, encryption is enabled by default unless specified otherwise.

#### Option 1 - Accessing DataStore with `Coroutines`

**Related Functions:**
* `put`
* `get`
* `delete`

SecurePersist extends encryption capabilities to `DataStore`, supporting both primitive and complex data types. Since `DataStore` operations are suspend functions, you need to call them within a coroutine or another suspend function.

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Encrypt and save a preference
CoroutineScope(Dispatchers.IO).launch {
    persistManager.dataStorePrefs.put("key1", "secureValue")
}

// Decrypt and retrieve a preference
CoroutineScope(Dispatchers.IO).launch {
    val value = persistManager.dataStorePrefs.get("key1", "defaultValue")
    println("Retrieved value: $value")
}

// Delete a preference
CoroutineScope(Dispatchers.IO).launch {
    persistManager.dataStorePrefs.delete("key1")
}
```

#### Option 2 - Accessing DataStore Without Coroutines
To access DataStore preferences without coroutines, use the `Direct` versions of the functions.

**Related Functions:**
* `putDirect` (non-blocking)
* `getDirect` (blocking)
* `deleteDirect` (non-blocking)


```kotlin
// assuming encryption - Encrypt and store in a non-blocking way to DataStore
persistManager.dataStorePrefs.putDirect("key1", "secureValue")

// assuming encryption - Decrypt and get in a blocking way the value from DataStore
val value: String = persistManager.dataStorePrefs.getDirect("key1", "defaultValue")

// no encryption - store unencrypted in a non-blocking way to DataStore
persistManager.dataStorePrefs.putDirect("key1", "secureValue", encrypted = false)

// no encryption - get unencrypted in a blocking way the value from DataStore
val value: String = persistManager.dataStorePrefs.getDirect("key1", "defaultValue", encrypted = false)

// Delete the DataStore preference without using coroutines
persistManager.dataStorePrefs.deleteDirect("key1")
```

#### Handling Complex Data Types


##### DataStore handling for unencrypted data

When storing to `DataStore` without encryption, `serialization` is only required for Double and complex data types, as DataStore supports primitive types directly.

| Data Type | Supported Directly | Handled via gson Serialization |
|-----------|:------------------:|:------------------------------:|
| Boolean   | Yes                | No                             |
| Int       | Yes                | No                             |
| Float     | Yes                | No                             |
| Long      | Yes                | No                             |
| Double    | No                 | Yes                            |
| String    | Yes                | No                            |
| Custom Objects (e.g., Data Classes) | No | Yes                  |

##### DataStore handling for encrypted data

When storing to `DataStore` with encryption enabled, `serialization` is performed on all data types because DataStore doesn't support encryption natively. During the encryption phase, everything gets serialized and stored as an encrypted string.

| Data Type | Supported Directly | Handled via gson Serialization |
|-----------|:------------------:|:------------------------------:|
| Boolean   | No                 | Yes                            |
| Int       | No                 | Yes                            |
| Float     | No                 | Yes                            |
| Long      | No                 | Yes                            |
| Double    | No                 | Yes                            |
| String    | No                 | Yes                           |
| Custom Objects (e.g., Data Classes) | No | Yes                  |

### Deleting Preferences with PersistManager
You can delete preferences using the `delete` function from inside the PersistManager or utilize the delete functions of the equivalent storage types like that:

```kotlin
// Delete a preference from both SharedPreferences and DataStore
persistManager.delete("key1")

// Delete a preference from SharedPreferences
persistManager.sharedPrefs.delete("key1")

// Delete a preference from DataStore Preferences using coroutines
CoroutineScope(Dispatchers.IO).launch {
    persistManager.dataStorePrefs.delete("key1")
}

// Delete a preference from DataStore Preferences directly without coroutines
persistManager.dataStorePrefs.deleteDirect("key1")
```

---

# EncryptionManager
`EncryptionManager`  provides additional functionality for encrypting and decrypting raw data, files, and complex objects.

It allows you to save your encryption key and pass it to a server, and thus also allows to pass during construction or with a setter such a key to use.  If you don't pass an external key, the library will create a custom key and push it to the KeyStore so it can be used as long as you don't uninstall your app.

### Initialization

#### You can initialize using the `KeyStore`
```kotlin
val encryptionManager = EncryptionManager(context, "your_key_alias")
```

#### You can initialize using the `External Key`

First, generate an external key:
```kotlin
val externalKey = EncryptionManager.generateExternalKey()
```

Then, use the generated key to create an instance of `EncryptionManager`:
```kotlin
val encryptionManager = EncryptionManager(context, externalKey)
```

#### Encryption Details
* **Algorithm:** AES (Advanced Encryption Standard)
* **Mode:** GCM (Galois/Counter Mode)
* **Padding:** No Padding (GCM handles it internally)
* **Key Management:** Managed by Android KeyStore
* **Key Strength:** 256-bit keys for strong encryption

AES in GCM mode is an authenticated encryption algorithm that provides both data confidentiality and integrity. Using the Android KeyStore for key management adds an extra layer of security by storing keys in a secure, hardware-backed environment.

#### Encrypting and Decrypting Raw Data

```kotlin
val encryptionManager = EncryptionManager(context, "your_key_alias")

// Encrypt data
val encryptedData = encryptionManager.encryptData("plainText")

// Decrypt data
val decryptedData = encryptionManager.decryptData(encryptedData)
val plainText = String(decryptedData, Charsets.UTF_8)

// Encrypt a value and encode it to a Base64 string
val encryptedValue = encryptionManager.encryptValue("valueToEncrypt")

// Decrypt a Base64 encoded string and return the original value
val decryptedValue: String = encryptionManager.decryptValue(encryptedValue, "defaultValue")

```

#### Encrypting and Decrypting Raw Data with an External Key
One important feature is the ability generate an external key, which you can then pass to the library.

By doing so, you can safe-keep that key on a server to be able to decrypt data when needed in the future.

```kotlin
// Generate an external key
val externalKey = EncryptionManager.generateExternalKey()

// Create an EncryptionManager instance with the external key
val encryptionManager = EncryptionManager(externalKey)
```

Also you can supply that key at runtime  only for a specific entryption/decryption in Static context, leaving the keystore key for everything else
```kotlin
// Create an EncryptionManager instance
val encryptionManager = EncryptionManager(context, "myKeyAlias")

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

##### Exporting custom key to save it to some server
```kotlin
// generate a key
val originalKey = EncryptionManager.generateExternalKey()

// encrypt your data with that external key
val encryptedData = EncryptionManager.encryptData("Hello, Secure World!", originalKey)

// create a string that contains the encoded key (then send it to some server)
val encodedKey: String = EncryptionManager.encodeSecretKey(originalKey)
```

##### Retrieving the custom key from the server to use it o the app
```kotlin
// construct a SecretKey from that encodedKey retrieved from the server
val decodedKey: SecretKey = EncryptionManager.decodeSecretKey(encodedKey)

// as you can see, you can decode the encrypted data using the key that was reconstructed from the encoded string
val decryptedText = EncryptionManager.decryptData(encryptedData, decodedKey)
```

## FILES Encryption/Decription ##
```kotlin
// Create an EncryptionManager instance
val encryptionManager = EncryptionManager(context, "your_key_alias")

// Specify the input file and the name for the encrypted file
val testFile = File(context.filesDir, "plain.txt")
val encryptedFileName = "encrypted.dat"

// Encrypt the file
encryptionManager.encryptFile(testFile, encryptedFileName)

// Decrypt the file
val decryptedContent: ByteArray = encryptionManager.decryptFile(encryptedFileName)
val decryptedText = String(decryptedContent)
```

---

# Testing
You can find extensive tests inside the `androidTest` folder for both the `PersistManager` and the `EncryptionManager`, providing more examples and ensuring reliability. 

---

## Contributing
Contributions are welcome! Please open an issue or submit a pull request on GitHub.

---

## License
This project is licensed under the MIT License

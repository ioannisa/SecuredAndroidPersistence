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
implementation("com.github.ioannisa:SecuredAndroidPersistence:2.0.0-beta1")
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
`PersistManager` is the core component of SecurePersist. It manages encrypted preferences using both `SharedPreferences` and `DataStore`, leveraging the `EncryptionManager`'s cryptographic algorithms. It now supports serialization and encryption of complex data types, including custom objects and collections.

In the version 2.0 of the library the `PersistManager` has taken a big refactor, introducing several breaking changes to the previous versions.  This documentation focuses on the latest PersistManager approaches.


### Initialization
When initializing `PersistManager`, it creates an instance of its own `EncryptionManager` to handle encryption and decryption of persistent data. If you don't need to encrypt and decrypt external data beyond preferences, you don't need a separate `EncryptionManager` instance.

When initializing the `PersistManager` you can optionally define a `Keystore alias`.  If you don't provide one, a default one will be created for you.  This `KeyAlias` is being used by keystore as an indentifier for pool of keys you store, so even if you don't define a `KeyAlias` is totally fine.

```kotlin
// Create a PersistManager instance with a custom KeyStore alias
val persistManager = PersistManager(context, "your_key_alias")

// Create a PersistManager instance with the default KeyStore alias ("keyAlias")
val persistManager = PersistManager(context)

```

---

### PersistManager - SharedPreferences Encryption
`SecurePersist` offers a zero-configuration approach for encrypting and decrypting `SharedPreferences`, now including complex data types.

Putting and Getting values to the SharedPreferences using PersistManager utilizes a zero configuration setup for `EncryptedSharedPreferences` under the hood for both direct or delegated access to each preference.

#### Securely utilizing SharedPreferences directly
```kotlin
// Encrypt and save a preference
persistManager.sharedPrefs.put("key1", "secureValue")

// Decrypt and retrieve a preference
val value: String = persistManager.sharedPrefs.get("key1", "defaultValue")

// Delete a preference
persistManager.sharedPrefs.delete("key1")
```

#### Securely utilizing SharedPreferences with Property-Delegation

To utilize delegation you can make use of the `encryptedSharedPreferenceDelegate` function.

```kotlin
// Encrypt and save a shared preference - the name of the variable becomes the key
var key1 by persistManager.encryptedSharedPreferenceDelegate("secureValue")

// just like that you can update the encrypted shared as if it was a variable
key1 = "a new value"

// Decrypt and retrieve a shared preference
// as simple as simply accessing the value
print(key1)

// To delete a shared preference has to happen without delegation
persistManager.sharedPrefs.delete("key1")
```

```kotlin
// Encrypt and save a shared preference - applying second string parameter becomes the key
// so you can use whatever variable name for your delegated preference you want
var myPreference by persistManager.encryptedSharedPreferenceDelegate("secureValue", "key1")

// Decrypt and retrieve a shared preference
// as simple as simply accessing the value
print(myPreference)

// To delete a shared preference has to happen without delegation
persistManager.sharedPrefs.delete("key1")
```

#### Handling complex data types
```kotlin
data class AuthInfo(
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresIn: Long = 0L
)

// create encrypted shared prefference and store it with an initial value
var authInfo by persistManager.encryptedSharedPreferenceDelegate(AuthInfo())

// just as if it was a normal variable, you can change authInfo
// and it will get encrypted under the "authInfo" key at EncryptedSharedPreferences
authInfo = AuthInfo(
    accessToken = "token123",
    refreshToken = "refresh123",
    expiresIn = 3600L
)

// this retrieves the encryptedSharedPreference
print(authInfo)

// and infact the above is the equivalent to this
print(persistManager.sharedPrefs.get("authInfo")
```

It is important to note that to handle Double and Complex DataTypes, the library uses serialization via the `gson` library behind the scenes, while for the standard types that are supported by SharedPreferences we avoid serialization. 

| Data Type | Supported Directly | Handled via gson Serialization |
|-----------|:------------------:|:------------------------------:|
| Boolean   | Yes                | No                             |
| Int       | Yes                | No                             |
| Float     | Yes                | No                             |
| Long      | Yes                | No                             |
| Double    | No                 | Yes                            |
| String    | Yes                | No                            |
| Custom Objects (e.g., Data Classes) | No | Yes                  |

---

## PersistManager - DataStore Preferences Encryption

Making use of the DataStore preferences has no difference for this library from accessing EncrytedSharedPreferences.  Infact you can make again direct usage or property delegated usage of the DataStore Preferences.  Not only that, but you can use the DataStore version of the preferences normally using coroutines (as data store uses anyway), or without using coroutines (as the fucntion call you do internally launches coroutine on your behalf).

Allowing to use the DataStore Preferences without including it in a coroutine, allows it to work with the exactly same property delegation as with EncryptedSharedPreferences, thus it allows you to use DataStore Preferences out of the box and with the option to apply the same Encryption algorithm as with EncryptedSharedPreferences out of the box.

Thus this library also complements DataStore Preferences with these two new functionalities not found by default in DataStore.
* Ability to run outside coroutine (optional)
* Ability to automatically store Encrypted data (optiona, default enabled)

**Note:** For DataStore Preferences, the encryption is standard, unless specified to not use encryption as extra parameter in the `put`, `get`, `delete` functions

### Option 1 - Accessing DataStore with coroutines

SecurePersist extends encryption capabilities to `DataStore`, supporting both primitive and complex data types. `DataStore` provides asynchronous, non-blocking operations, making it suitable for handling preferences without affecting the main thread.

Storing and Retrieving objects with DataStore
Since `DataStore` operations are suspend functions, you need to call them within a coroutine or another suspend function.

Related suspended functions:
* `put`
* `get` 
* `delete`

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
    val value: String = persistManager.dataStorePrefs.get("key1", "defaultValue")
    println("Retrieved value: $value")
}

// Delete a preference
CoroutineScope(Dispatchers.IO).launch {
    persistManager.dataStorePrefs.delete("key1")
}
```

### Option 2 - Accessing DataStore Without coroutines

Related functions:
* `putDirect` (non-blocking)
* `getDirect` (blocking)
* `deleteDirect` (non-blocking)

Accessing DataStore Preferences without coroutines happens with the `Direct` version of the same functions that were used in the coroutines approach.

```kotlin
// encrypt and store in non blocking way to DataStore the "value" for the given "key"
persistManager.dataStorePrefs.putDirect("key1", "secureValue")

// decrypt and get in a blocking way the value held by the "key" from the DataStore
val value: String = persistManager.dataStorePrefs.getDirect("key1", "defaultValue")

// deletes the DataStore Preference without using coroutines
persistManager.dataStorePrefs.deleteDirect("key1")
```


Storing to `DataStore Preferences` **WITHOUT encryption** performs serialization only for Double and for Complex DataTypes, since DataStore already knows to to store these types, while Double and Complex datatypes get serialized using gson library to get stored.

| Data Type | Supported Directly | Handled via gson Serialization |
|-----------|:------------------:|:------------------------------:|
| Boolean   | Yes                | No                             |
| Int       | Yes                | No                             |
| Float     | Yes                | No                             |
| Long      | Yes                | No                             |
| Double    | No                 | Yes                            |
| String    | Yes                | No                             |
| Custom Objects (e.g., Data Classes) | No | Yes                  |



Storing to `DataStore Preferences` **WITH encryption** performs serialization to every object because DataStore doesn't support encryption, and during the encryption phase it al gets serialized and stored as an encrypted string.

| Data Type | Supported Directly | Handled via gson Serialization |
|-----------|:------------------:|:------------------------------:|
| Boolean   | No                 | Yes                            |
| Int       | No                 | Yes                            |
| Float     | No                 | Yes                            |
| Long      | No                 | Yes                            |
| Double    | No                 | Yes                            |
| String    | No                 | Yes                            |
| Custom Objects (e.g., Data Classes) | No | Yes                  |
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

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
implementation("com.github.ioannisa:SecuredAndroidPersistence:1.1.3-beta")
```

2. Add Jitpack as a dependencies repository in your `settings.gradle` (or at Project's `build.gradle` for older Android projects) so this library is able to download
```kotlin
repositories {
    google()
    mavenCentral()
    maven(url = "https://jitpack.io") // <-- add this line
}
```

# PersistManager
`PersistManager` is the core component of SecurePersist. It manages encrypted preferences using both `SharedPreferences` and `DataStore`, leveraging the `EncryptionManager`'s cryptographic algorithms. It now supports serialization and encryption of complex data types, including custom objects and collections.


### Initialization
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


## Securely Storing and Retrieving Primitive Types with DataStore

### Option 1 - Accessing DataStore with coroutines

Related functions:
* `encryptDataStorePreference`
* `decryptDataStorePreference`

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

##### Note on Coroutines
* **Dispatchers.IO:** Used for IO-bound operations.
* **CoroutineScope:** Manages the lifecycle of coroutines. Ensure you handle coroutine scopes appropriately to avoid memory leaks.
 
###  Complex Objects
You can store and retrieve complex objects by serializing them to JSON and encrypting the JSON string.

### Option 2 - Accessing DataStore Without coroutines

Related functions:
* `encryptDataStorePreferenceSync`
* `decryptDataStorePreferenceSync`

The library allows you to make use of the DataStore without having to use coroutines (it uses coroutines behind the scenes). 

It stores in a non-blocking way your preference to DataStore, while retrieving it in a blocking way (same as SharedPreferences do anyway).

This allows for a more refined and easier way to use DataStore, same as with SharedPreferences.

```kotlin


// encrypt and store in non blocking way to DataStore the "value" for the given "key"
persistManager.encryptDataStorePreferenceSync("key", "value")

// decrypt and get in a blocking way the value held by the "key" from the DataStore
val value: String = persistManager.decryptDataStorePreferenceSync("key", "defaultValue")
```



## Storing and retrieving Object:

This is a sample object
```kotlin
data class User(
    val id: Int, 
    val name: String, 
    val email: String
)

val user = User(1, "John Doe", "john.doe@example.com")
```

#### Securely Storing and Retrieving using SharedPreferences:
```kotlin
// encrypt and store using SharedPreferences
persistManager.putObjectSharedPreference("user_key", user)

// retrieve
val retrievedUser: User? = persistManager.getObjectSharedPreference("user_key")
retrievedUser?.let {
    println("User ID: ${it.id}, Name: ${it.name}, Email: ${it.email}")
}
```

#### Securely Storing and Retrieving using DataStore:
```kotlin
// store using DataStore
CoroutineScope(Dispatchers.IO).launch {
    persistManager.putObjectDataStorePreference("user_key", user)
}

// retrieve using DataStore
CoroutineScope(Dispatchers.IO).launch {
    val retrievedUser: User? = persistManager.getObjectDataStorePreference("user_key")
    retrievedUser?.let {
        println("User ID: ${it.id}, Name: ${it.name}, Email: ${it.email}")
    }
}
```


## SharedPreferences Property Delegation

Use Kotlin **property delegatio**n for encrypted `SharedPreferences`, providing a clean and intuitive way to handle your encrypted preferences.

**Use encrypted SharedPreferences as if they were normal variables using property delegation**

##### Using Property Delegation with a Custom Key:

 you can just add or read values to the secureString as if it was a normal value,  however setting values will put them in sharedPreferences securely.
```kotlin
// Create a delegated property with "key1" as the key used in shared-preferences
var secureString by persistManager.preference("key1", "defaultValue")

secureString = "newSecureValue"
val storedValue = secureString
```

But you can take this a step further.  If you don't set a custom Key the variable name is used as a key instead, so if you call your variable "secureString" then that is the key name used behind the scenes also in your sharedPreferences.
```kotlin
// Create a delegated property using the variable name as the key
var secureString by persistManager.preference("defaultValue")

secureString = "newSecureValue"
val storedValue = secureString
```


#### Property Delegation on Custom Types
```kotlin
data class AuthInfo(
    val accessToken: String = "",
    val refreshToken: String = "",
    val userId: Int = 0
)

// create authInfo1 which is assigned to "authInfoKey" with Default Value AuthInfo()
var authInfo1 by persistManager.preference("authInfoKey", AuthInfo())
// update authInfo1 with new accessToken
authInfo1 = authInfo1.copy(accessToken = "newAccessToken")

// retrieve authInfo2 from "authInfoKey"
val authInfo2 by persistManager.preference("authInfoKey", AuthInfo())

// Assertions
assertEquals(authInfo2.accessToken, "newAccessToken")
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

## DataTypes Supported
`PersistnManager` and `EncryptionManager` provide support to the following data types:

* `Boolean`
* `Int`
* `Float`
* `Double`
* `Long`
* `String`

for any other type the library will attempt to serialize the given input using `gson` internally.

| Data Type | Supported Directly | Handled via gson Serialization |
|-----------|:------------------:|:------------------------------:|
| Boolean   | Yes                | N/A                            |
| Int       | Yes                | N/A                            |
| Float     | Yes                | N/A                            |
| Long      | Yes                | N/A                            |
| Double    | Yes                | N/A                            |
| String    | Yes                | N/A                            |
| Custom Objects (e.g., Data Classes) | No | Yes                  |

Note: Custom objects, such as data classes, are not directly supported but are handled through serialization.

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

# Use Cases

### 1. Securely Storing User Preferences
Using `PersistManager`, you can securely store user preferences, such as authentication tokens, settings, or any sensitive information.

##### With SharedPreferences:
```kotlin
// Storing a user token securely
persistManager.encryptSharedPreference("user_token", "your_token_here")

// Retrieving the user token
val userToken: String = persistManager.decryptSharedPreference("user_token", "")
```

##### With DataStore:
```kotlin
CoroutineScope(Dispatchers.IO).launch {
    // Storing a user token securely
    persistManager.encryptDataStorePreference("user_token", "your_token_here")

    // Retrieving the user token
    val userToken: String = persistManager.decryptDataStorePreference("user_token", "")
}
```

### 2. Securely Storing Complex Objects
You can store complex data structures, such as user profiles or app configurations.

```kotlin
data class UserProfile(
    val id: Int, 
    val name: String, 
    val email: String, 
    val roles: List<String>
)

val userProfile = UserProfile(1, "John Doe", "john.doe@example.com", listOf("admin", "editor"))
```

##### With SharedPreferences:
```kotlin
persistManager.putObjectSharedPreference("user_profile", userProfile)

// Later retrieve it
val retrievedProfile: UserProfile? = persistManager.getObjectSharedPreference("user_profile")
```

##### With DataStore:
```kotlin
CoroutineScope(Dispatchers.IO).launch {
    persistManager.putObjectDataStorePreference("user_profile", userProfile)
    val retrievedProfile: UserProfile? = persistManager.getObjectDataStorePreference("user_profile")
}
```

### 3. Encrypting Files Before Uploading
If you need to upload files to a server but want to ensure they are encrypted before transmission, you can use `EncryptionManager` to encrypt the file, then upload the encrypted file.

```kotlin
val encryptionManager = EncryptionManager(context, "your_key_alias")

// Encrypt the file
val inputFile = File(context.filesDir, "sensitive_data.txt")
val encryptedFileName = "sensitive_data_encrypted.dat"
encryptionManager.encryptFile(inputFile, encryptedFileName)

// Now upload 'sensitive_data_encrypted.dat' to your server
```

### 4. Decrypting Files Received from a Server
When you receive an encrypted file from a server, you can decrypt it using `EncryptionManager`.
```kotlin
// Assuming you've downloaded 'sensitive_data_encrypted.dat' from the server
val decryptedContent: ByteArray = encryptionManager.decryptFile("sensitive_data_encrypted.dat")
val decryptedText = String(decryptedContent)

// Use the decrypted content as needed
```

### 5. Cross-Device Data Encryption Using External Keys
If you need to encrypt data on one device and decrypt it on another, you can use external keys.
```kotlin
// On Device A - Generate external key and encrypt data
val externalKey = EncryptionManager.generateExternalKey()
val encodedKey = EncryptionManager.encodeSecretKey(externalKey)

// Store or transmit 'encodedKey' securely to Device B

// Encrypt data with the external key
val encryptedData = EncryptionManager.encryptData("Sensitive information", externalKey)

// Store or transmit 'encryptedData' to Device B

// On Device B - Decode external key and decrypt data
val decodedKey = EncryptionManager.decodeSecretKey(encodedKey)

// Decrypt data
val decryptedData = EncryptionManager.decryptData(encryptedData, decodedKey)
val sensitiveInfo = String(decryptedData)
```

### 6. Secure Backup and Restore
Use `EncryptionManager` to encrypt user data before backing up to cloud storage, ensuring that even if the backup is accessed by unauthorized parties, the data remains secure.
```kotlin
// Serialize user data to JSON
val userDataJson = gson.toJson(userData)

// Encrypt the data
val encryptedUserData = encryptionManager.encryptData(userDataJson)

// Convert to Base64 string for storage
val encryptedUserDataString = Base64.encodeToString(encryptedUserData, Base64.NO_WRAP)

// Store 'encryptedUserDataString' in cloud storage

// To restore, retrieve the string from cloud storage and decrypt
val encryptedData = Base64.decode(encryptedUserDataString, Base64.NO_WRAP)
val decryptedJson = encryptionManager.decryptData(encryptedData)

// Deserialize back to user data object
val restoredUserData = gson.fromJson(decryptedJson, UserData::class.java)
```

# Testing
You can find extensive tests inside the `androidTest` folder for both the `PersistManager` and the `EncryptionManage`r, providing more examples and ensuring reliability. Tests cover scenarios including:

* Encryption and decryption of primitive types with `SharedPreferences` and `DataStore`.
* Serialization, encryption, and decryption of complex objects with both storage mechanisms.
* File encryption and decryption.
* Using external keys for encryption and decryption.




## Contributing
Contributions are welcome! Please open an issue or submit a pull request on GitHub.

## License
This project is licensed under the MIT License

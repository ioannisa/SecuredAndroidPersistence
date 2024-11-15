package eu.anifantakis.lib.securepersist.compose

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.structuralEqualityPolicy
import eu.anifantakis.lib.securepersist.PersistManager
import eu.anifantakis.lib.securepersist.SecurePersistSolution
import eu.anifantakis.lib.securepersist.internal.DataStoreManager
import eu.anifantakis.lib.securepersist.internal.SharedPreferencesManager
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A property delegate that manages a persisted mutable state in Jetpack Compose applications.
 *
 * This class provides a bridge between Compose's [MutableState] and persistent storage mechanisms,
 * enabling automatic persistence of state changes. It supports multiple storage backends including
 * SharedPreferences and DataStore, with optional encryption.
 *
 * The state is lazily initialized when first accessed and automatically persists changes when the
 * state value is modified. Thread safety is ensured during initialization and state updates.
 *
 * Example usage:
 * ```
 * class MyViewModel {
 *     private val persistManager = PersistManager(context)
 *
 *     // Using DataStore with encryption
 *     private var counter by persistManager.dataStore.mutableStateOf(0)
 *
 *     // Using SharedPreferences
 *     private var userName by persistManager.sharedPrefs.mutableStateOf("Guest")
 * }
 * ```
 *
 * @param T The type of the state value. Must be compatible with the chosen storage mechanism.
 * @property defaultValue The default value used when no persisted value exists.
 * @property key The storage key. If null, the property name will be used as the key.
 * @property storage The storage backend to use ([Storage.SHARED_PREFERENCES], [Storage.DATA_STORE], or [Storage.DATA_STORE_ENCRYPTED]).
 * @property snapshotPolicy Policy determining when state updates should trigger recomposition.
 * @property securePersistSolution The storage implementation to use for persistence.
 *
 * @throws IllegalArgumentException If the storage key cannot be determined.
 * @throws ClassCastException If the persisted value cannot be cast to type T.
 *
 * @see MutableState
 * @see SecurePersistSolution
 */
class PersistedMutableState<T>(
    private val defaultValue: T,
    private val key: String?,
    private val storage: Storage,
    private val snapshotPolicy: SnapshotMutationPolicy<T>,
    private val securePersistSolution: SecurePersistSolution
) : MutableState<T>, ReadWriteProperty<Any?, T> {

    /**
     * Holds the property name if [key] is not provided.
     */
    private var propertyName: String? = null
    private var isInitialized = false

    /**
     * Computes the preference key using [key] or the property's name.
     *
     * @throws IllegalArgumentException If neither [key] nor the property's name is available.
     */
    private val preferenceKey: String
        get() = key ?: propertyName ?: throw IllegalArgumentException("Key cannot be null")

    /**
     * The internal [MutableState] holding the current value.
     */
    private var _state: MutableState<T> =
        mutableStateOf(defaultValue, policy = snapshotPolicy)

    /**
     * The current value of the state.
     */
    override var value: T
        get() {
            if (!isInitialized && propertyName != null) {
                synchronized(this) {
                    if (!isInitialized) {
                        // Initialize with persisted value outside of the snapshot
                        _state = mutableStateOf(getPersistedValue(), policy = snapshotPolicy)
                        isInitialized = true
                    }
                }
            }
            return _state.value
        }
        set(newValue) {
            if (!snapshotPolicy.equivalent(_state.value, newValue)) {
                _state.value = newValue
                saveValue(newValue)
            }
        }

    /**
     * Retrieves the value of the delegated property.
     *
     * If the property's name hasn't been set yet, it initializes [propertyName] and
     * updates the internal state with the persisted value.
     *
     * @param thisRef The reference to the property owner.
     * @param property The metadata for the property.
     * @return The current value of the state.
     */
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (propertyName == null) {
            propertyName = property.name
        }
        return value
    }

    /**
     * Sets the value of the delegated property.
     *
     * If the property's name hasn't been set yet, it initializes [propertyName] and
     * updates the internal state with the persisted value before setting the new value.
     *
     * @param thisRef The reference to the property owner.
     * @param property The metadata for the property.
     * @param value The new value to set.
     */
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (propertyName == null) {
            propertyName = property.name
        }
        this.value = value
    }

    /**
     * Retrieves the persisted value from the specified storage.
     *
     * @return The persisted value if it exists; otherwise, the [defaultValue].
     */
    private fun getPersistedValue(): T {
        return when (storage) {

            Storage.SHARED_PREFERENCES -> {
                (securePersistSolution as SharedPreferencesManager).get(preferenceKey, defaultValue)
            }
            Storage.DATA_STORE, Storage.DATA_STORE_ENCRYPTED -> {
                (securePersistSolution as DataStoreManager).getDirect(
                    preferenceKey,
                    defaultValue,
                    storage == Storage.DATA_STORE_ENCRYPTED
                )
            }

        }
    }

    /**
     * Saves the given [value] to the specified storage.
     *
     * @param value The value to persist.
     */
    private fun saveValue(value: T) {
        when (storage) {
            Storage.SHARED_PREFERENCES -> {
                (securePersistSolution as SharedPreferencesManager).put(preferenceKey, value)
            }
            Storage.DATA_STORE, Storage.DATA_STORE_ENCRYPTED -> {
                (securePersistSolution as DataStoreManager).putDirect(
                    preferenceKey,
                    value,
                    storage == Storage.DATA_STORE_ENCRYPTED
                )
            }
        }
    }

    /**
     * Destructuring operator function for Compose state.
     *
     * @return The current value of the state.
     */
    override fun component1(): T = value

    /**
     * Destructuring operator function for Compose state.
     *
     * @return A lambda that sets the value of the state.
     */
    override fun component2(): (T) -> Unit = { value = it }

    /**
     * Enum representing the types of storage available for preferences.
     */
    enum class Storage {
        /**
         * Store preference in SharedPreferences.
         */
        SHARED_PREFERENCES,
        /**
         * Store preference in encrypted DataStore.
         */
        DATA_STORE_ENCRYPTED,
        /**
         * Store preference in unencrypted DataStore.
         */
        DATA_STORE
    }
}

/**
 * Provides a property delegate for a persisted mutable state.
 *
 * This extension function on [PersistManager] enforces the use of property delegation
 * via the `by` keyword. Attempting to use it without delegation will result in a
 * compilation error, ensuring that the state is correctly managed and persisted.
 *
 * @param T The type of the state value.
 * @param defaultValue The default value of the state if no persisted value exists.
 * @param key The key used for persisting the state. If `null`, the property's name is used.
 * @param policy The policy to determine state equality. Defaults to structural equality.
 *
 * @return A [PropertyDelegateProvider] that must be used with the `by` keyword for property delegation.
 *
 * @throws IllegalArgumentException If both [key] and the property's name are `null`.
 */
inline fun <reified T> DataStoreManager.mutableStateOf(
    defaultValue: T,
    key: String? = null,
    encrypted: Boolean = true,
    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy()
): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>> {
    return PropertyDelegateProvider { thisRef, property ->
        val preferenceKey = key ?: property.name
        PersistedMutableState(
            defaultValue = defaultValue,
            key = preferenceKey,
            storage = if (encrypted) PersistedMutableState.Storage.DATA_STORE_ENCRYPTED else PersistedMutableState.Storage.DATA_STORE,
            snapshotPolicy = policy,
            securePersistSolution = this
        )
    }
}

/**
 * Provides a property delegate for a persisted mutable state.
 *
 * This extension function on [PersistManager] enforces the use of property delegation
 * via the `by` keyword. Attempting to use it without delegation will result in a
 * compilation error, ensuring that the state is correctly managed and persisted.
 *
 * @param T The type of the state value.
 * @param defaultValue The default value of the state if no persisted value exists.
 * @param key The key used for persisting the state. If `null`, the property's name is used.
 * @param policy The policy to determine state equality. Defaults to structural equality.
 *
 * @return A [PropertyDelegateProvider] that must be used with the `by` keyword for property delegation.
 *
 * @throws IllegalArgumentException If both [key] and the property's name are `null`.
 */
inline fun <reified T> SharedPreferencesManager.mutableStateOf(
    defaultValue: T,
    key: String? = null,
    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy()
): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>> {
    return PropertyDelegateProvider { thisRef, property ->
        val preferenceKey = key ?: property.name
        PersistedMutableState(
            defaultValue = defaultValue,
            key = preferenceKey,
            snapshotPolicy = policy,
            storage = PersistedMutableState.Storage.SHARED_PREFERENCES,
            securePersistSolution = this
        )
    }
}

// allows for property delegation TOGETHER with direct handling via "value" property

//inline fun <reified T> PersistManager.mutableStateOf(
//    defaultValue: T,
//    key: String? = null,
//    storage: PersistManager.Storage = PersistManager.Storage.SHARED_PREFERENCES,
//    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy()
//): PersistedMutableState<T> {
//    return PersistedMutableState(
//        defaultValue = defaultValue,
//        key = key,
//        storage = storage,
//        snapshotPolicy = policy,
//        persistManager = this
//    )
//}



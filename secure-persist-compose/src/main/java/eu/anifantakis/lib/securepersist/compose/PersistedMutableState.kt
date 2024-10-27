package eu.anifantakis.lib.securepersist.compose

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.structuralEqualityPolicy
import eu.anifantakis.lib.securepersist.PersistManager
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PersistedMutableState<T>(
    private val defaultValue: T,
    private val key: String?,
    private val storage: PersistManager.Storage,
    private val snapshotPolicy: SnapshotMutationPolicy<T>,
    private val persistManager: PersistManager
) : MutableState<T>, ReadWriteProperty<Any?, T> {

    // Will hold the property name if key is not provided
    private var propertyName: String? = null

    // Computes the preference key, using key or property name
    private val preferenceKey: String
        get() = key ?: propertyName ?: throw IllegalArgumentException("Key cannot be null")

    // Initialize _state with defaultValue; will update it later
    private var _state: MutableState<T> = mutableStateOf(defaultValue, policy = snapshotPolicy)

    override var value: T
        get() = _state.value
        set(newValue) {
            if (!snapshotPolicy.equivalent(_state.value, newValue)) {
                _state.value = newValue
                saveValue(newValue)
            }
        }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (propertyName == null) {
            propertyName = property.name
            // Now we can get the persisted value using the correct key
            val persistedValue = getPersistedValue()
            if (!snapshotPolicy.equivalent(_state.value, persistedValue)) {
                _state.value = persistedValue
            }
        }
        return value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (propertyName == null) {
            propertyName = property.name
            // Now we can get the persisted value using the correct key
            val persistedValue = getPersistedValue()
            if (!snapshotPolicy.equivalent(_state.value, persistedValue)) {
                _state.value = persistedValue
            }
        }
        this.value = value
    }

    private fun getPersistedValue(): T {
        return when (storage) {
            PersistManager.Storage.SHARED_PREFERENCES -> {
                persistManager.sharedPrefs.get(preferenceKey, defaultValue)
            }
            PersistManager.Storage.DATA_STORE, PersistManager.Storage.DATA_STORE_ENCRYPTED -> {
                persistManager.dataStorePrefs.getDirect(
                    preferenceKey,
                    defaultValue,
                    storage == PersistManager.Storage.DATA_STORE_ENCRYPTED
                )
            }
        }
    }

    private fun saveValue(value: T) {
        when (storage) {
            PersistManager.Storage.SHARED_PREFERENCES -> {
                persistManager.sharedPrefs.put(preferenceKey, value)
            }
            PersistManager.Storage.DATA_STORE, PersistManager.Storage.DATA_STORE_ENCRYPTED -> {
                persistManager.dataStorePrefs.putDirect(
                    preferenceKey,
                    value,
                    storage == PersistManager.Storage.DATA_STORE_ENCRYPTED
                )
            }
        }
    }

    // For Compose State
    override fun component1(): T = value
    override fun component2(): (T) -> Unit = { value = it }
}


inline fun <reified T> PersistManager.persistedMutableStateOf(
    defaultValue: T,
    key: String? = null,
    storage: PersistManager.Storage = PersistManager.Storage.SHARED_PREFERENCES,
    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy()
): PersistedMutableState<T> {
    return PersistedMutableState(
        defaultValue = defaultValue,
        key = key,
        storage = storage,
        snapshotPolicy = policy,
        persistManager = this
    )
}

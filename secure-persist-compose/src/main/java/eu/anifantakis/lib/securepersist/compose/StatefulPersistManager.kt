package eu.anifantakis.lib.securepersist.compose

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.structuralEqualityPolicy
import eu.anifantakis.lib.securepersist.PersistManager
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class StatefulPersistManager(private val persistManager: PersistManager) {

    inner class PersistedMutableState<T>(
        private val defaultValue: T,
        private val key: String?,
        private val storage: PersistManager.Storage = PersistManager.Storage.SHARED_PREFERENCES,
    ) : MutableState<T>, ReadWriteProperty<Any?, T> {

        private val snapshotPolicy: SnapshotMutationPolicy<T> = structuralEqualityPolicy()

        private var preferenceKey: String? = null
        private var _state: MutableState<T>? = null

        override var value: T
            get() {
                if (_state == null) {
                    initializeState()
                }
                return _state!!.value
            }
            set(newValue) {
                if (_state == null) {
                    initializeState()
                }
                if (!snapshotPolicy.equivalent(_state!!.value, newValue)) {
                    _state!!.value = newValue
                    saveValue(newValue)
                }
            }

        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            if (preferenceKey == null) {
                preferenceKey = key ?: property.name
            }
            return value
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            if (preferenceKey == null) {
                preferenceKey = key ?: property.name
            }
            this.value = value
        }

        private fun initializeState() {
            if (preferenceKey == null) {
                throw IllegalStateException("Preference key is not set.")
            }
            val persistedValue = getPersistedValue()
            _state = mutableStateOf(persistedValue, policy = snapshotPolicy)
        }

        private fun getPersistedValue(): T {
            val prefKey = preferenceKey ?: throw IllegalStateException("Preference key is not set.")

            return when (storage) {
                PersistManager.Storage.SHARED_PREFERENCES -> {
                    persistManager.sharedPrefs.get(prefKey, defaultValue)
                }
                PersistManager.Storage.DATA_STORE, PersistManager.Storage.DATA_STORE_ENCRYPTED -> {
                    persistManager.dataStorePrefs.getDirect(
                        prefKey,
                        defaultValue,
                        storage == PersistManager.Storage.DATA_STORE_ENCRYPTED
                    )
                }
            }
        }

        private fun saveValue(value: T) {
            val prefKey = preferenceKey ?: throw IllegalStateException("Preference key is not set.")
            when (storage) {
                PersistManager.Storage.SHARED_PREFERENCES -> {
                    persistManager.sharedPrefs.put(prefKey, value)
                }
                PersistManager.Storage.DATA_STORE, PersistManager.Storage.DATA_STORE_ENCRYPTED -> {
                    persistManager.dataStorePrefs.putDirect(
                        prefKey,
                        value,
                        storage == PersistManager.Storage.DATA_STORE_ENCRYPTED
                    )
                }
            }
        }

        override fun component1(): T = value
        override fun component2(): (T) -> Unit = { value = it }
    }

    inline fun <reified T> persistedMutableState(
        defaultValue: T,
        key: String? = null,
        storage: PersistManager.Storage = PersistManager.Storage.SHARED_PREFERENCES,
    ): PersistedMutableState<T> {
        return PersistedMutableState(
            defaultValue = defaultValue,
            key = key,
            storage = storage,
        )
    }
}

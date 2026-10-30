package com.atvantiq.wfms.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/* Lists a form builds up or is offered, kept in the SavedStateHandle as JSON (see SavedStateExt.kt). */

/**
 * A plain `var` list (the sites offered for a date, the types picked), kept in the handle as JSON.
 * Change it by assigning a new list: a list edited in place is not saved.
 */
inline fun <reified T : Any> SavedStateHandle.savedListValue(key: String): ReadWriteProperty<Any?, List<T>> =
    savedListValue(key, T::class.java)

fun <T : Any> SavedStateHandle.savedListValue(key: String, itemType: Class<T>): ReadWriteProperty<Any?, List<T>> =
    object : ReadWriteProperty<Any?, List<T>> {
        private var cache: List<T>? = null

        override fun getValue(thisRef: Any?, property: KProperty<*>): List<T> =
            cache ?: (get<String>(key)?.let { stateGson.fromJson(it, arrayOf(itemType)).toList() } ?: emptyList())
                .also { cache = it }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: List<T>) {
            cache = value
            this@savedListValue[key] = stateGson.toJson(value)
        }
    }

/** Like [savedListValue], for a property that is declared as a nullable `ArrayList` (assign a new list to change it). */
inline fun <reified T : Any> SavedStateHandle.savedArrayList(key: String): ReadWriteProperty<Any?, ArrayList<T>?> =
    savedArrayList(key, T::class.java)

fun <T : Any> SavedStateHandle.savedArrayList(key: String, itemType: Class<T>): ReadWriteProperty<Any?, ArrayList<T>?> =
    object : ReadWriteProperty<Any?, ArrayList<T>?> {
        private var cache: ArrayList<T>? = null

        override fun getValue(thisRef: Any?, property: KProperty<*>): ArrayList<T>? =
            cache ?: get<String>(key)?.let { ArrayList(stateGson.fromJson(it, arrayOf(itemType)).toList()) }
                .also { cache = it }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: ArrayList<T>?) {
            cache = value
            this@savedArrayList[key] = value?.let { stateGson.toJson(it) }
        }
    }

/** A list the form builds up (added expenses, picked sites), kept in the handle as JSON. */
inline fun <reified T : Any> SavedStateHandle.savedList(key: String): MutableLiveData<List<T>> =
    savedList(key, T::class.java)

fun <T : Any> SavedStateHandle.savedList(key: String, itemType: Class<T>): MutableLiveData<List<T>> =
    SavedLiveList(this, key, itemType)

private class SavedLiveList<T : Any>(
    private val handle: SavedStateHandle,
    private val key: String,
    itemType: Class<T>
) : MutableLiveData<List<T>>(
    handle.get<String>(key)?.let { stateGson.fromJson(it, arrayOf(itemType)).toList() } ?: emptyList()
) {
    override fun setValue(value: List<T>?) {
        handle[key] = stateGson.toJson(value.orEmpty())
        super.setValue(value)
    }

    override fun postValue(value: List<T>?) {
        handle[key] = stateGson.toJson(value.orEmpty())
        super.postValue(value)
    }
}

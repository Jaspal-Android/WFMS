package com.atvantiq.wfms.base

import androidx.databinding.ObservableField
import androidx.lifecycle.SavedStateHandle
import com.google.gson.Gson
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/*
 * What a form has typed or picked must survive Android ending the app's process while the user is
 * in the camera or another app. These helpers keep a ViewModel's form state in its
 * SavedStateHandle: every change is written through, and a recreated ViewModel starts from what
 * was saved. Plain values (String, Boolean, Long, ...) go in directly; lists and objects are saved
 * as JSON, so the model classes need no changes.
 */

internal val stateGson = Gson()

/** The array class for [itemType]; Gson needs it to read a list back as the real row type, not as maps. */
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> arrayOf(itemType: Class<T>): Class<Array<T>> =
    java.lang.reflect.Array.newInstance(itemType, 0).javaClass as Class<Array<T>>

/** An [ObservableField] for a form field. [T] must be a type a Bundle can hold (String, Boolean, Long, ...). */
fun <T> SavedStateHandle.savedField(key: String, initial: T): ObservableField<T> =
    SavedObservableField(this, key, if (contains(key)) get<T>(key) as T else initial)

private class SavedObservableField<T>(
    private val handle: SavedStateHandle,
    private val key: String,
    start: T
) : ObservableField<T>(start) {
    override fun set(value: T) {
        super.set(value)
        handle[key] = value
    }
}

/** A plain `var` of a Bundle-safe type, kept in the handle: `var projectId: Long? by handle.savedValue("projectId", null)`. */
fun <T> SavedStateHandle.savedValue(key: String, initial: T): ReadWriteProperty<Any?, T> =
    object : ReadWriteProperty<Any?, T> {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(thisRef: Any?, property: KProperty<*>): T =
            if (contains(key)) get<T>(key) as T else initial

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            this@savedValue[key] = value
        }
    }

/** A nullable object (a picked client, a picked site), kept in the handle as JSON. */
inline fun <reified T : Any> SavedStateHandle.savedObject(key: String): ReadWriteProperty<Any?, T?> =
    savedObject(key, T::class.java)

fun <T : Any> SavedStateHandle.savedObject(key: String, type: Class<T>): ReadWriteProperty<Any?, T?> =
    object : ReadWriteProperty<Any?, T?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T? =
            get<String>(key)?.let { stateGson.fromJson(it, type) }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
            this@savedObject[key] = value?.let { stateGson.toJson(it) }
        }
    }

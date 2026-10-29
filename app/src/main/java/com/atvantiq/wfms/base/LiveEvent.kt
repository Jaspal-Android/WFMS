package com.atvantiq.wfms.base

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * LiveData for something that happens once: a tap that opens a screen, a validation message, a
 * finished login. Plain LiveData hands its last value to every new observer, so after a
 * rotation or a return to the screen the same dialog, toast or navigation would run again.
 *
 * A value posted here is delivered once. If the screen is not active when it is posted (a
 * dialog is on top, the app is in the background) it waits and is delivered when the screen
 * becomes active again. A recreated screen never receives an old one.
 *
 * The event goes to a single observer, so observe it from one place per screen.
 */
class LiveEvent<T> : MutableLiveData<T>() {

    private val pending = AtomicBoolean(false)

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner) { value ->
            if (pending.compareAndSet(true, false)) observer.onChanged(value)
        }
    }

    @MainThread
    override fun setValue(value: T?) {
        pending.set(true)
        super.setValue(value)
    }

    override fun postValue(value: T?) {
        pending.set(true)
        super.postValue(value)
    }
}

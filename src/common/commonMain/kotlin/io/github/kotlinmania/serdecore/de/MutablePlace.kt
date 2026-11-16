package io.github.kotlinmania.serdecore.de

/** A caller-owned slot used by the Rust mutable-place deserialization contract. */
interface MutablePlace<T> {
    var value: T
}

/** Retains access to the caller's slot instead of copying its current value. */
fun <T> mutablePlace(getValue: () -> T, setValue: (T) -> Unit): MutablePlace<T> =
    object : MutablePlace<T> {
        override var value: T
            get() = getValue()
            set(value) = setValue(value)
    }

package io.github.recrafter.lapis.extensions

fun <T> MutableCollection<T>.addIfNotNull(element: T?) {
    element?.let { add(it) }
}

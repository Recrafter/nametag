package io.github.recrafter.lapis.extensions

import kotlin.reflect.KClass

val KClass<out Annotation>.atName: String
    get() = "@$simpleName"

package io.github.recrafter.lapis.annotations.accessors

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Accessor(
    val target: KClass<*>,
    val widener: String = ""
)

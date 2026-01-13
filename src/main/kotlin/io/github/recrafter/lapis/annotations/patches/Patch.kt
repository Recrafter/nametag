package io.github.recrafter.lapis.annotations.patches

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Patch(val target: KClass<*>)

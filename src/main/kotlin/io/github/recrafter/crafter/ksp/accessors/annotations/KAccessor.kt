package io.github.recrafter.crafter.ksp.accessors.annotations

import kotlin.reflect.KClass

/**
 * Marks an interface as a Kotlin-based accessor definition.
 *
 * Interfaces annotated with [KAccessor] are processed by the Crafter KSP Accessors processor.
 * The processor generates a mixin interface and the corresponding Kotlin bindings for
 * the specified [target] class.
 *
 * @property widener Optional fully qualified class name of a widener that exposes the target
 *                   if it is not accessible.
 * @property target  The class that this accessor refers to.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class KAccessor(
    val widener: String = "",
    val target: KClass<*> = Any::class,
)

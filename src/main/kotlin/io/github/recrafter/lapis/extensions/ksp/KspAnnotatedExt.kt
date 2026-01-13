package io.github.recrafter.lapis.extensions.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.Dependencies
import kotlin.reflect.KClass

@OptIn(KspExperimental::class)
fun KspAnnotated.hasAnnotation(annotation: KClass<out Annotation>): Boolean =
    isAnnotationPresent(annotation)

@OptIn(KspExperimental::class)
inline fun <reified A : Annotation> KspAnnotated.hasAnnotation(): Boolean =
    hasAnnotation(A::class)

@OptIn(KspExperimental::class)
inline fun <reified A : Annotation> KspAnnotated.getSingleAnnotationOrNull(): A? =
    getAnnotationsByType(A::class).singleOrNull()

fun Iterable<KspAnnotated>.toDependencies(aggregating: Boolean = false): Dependencies {
    val containingFiles = mapNotNull { it.containingFile }
    return if (containingFiles.isNotEmpty()) {
        Dependencies(aggregating, *containingFiles.toTypedArray())
    } else {
        Dependencies(aggregating)
    }
}

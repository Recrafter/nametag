package io.github.recrafter.nametag.extensions.poets.kotlin

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FileSpec

inline fun <reified A : Annotation> FileSpec.Builder.addAnnotation(builder: AnnotationSpec.Builder.() -> Unit = {}) {
    addAnnotation(buildKotlinAnnotation<A>(builder))
}

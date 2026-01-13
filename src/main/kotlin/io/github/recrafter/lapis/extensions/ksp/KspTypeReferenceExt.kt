package io.github.recrafter.lapis.extensions.ksp

import com.google.devtools.ksp.symbol.KSTypeReference
import io.github.recrafter.lapis.kj.KJTypeName

val KSTypeReference.qualifiedName: String
    get() = resolve().declaration.requireQualifiedName()

fun KSTypeReference.asKJTypeName(): KJTypeName =
    resolve().asKJTypeName()

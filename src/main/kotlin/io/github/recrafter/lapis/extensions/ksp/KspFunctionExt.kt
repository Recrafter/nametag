package io.github.recrafter.lapis.extensions.ksp

import com.squareup.kotlinpoet.UNIT
import io.github.recrafter.lapis.extensions.kp.asKJTypeName
import io.github.recrafter.lapis.kj.KJTypeName

val KspFunction.isConstructor: Boolean
    get() = name == "<init>"

fun KspFunction.getReturnTypeOrNull(): KJTypeName? =
    returnType?.asKJTypeName().takeIf { it != UNIT.asKJTypeName() }

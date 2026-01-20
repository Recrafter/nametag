package io.github.recrafter.lapis.extensions.ksp

import com.squareup.kotlinpoet.UNIT
import io.github.recrafter.lapis.extensions.kp.asKJTypeName
import io.github.recrafter.lapis.kj.KJTypeName
import io.github.recrafter.lapis.utils.Descriptors

val KspFunction.isConstructor: Boolean
    get() = name == Descriptors.CONSTRUCTOR_METHOD_NAME

fun KspFunction.getReturnTypeOrNull(): KJTypeName? =
    returnType?.asKJTypeName().takeIf { it != UNIT.asKJTypeName() }

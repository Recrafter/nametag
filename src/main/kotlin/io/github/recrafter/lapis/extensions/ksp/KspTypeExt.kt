package io.github.recrafter.lapis.extensions.ksp

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ksp.toTypeName
import io.github.recrafter.lapis.extensions.kp.asKJTypeName
import io.github.recrafter.lapis.kj.KJTypeName

fun KSType.genericTypes(): List<KSType> =
    arguments.map { requireNotNull(it.type).resolve() }

fun KSType.asKJTypeName(): KJTypeName =
    toTypeName().asKJTypeName()

package io.github.recrafter.lapis.extensions.ksp

import com.google.devtools.ksp.symbol.KSValueParameter
import io.github.recrafter.lapis.kj.KJParameter
import io.github.recrafter.lapis.kj.KJParameterList

fun KSValueParameter.requireName(): String =
    requireNotNull(name?.asString()) {
        "Unnamed parameter is not supported in this context: $this."
    }

fun List<KSValueParameter>.asKJParameterList(): KJParameterList =
    KJParameterList(map { KJParameter(it.requireName(), it.type.asKJTypeName()) })

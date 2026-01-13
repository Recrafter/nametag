package io.github.recrafter.lapis.extensions.kp

import io.github.recrafter.lapis.kj.KJTypeName

fun KPTypeName.asKJTypeName(): KJTypeName =
    KJTypeName(this)

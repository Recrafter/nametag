package io.github.recrafter.lapis.extensions.jp

fun JPTypeName.boxIfPrimitive(extraCondition: Boolean = true): JPTypeName =
    if (extraCondition && isPrimitive && !isBoxedPrimitive) box()
    else this

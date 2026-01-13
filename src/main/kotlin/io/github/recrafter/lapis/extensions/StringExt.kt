package io.github.recrafter.lapis.extensions

fun String.prefixed(prefix: String = ""): String =
    prefix + replaceFirstChar { it.titlecaseChar() }

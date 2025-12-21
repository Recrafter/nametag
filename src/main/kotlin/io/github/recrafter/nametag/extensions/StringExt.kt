package io.github.recrafter.nametag.extensions

fun String.capitalized(): String =
    replaceFirstChar { it.titlecaseChar() }

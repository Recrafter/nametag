package io.github.recrafter.nametag.extensions.poets.kotlin

import com.squareup.kotlinpoet.AnnotationSpec

fun AnnotationSpec.Builder.addStringMember(value: String) {
    addMember("%S", value)
}

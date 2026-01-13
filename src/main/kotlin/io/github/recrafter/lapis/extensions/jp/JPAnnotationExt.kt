package io.github.recrafter.lapis.extensions.jp

import io.github.recrafter.lapis.kj.KJTypeName

fun JPAnnotationBuilder.addStringMember(name: String, value: String) {
    addMember(name, "\$S", value)
}

fun JPAnnotationBuilder.addClassMember(name: String, type: KJTypeName) {
    addMember(name, "\$T.class", type.javaVersion)
}

fun JPAnnotationBuilder.addClassArrayMember(name: String, vararg types: KJTypeName) {
    val javaTypes = types.map { it.javaVersion }.toTypedArray()
    val format = buildString {
        append("{")
        if (javaTypes.isNotEmpty()) {
            append(javaTypes.joinToString { "\$T.class" })
        }
        append("}")
    }
    addMember(name, format, *javaTypes)
}

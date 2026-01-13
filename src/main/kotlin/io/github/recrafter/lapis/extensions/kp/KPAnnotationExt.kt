package io.github.recrafter.lapis.extensions.kp

fun KPAnnotationBuilder.addStringArrayMember(name: String, strings: List<String>) {
    val format = buildString {
        if (strings.isNotEmpty()) {
            append(strings.joinToString { "%S" })
        }
    }
    addMember(format, *strings.toTypedArray())
}

package io.github.recrafter.lapis.utils

import io.github.recrafter.lapis.kj.KJTypeName

object MixinUtils {

    const val CONSTRUCTOR_METHOD_NAME: String = "<init>"

    fun getConstructorDescriptor(argumentTypes: List<KJTypeName>): String =
        CONSTRUCTOR_METHOD_NAME + "(" + argumentTypes.joinToString("") { it.descriptor } + ")V"

}

private val KJTypeName.descriptor: String
    get() = "L" + className!!.qualifiedName.replace(".", "/") + ";"

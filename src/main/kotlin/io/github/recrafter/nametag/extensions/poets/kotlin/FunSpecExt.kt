package io.github.recrafter.nametag.extensions.poets.kotlin

import com.squareup.kotlinpoet.FunSpec

fun FunSpec.Builder.addInvokeFunctionStatement(
    hasReturnType: Boolean,
    receiver: String,
    functionName: String,
    parameterNames: List<String> = emptyList(),
) {
    addStatement(buildString {
        if (hasReturnType) {
            append("return ")
        }
        append(receiver)
        append(".")
        append(functionName)
        append("(")
        append(parameterNames.joinToString())
        append(")")
    })
}

fun FunSpec.Builder.addGetPropertyStatement(receiver: String, propertyName: String) {
    addStatement(buildString {
        append("return ")
        append(receiver)
        append(".")
        append(propertyName)
    })
}

fun FunSpec.Builder.addSetPropertyStatement(receiver: String, propertyName: String, propertyValue: String) {
    addStatement(buildString {
        append(receiver)
        append(".")
        append(propertyName)
        append(" = ")
        append(propertyValue)
    })
}

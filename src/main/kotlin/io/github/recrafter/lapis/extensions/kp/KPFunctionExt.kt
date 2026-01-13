package io.github.recrafter.lapis.extensions.kp

import com.squareup.kotlinpoet.UNIT
import io.github.recrafter.lapis.kj.KJClassName
import io.github.recrafter.lapis.kj.KJTypeName

fun KPFunctionBuilder.addInvokeFunctionStatement(
    isReturn: Boolean,
    receiver: KPCodeBlock? = null,
    functionName: String,
    parameterNames: List<String> = emptyList(),
) {
    val args = mutableListOf<Any>()
    val format = buildString {
        if (isReturn) {
            append("return ")
        }
        if (receiver != null) {
            append("%L.")
            args += receiver
        }
        append("%L(")
        args += functionName

        append(parameterNames.joinToString())
        append(")")
    }
    addStatement(format, *args.toTypedArray())
}

fun KPFunctionBuilder.addInvokeFunctionStatement(
    isReturn: Boolean,
    receiver: KJClassName,
    functionName: String,
    parameterNames: List<String> = emptyList(),
) {
    addInvokeFunctionStatement(isReturn, receiver.kotlinCodeBlock, functionName, parameterNames)
}

fun KPFunctionBuilder.addGetterStatement(propertyName: String, receiver: KPCodeBlock = KPCodeBlock.of("this")) {
    addStatement("return %L.%L", receiver, propertyName)
}

fun KPFunctionBuilder.addGetterStatement(receiver: KJClassName, propertyName: String) {
    addGetterStatement(propertyName, receiver.kotlinCodeBlock)
}

fun KPFunctionBuilder.addSetterStatement(
    propertyName: String,
    propertyValue: String,
    receiver: KPCodeBlock = KPCodeBlock.of("this"),
) {
    addStatement("%L.%L = %L", receiver, propertyName, propertyValue)
}

fun KPFunctionBuilder.addSetterStatement(receiver: KJClassName, propertyName: String, propertyValue: String) {
    addSetterStatement(propertyName, propertyValue, receiver.kotlinCodeBlock)
}

fun KPFunctionBuilder.setReturnType(type: KJTypeName?) {
    returns(type?.kotlinVersion ?: UNIT)
}

fun KPFunctionBuilder.setReturnType(type: KJClassName?) {
    setReturnType(type?.typeName)
}

fun KPFunctionBuilder.setReceiverType(type: KJTypeName) {
    receiver(type.kotlinVersion)
}

fun KPFunctionBuilder.setReceiverType(type: KJClassName) {
    setReceiverType(type.typeName)
}

fun KPFunctionBuilder.setParameters(vararg parameters: Pair<String, KJTypeName>) {
    clearParameters()
    addParameters(*parameters)
}

fun KPFunctionBuilder.addParameters(vararg parameters: Pair<String, KJTypeName>) {
    parameters.forEach { (name, type) ->
        addParameter(name, type.kotlinVersion)
    }
}

fun KPFunctionBuilder.setParameters(parameters: List<KPParameter>) {
    clearParameters()
    addParameters(parameters)
}

private fun KPFunctionBuilder.clearParameters() {
    parameters.clear()
}

package io.github.recrafter.lapis.extensions.jp

import io.github.recrafter.lapis.kj.KJClassName
import io.github.recrafter.lapis.kj.KJTypeName

inline fun <reified A : Annotation> JPMethodBuilder.addAnnotation(builder: JPAnnotationBuilder.() -> Unit = {}) {
    addAnnotation(buildJavaAnnotation<A>(builder))
}

fun JPMethodBuilder.addStubStatement() {
    addStatement("throw new ${AssertionError::class.simpleName}()")
}

fun JPMethodBuilder.addIfStatement(condition: JPCodeBlock, body: JPMethodBuilder.() -> Unit) {
    withControlFlow(
        buildJavaCodeBlock {
            add("if (")
            add(condition)
            add(")")
        },
        body
    )
}

fun JPMethodBuilder.withControlFlow(controlFlow: JPCodeBlock, body: JPMethodBuilder.() -> Unit) {
    beginControlFlow(controlFlow)
    apply(body)
    endControlFlow()
}

fun JPMethodBuilder.addReturnStatement(value: String) {
    addStatement("return $value")
}

fun JPMethodBuilder.addInvokeFunctionStatement(
    isReturn: Boolean,
    receiver: JPCodeBlock? = null,
    functionName: String,
    parameterNames: List<String> = emptyList(),
) {
    val args = mutableListOf<Any>()
    val format = buildString {
        if (isReturn) {
            append("return ")
        }
        if (receiver != null) {
            append("\$L.")
            args += receiver
        }
        append("\$L")
        args += functionName

        append("(")
        append(parameterNames.joinToString())
        append(")")
    }
    addStatement(format, *args.toTypedArray())
}

fun JPMethodBuilder.addInvokeFunctionStatement(
    isReturn: Boolean,
    receiver: KJClassName,
    functionName: String,
    parameterNames: List<String> = emptyList(),
) {
    addInvokeFunctionStatement(isReturn, receiver.javaCodeBlock, functionName, parameterNames)
}

fun JPMethodBuilder.setReturnType(type: KJTypeName?) {
    setReturnType(type?.javaVersion)
}

fun JPMethodBuilder.setReturnType(type: KJClassName?) {
    setReturnType(type?.typeName)
}

fun JPMethodBuilder.setReturnType(type: JPTypeName?) {
    returns(type ?: JPTypeName.VOID)
}

fun JPMethodBuilder.setParameters(parameters: List<JPParameter>) {
    require(build().parameters().isEmpty()) {
        "Parameters are already set. Use setParameters() only once."
    }
    addParameters(parameters)
}

fun JPMethodBuilder.setParameters(vararg parameters: Pair<KJTypeName, String>) {
    require(build().parameters().isEmpty()) {
        "Parameters are already set. Use setParameters() only once."
    }
    parameters.forEach { (type, name) ->
        addParameter(type.javaVersion, name)
    }
}

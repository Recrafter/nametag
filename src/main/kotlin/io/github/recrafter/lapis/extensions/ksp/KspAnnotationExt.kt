package io.github.recrafter.lapis.extensions.ksp

import com.google.devtools.ksp.symbol.KSType

fun KspAnnotation.getClassDeclarationArgumentOrNull(argumentName: String): KspClass? {
    val ksType = arguments
        .firstOrNull { it.name?.asString() == argumentName }
        ?.value as? KSType
        ?: return null
    return ksType.declaration as? KspClass
}

fun KspAnnotation.getClassDeclarationArgument(argumentName: String): KspClass =
    requireNotNull(getClassDeclarationArgumentOrNull(argumentName)) {
        "Class declaration argument '$argumentName' not found"
    }

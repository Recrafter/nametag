package io.github.recrafter.lapis.extensions.ksp

val KspDeclaration.name: String
    get() = simpleName.asString()

fun KspDeclaration.requireQualifiedName(): String =
    requireNotNull(qualifiedName).asString()

inline fun <reified T> KspDeclaration.isInstance(): Boolean =
    requireQualifiedName() == T::class.qualifiedName

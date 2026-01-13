package io.github.recrafter.lapis.extensions.ksp

import com.google.devtools.ksp.symbol.KSModifierListOwner
import com.google.devtools.ksp.symbol.Modifier

val KSModifierListOwner.isPublic: Boolean
    get() = !isPrivate && !isProtected && !isInternal

val KSModifierListOwner.isPrivate: Boolean
    get() = modifiers.contains(Modifier.PRIVATE)

val KSModifierListOwner.isInternal: Boolean
    get() = modifiers.contains(Modifier.INTERNAL)

val KSModifierListOwner.isProtected: Boolean
    get() = modifiers.contains(Modifier.PROTECTED)

val KSModifierListOwner.isAbstract: Boolean
    get() = modifiers.contains(Modifier.ABSTRACT)

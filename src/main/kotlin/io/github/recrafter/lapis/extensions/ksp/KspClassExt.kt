package io.github.recrafter.lapis.extensions.ksp

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ksp.toClassName
import io.github.recrafter.lapis.extensions.kp.asKJClassName
import io.github.recrafter.lapis.kj.KJClassName

val KspClass.isInterface: Boolean
    get() = classKind == ClassKind.INTERFACE

val KspClass.isClass: Boolean
    get() = classKind == ClassKind.CLASS

val KspClass.superInterfaceTypes: List<KSType>
    get() = superTypes
        .map { it.resolve() }
        .filter { type ->
            (type.declaration as? KspClass)?.isInterface == true
        }
        .toList()

val KspClass.constructors: List<KspFunction>
    get() = declarations.filterIsInstance<KspFunction>().filter { it.isConstructor }.toList()

val KspClass.properties: List<KspProperty>
    get() = declarations.filterIsInstance<KspProperty>().toList()

val KspClass.functions: List<KSFunctionDeclaration>
    get() = declarations.filterIsInstance<KSFunctionDeclaration>().filter { !it.isConstructor }.toList()

fun KspClass.getSuperClassTypeOrNull(): KSType? =
    superTypes.map { it.resolve() }.firstOrNull { type ->
        (type.declaration as? KspClass)?.isClass == true
    }

fun KspClass.asKJClassName(): KJClassName =
    toClassName().asKJClassName()

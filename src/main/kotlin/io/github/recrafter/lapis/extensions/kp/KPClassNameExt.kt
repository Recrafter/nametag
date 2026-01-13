package io.github.recrafter.lapis.extensions.kp

import io.github.recrafter.lapis.extensions.jp.JPClassName
import io.github.recrafter.lapis.extensions.jp.JPTypeName
import io.github.recrafter.lapis.extensions.jp.boxIfPrimitive
import io.github.recrafter.lapis.kj.KJClassName

fun KPClassName.asKJClassName(): KJClassName =
    KJClassName(packageName, *simpleNames.toTypedArray())

fun KPClassName.toJavaType(shouldBox: Boolean = false): JPTypeName =
    when (copy(nullable = false)) {
        com.squareup.kotlinpoet.BOOLEAN -> JPTypeName.BOOLEAN.boxIfPrimitive(shouldBox || isNullable)
        com.squareup.kotlinpoet.BYTE -> JPTypeName.BYTE.boxIfPrimitive(shouldBox || isNullable)
        com.squareup.kotlinpoet.CHAR -> JPTypeName.CHAR.boxIfPrimitive(shouldBox || isNullable)
        com.squareup.kotlinpoet.SHORT -> JPTypeName.SHORT.boxIfPrimitive(shouldBox || isNullable)
        com.squareup.kotlinpoet.INT -> JPTypeName.INT.boxIfPrimitive(shouldBox || isNullable)
        com.squareup.kotlinpoet.LONG -> JPTypeName.LONG.boxIfPrimitive(shouldBox || isNullable)
        com.squareup.kotlinpoet.FLOAT -> JPTypeName.FLOAT.boxIfPrimitive(shouldBox || isNullable)
        com.squareup.kotlinpoet.DOUBLE -> JPTypeName.DOUBLE.boxIfPrimitive(shouldBox || isNullable)
        com.squareup.kotlinpoet.UNIT -> JPTypeName.VOID

        com.squareup.kotlinpoet.STRING -> JPClassName.get("java.lang", "String")
        com.squareup.kotlinpoet.ANY -> JPClassName.get("java.lang", "Object")

        com.squareup.kotlinpoet.LIST -> JPClassName.get("java.util", "List")
        com.squareup.kotlinpoet.SET -> JPClassName.get("java.util", "Set")
        com.squareup.kotlinpoet.MAP -> JPClassName.get("java.util", "Map")
        else -> {
            if (simpleNames.size == 1) {
                JPClassName.get(packageName, simpleName)
            } else {
                JPClassName.get(
                    packageName,
                    simpleNames.first(),
                    *simpleNames.drop(1).toTypedArray()
                )
            }
        }
    }

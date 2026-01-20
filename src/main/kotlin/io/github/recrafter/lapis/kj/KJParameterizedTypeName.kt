package io.github.recrafter.lapis.kj

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.recrafter.lapis.extensions.common.unsafeLazy
import io.github.recrafter.lapis.extensions.jp.JPParameterizedTypeName
import io.github.recrafter.lapis.extensions.kp.KPParameterizedTypeName

class KJParameterizedTypeName(val className: KJClassName, vararg val genericTypes: KJTypeName) {

    val kotlinVersion: KPParameterizedTypeName by unsafeLazy {
        className.kotlinVersion.parameterizedBy(genericTypes.map { it.kotlinVersion })
    }

    val javaVersion: JPParameterizedTypeName by unsafeLazy {
        JPParameterizedTypeName.get(
            className.javaVersion,
            *genericTypes.map { it.javaVersion }.toTypedArray()
        )
    }
}

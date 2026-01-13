package io.github.recrafter.lapis.extensions.kp

import io.github.recrafter.lapis.kj.KJTypeName

fun KPType.requireName(): String =
    requireNotNull(name) {
        "Unnamed type is not supported in this context: $this."
    }

fun KPType.toKotlinFile(packageName: String, builder: KPFileBuilder.() -> Unit = {}): KPFile =
    buildKotlinFile(
        packageName,
        requireName()
    ) fileBuilder@{
        builder()
        addType(this@toKotlinFile)
    }

fun KPTypeBuilder.setConstructor(vararg parameters: Pair<String, KJTypeName>) {
    primaryConstructor(buildKotlinConstructor {
        addParameters(*parameters)
    })
}

fun KPTypeBuilder.setSuperClassType(type: KJTypeName) {
    superclass(type.kotlinVersion)
}

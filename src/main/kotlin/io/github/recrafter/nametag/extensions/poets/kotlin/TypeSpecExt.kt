package io.github.recrafter.nametag.extensions.poets.kotlin

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.recrafter.nametag.accessors.processor.KotlinFile

fun TypeSpec.requireName(): String =
    requireNotNull(name) {
        "Unnamed type is not supported in this context: $this."
    }

fun TypeSpec.toKotlinFile(packageName: String, builder: FileSpec.Builder.() -> Unit = {}): KotlinFile =
    buildKotlinFile(packageName, requireName()) fileBuilder@{
        builder()
        addType(this@toKotlinFile)
    }

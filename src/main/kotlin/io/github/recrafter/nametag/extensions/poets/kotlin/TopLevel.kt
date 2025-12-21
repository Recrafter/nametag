package io.github.recrafter.nametag.extensions.poets.kotlin

import com.squareup.kotlinpoet.*
import io.github.recrafter.nametag.accessors.processor.KotlinFile
import io.github.recrafter.nametag.accessors.processor.KotlinType

fun buildKotlinProperty(type: KotlinType, name: String, builder: PropertySpec.Builder.() -> Unit = {}): PropertySpec =
    PropertySpec.builder(name, type).apply(builder).build()

fun buildKotlinGetter(builder: FunSpec.Builder.() -> Unit = {}): FunSpec =
    FunSpec.getterBuilder().apply(builder).build()

fun buildKotlinSetter(builder: FunSpec.Builder.() -> Unit = {}): FunSpec =
    FunSpec.setterBuilder().apply(builder).build()

fun buildKotlinParameter(
    type: KotlinType,
    name: String,
    builder: ParameterSpec.Builder.() -> Unit = {}
): ParameterSpec =
    ParameterSpec.builder(name, type).apply(builder).build()

fun buildKotlinFunction(name: String, builder: FunSpec.Builder.() -> Unit = {}): FunSpec =
    FunSpec.builder(name).apply(builder).build()

fun buildKotlinObject(name: String, builder: TypeSpec.Builder.() -> Unit = {}): TypeSpec =
    TypeSpec.objectBuilder(name).apply(builder).build()

fun buildKotlinFile(packageName: String, name: String, builder: FileSpec.Builder.() -> Unit = {}): KotlinFile =
    KotlinFile.builder(packageName, name).apply(builder).indent("    ").build()

inline fun <reified A : Annotation> buildKotlinAnnotation(
    builder: AnnotationSpec.Builder.() -> Unit = {}
): AnnotationSpec =
    AnnotationSpec.builder(A::class).apply(builder).build()

package io.github.recrafter.lapis.extensions.kp

import io.github.recrafter.lapis.kj.KJClassName
import io.github.recrafter.lapis.kj.KJTypeName

typealias KPAnnotation = com.squareup.kotlinpoet.AnnotationSpec
typealias KPAnnotationBuilder = com.squareup.kotlinpoet.AnnotationSpec.Builder

typealias KPProperty = com.squareup.kotlinpoet.PropertySpec
typealias KPPropertyBuilder = com.squareup.kotlinpoet.PropertySpec.Builder

typealias KPParameter = com.squareup.kotlinpoet.ParameterSpec
typealias KPParameterBuilder = com.squareup.kotlinpoet.ParameterSpec.Builder

typealias KPFunction = com.squareup.kotlinpoet.FunSpec
typealias KPFunctionBuilder = com.squareup.kotlinpoet.FunSpec.Builder

typealias KPTypeAlias = com.squareup.kotlinpoet.TypeAliasSpec
typealias KPTypeAliasBuilder = com.squareup.kotlinpoet.TypeAliasSpec.Builder

typealias KPType = com.squareup.kotlinpoet.TypeSpec
typealias KPTypeBuilder = com.squareup.kotlinpoet.TypeSpec.Builder

typealias KPFile = com.squareup.kotlinpoet.FileSpec
typealias KPFileBuilder = com.squareup.kotlinpoet.FileSpec.Builder

typealias KPTypeName = com.squareup.kotlinpoet.TypeName
typealias KPClassName = com.squareup.kotlinpoet.ClassName
typealias KPParameterizedTypeName = com.squareup.kotlinpoet.ParameterizedTypeName

typealias KPCodeBlock = com.squareup.kotlinpoet.CodeBlock

val KPAny: KPTypeName = com.squareup.kotlinpoet.ANY
val KPUnit: KPTypeName = com.squareup.kotlinpoet.UNIT
val KPBoolean: KPTypeName = com.squareup.kotlinpoet.BOOLEAN
val KPByte: KPTypeName = com.squareup.kotlinpoet.BYTE
val KPShort: KPTypeName = com.squareup.kotlinpoet.SHORT
val KPInt: KPTypeName = com.squareup.kotlinpoet.INT
val KPLong: KPTypeName = com.squareup.kotlinpoet.LONG
val KPChar: KPTypeName = com.squareup.kotlinpoet.CHAR
val KPFloat: KPTypeName = com.squareup.kotlinpoet.FLOAT
val KPDouble: KPTypeName = com.squareup.kotlinpoet.DOUBLE
val KPString: KPTypeName = com.squareup.kotlinpoet.STRING
val KPList: KPTypeName = com.squareup.kotlinpoet.LIST
val KPSet: KPTypeName = com.squareup.kotlinpoet.SET
val KPMap: KPTypeName = com.squareup.kotlinpoet.MAP

fun buildKotlinConstructor(builder: KPFunctionBuilder.() -> Unit = {}): KPFunction =
    KPFunction.constructorBuilder().apply(builder).build()

fun buildKotlinProperty(name: String, type: KJTypeName, builder: KPPropertyBuilder.() -> Unit = {}): KPProperty =
    KPProperty.builder(name, type.kotlinVersion).apply(builder).build()

fun buildKotlinGetter(builder: KPFunctionBuilder.() -> Unit = {}): KPFunction =
    KPFunction.getterBuilder().apply(builder).build()

fun buildKotlinSetter(builder: KPFunctionBuilder.() -> Unit = {}): KPFunction =
    KPFunction.setterBuilder().apply(builder).build()

fun buildKotlinTypeAlias(
    name: String,
    type: KJClassName,
    builder: KPTypeAliasBuilder.() -> Unit = {}
): KPTypeAlias =
    KPTypeAlias.builder(name, type.kotlinVersion).apply(builder).build()

fun buildKotlinParameter(
    name: String,
    type: KJTypeName,
    builder: KPParameterBuilder.() -> Unit = {}
): KPParameter =
    KPParameter.builder(name, type.kotlinVersion).apply(builder).build()

fun buildKotlinFunction(name: String, builder: KPFunctionBuilder.() -> Unit = {}): KPFunction =
    KPFunction.builder(name).apply(builder).build()

fun buildKotlinInterface(name: String, builder: KPTypeBuilder.() -> Unit = {}): KPType =
    KPType.interfaceBuilder(name).apply(builder).build()

fun buildKotlinClass(name: String, builder: KPTypeBuilder.() -> Unit = {}): KPType =
    KPType.classBuilder(name).apply(builder).build()

fun buildKotlinObject(name: String, builder: KPTypeBuilder.() -> Unit = {}): KPType =
    KPType.objectBuilder(name).apply(builder).build()

fun buildKotlinFile(packageName: String, name: String, builder: KPFileBuilder.() -> Unit = {}): KPFile =
    KPFile.builder(packageName, name).apply(builder).indent("    ").build()

inline fun <reified A : Annotation> buildKotlinAnnotation(builder: KPAnnotationBuilder.() -> Unit = {}): KPAnnotation =
    KPAnnotation.builder(A::class).apply(builder).build()

fun buildKotlinCast(from: KPCodeBlock = KPCodeBlock.of("this"), to: KJClassName): KPCodeBlock =
    KPCodeBlock.of("(%L as %T)", from, to.kotlinVersion)

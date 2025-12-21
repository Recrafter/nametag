package io.github.recrafter.nametag.accessors.processor

import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ksp.toTypeName
import io.github.recrafter.nametag.accessors.annotations.*
import io.github.recrafter.nametag.extensions.*
import io.github.recrafter.nametag.extensions.poets.java.*
import io.github.recrafter.nametag.extensions.poets.kotlin.*
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Mutable
import org.spongepowered.asm.mixin.gen.Accessor
import org.spongepowered.asm.mixin.gen.Invoker
import javax.lang.model.element.Modifier

typealias JavaFunction = com.palantir.javapoet.MethodSpec
typealias JavaType = com.palantir.javapoet.TypeName
typealias JavaParameter = com.palantir.javapoet.ParameterSpec

typealias KotlinFunction = com.squareup.kotlinpoet.FunSpec
typealias KotlinType = com.squareup.kotlinpoet.TypeName
typealias KotlinProperty = com.squareup.kotlinpoet.PropertySpec
typealias KotlinParameter = com.squareup.kotlinpoet.ParameterSpec
typealias KotlinClass = com.squareup.kotlinpoet.ClassName
typealias KotlinFile = com.squareup.kotlinpoet.FileSpec

class KotlinAccessorProcessor(private val generator: CodeGenerator, private val logger: KSPLogger) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation<KAccessor>()
        symbols.forEach { symbol ->
            logger.kspRequire(symbol is KSClassDeclaration && symbol.isInterface(), symbol) {
                "Annotation ${KAccessor::class.atName} " +
                        "can only be applied to interfaces."
            }
            logger.kspRequire(symbol.isPrivate(), symbol) {
                "Interface annotated with ${KAccessor::class.atName} " +
                        "must be private."
            }
            symbol.parentDeclaration?.let { parent ->
                logger.kspRequire(parent is KSClassDeclaration && parent.isInterface(), symbol) {
                    "Interface annotated with ${KAccessor::class.atName} " +
                            "must be nested inside another interface, " +
                            "but its outer type '${parent.simpleName.asString()}' is not an interface."
                }
                logger.kspRequire(parent.hasAnnotation<Widener>() || parent.hasAnnotation<KAccessor>(), symbol) {
                    "Outer interface '${parent.simpleName.asString()}' " +
                            "must be annotated with " +
                            "${Widener::class.atName} and/or ${KAccessor::class.atName} " +
                            "to contain nested ${KAccessor::class.atName} interfaces."
                }
            }
            val kotlinTargetClass = symbol.getAnnotationArgumentType<KAccessor>("value").toTypeName()
            logger.kspRequire(kotlinTargetClass is KotlinClass, symbol) {
                "${KAccessor::class.atName} value must be a class reference."
            }
            val mixinJavaMethods = mutableListOf<JavaFunction>()
            val topLevelKotlinFunctions = mutableListOf<KotlinFunction>()
            val kotlinExtensionProperties = mutableListOf<KotlinProperty>()
            val kotlinExtensionFunctions = mutableListOf<KotlinFunction>()
            val kotlinFactoryProperties = mutableListOf<KotlinProperty>()
            val kotlinFactoryFunctions = mutableListOf<KotlinFunction>()

            val javaTargetClass = kotlinTargetClass.toJavaClassName()
            val mixinClassName = symbol.name + "_Generated"
            val mixinCast = "(this as $mixinClassName)"
            val factoryObjectName = kotlinTargetClass.simpleName + "Factory"

            symbol.declarations.forEach { declaration ->
                when {
                    declaration is KSPropertyDeclaration -> {
                        val property = declaration
                        val openPropertyAnnotation = property.getSingleAnnotationOrNull<OpenProperty>()
                        logger.kspRequire(openPropertyAnnotation != null, property) {
                            "Properties inside ${KAccessor::class.atName} interfaces " +
                                    "must be annotated with ${OpenProperty::class.atName}."
                        }
                        logger.kspRequire(property.getter?.isAbstract == true, property) {
                            "Properties inside ${KAccessor::class.atName} interfaces " +
                                    "must not declare a getter."
                        }
                        val kotlinPropertyType = property.type.resolve().toTypeName()
                        val javaPropertyType = kotlinPropertyType.toJavaType()
                        val propertyName = property.name
                        val target = openPropertyAnnotation.target.ifEmpty { propertyName }
                        val accessorGetter = buildAccessor(
                            methodType = AccessorMethodType.GETTER,
                            propertyType = javaPropertyType,
                            propertyName = propertyName,
                            target = target,
                            isStatic = openPropertyAnnotation.isStatic
                        )
                        mixinJavaMethods += accessorGetter
                        val accessorSetter = if (property.isMutable) {
                            buildAccessor(
                                methodType = AccessorMethodType.SETTER,
                                propertyType = javaPropertyType,
                                propertyName = propertyName,
                                target = target,
                                isStatic = openPropertyAnnotation.isStatic
                            )
                        } else null
                        if (accessorSetter != null) {
                            mixinJavaMethods += accessorSetter
                        }
                        val factoryProperty = if (openPropertyAnnotation.isStatic) {
                            buildKotlinProperty(kotlinPropertyType, propertyName) {
                                getter(buildKotlinGetter {
                                    addInvokeFunctionStatement(true, mixinClassName, accessorGetter.name())
                                })
                                if (accessorSetter != null) {
                                    mutable(true)
                                    setter(buildKotlinSetter {
                                        addParameter("newValue", kotlinPropertyType)
                                        addInvokeFunctionStatement(
                                            false,
                                            mixinClassName,
                                            accessorSetter.name(),
                                            listOf("newValue")
                                        )
                                    })
                                }
                            }
                        } else null
                        if (factoryProperty != null) {
                            kotlinFactoryProperties += factoryProperty
                        }
                        kotlinExtensionProperties += buildKotlinProperty(kotlinPropertyType, propertyName) {
                            receiver(kotlinTargetClass)
                            getter(buildKotlinGetter {
                                if (factoryProperty != null) {
                                    addGetPropertyStatement(factoryObjectName, factoryProperty.name)
                                } else {
                                    addInvokeFunctionStatement(true, mixinCast, accessorGetter.name())
                                }
                            })
                            if (accessorSetter != null) {
                                mutable(true)
                                setter(buildKotlinSetter {
                                    addParameter("newValue", kotlinPropertyType)
                                    if (factoryProperty != null) {
                                        addSetPropertyStatement(factoryObjectName, factoryProperty.name, "newValue")
                                    } else {
                                        addInvokeFunctionStatement(
                                            true,
                                            mixinCast,
                                            accessorSetter.name(),
                                            listOf("newValue")
                                        )
                                    }
                                })
                            }
                        }
                    }

                    declaration is KSFunctionDeclaration -> {
                        val function = declaration
                        logger.kspRequire(function.isAbstract, function) {
                            "Functions inside ${KAccessor::class.atName} interfaces " +
                                    "must not have a body."
                        }
                        val kotlinParameters = function.parameters.map { parameter ->
                            buildKotlinParameter(parameter.type.toKotlinType(), parameter.requireName())
                        }
                        val returnType = function.returnType?.toKotlinType().orUnit()
                        val hasReturn = !returnType.isUnit
                        val parameterNames = kotlinParameters.map { it.name }
                        val javaParameters = function.parameters.map { parameter ->
                            buildJavaParameter(parameter.type.toJavaType(), parameter.requireName())
                        }
                        val openFunctionAnnotation = function.getSingleAnnotationOrNull<OpenFunction>()
                        val openConstructorAnnotation = function.getSingleAnnotationOrNull<OpenConstructor>()
                        if (openFunctionAnnotation != null && openConstructorAnnotation == null) {
                            val invoker = buildInvoker(
                                name = "invoke" + function.name.capitalized(),
                                target = openFunctionAnnotation.target.ifEmpty { function.name },
                                isStatic = openFunctionAnnotation.isStatic,
                                parameters = javaParameters,
                                returnType = function.returnType?.toJavaType().orVoid(),
                            )
                            mixinJavaMethods += invoker

                            val factoryFunction = if (openFunctionAnnotation.isStatic) {
                                buildKotlinFunction(function.name) {
                                    addParameters(kotlinParameters)
                                    addInvokeFunctionStatement(
                                        hasReturn,
                                        mixinClassName,
                                        invoker.name(),
                                        parameterNames
                                    )
                                    returns(returnType)
                                }
                            } else null
                            if (factoryFunction != null) {
                                kotlinFactoryFunctions += factoryFunction
                            }

                            kotlinExtensionFunctions += buildKotlinFunction(function.name) {
                                receiver(kotlinTargetClass)
                                addParameters(kotlinParameters)
                                addInvokeFunctionStatement(
                                    hasReturn,
                                    if (factoryFunction != null) factoryObjectName else mixinCast,
                                    factoryFunction?.name ?: invoker.name(),
                                    parameterNames
                                )
                                returns(returnType)
                            }
                        } else if (openConstructorAnnotation != null && openFunctionAnnotation == null) {
                            logger.kspRequire(!hasReturn, function) {
                                "Functions annotated with ${OpenConstructor::class.atName} " +
                                        "must not have a return type."
                            }
                            val invoker = buildInvoker(
                                name = function.name,
                                target = openConstructorAnnotation.target,
                                isStatic = true,
                                parameters = javaParameters,
                                returnType = javaTargetClass
                            )
                            mixinJavaMethods += invoker
                            val factoryFunction = buildKotlinFunction(function.name) {
                                addParameters(kotlinParameters)
                                addInvokeFunctionStatement(true, mixinClassName, invoker.name(), parameterNames)
                                returns(kotlinTargetClass)
                            }
                            kotlinFactoryFunctions += factoryFunction
                            topLevelKotlinFunctions += buildKotlinFunction(kotlinTargetClass.simpleName) {
                                addParameters(kotlinParameters)
                                addInvokeFunctionStatement(
                                    true,
                                    factoryObjectName,
                                    factoryFunction.name,
                                    parameterNames
                                )
                                returns(kotlinTargetClass)
                            }
                        } else {
                            logger.kspError {
                                "Functions inside ${KAccessor::class.atName} interfaces " +
                                        "must be annotated with " +
                                        "${OpenFunction::class.atName} or ${OpenConstructor::class.atName}."
                            }
                        }
                    }

                    declaration is KSClassDeclaration && declaration.isInterface() -> {
                        val nestedInterface = declaration
                        val hasWidener = nestedInterface.hasAnnotation<Widener>()
                        logger.kspRequire(hasWidener || nestedInterface.hasAnnotation<KAccessor>(), nestedInterface) {
                            "Nested interface '${nestedInterface.simpleName.asString()}' " +
                                    "must be annotated with " +
                                    "${Widener::class.atName} and/or ${KAccessor::class.atName}."
                        }
                        if (hasWidener) {
                            logger.kspRequire(symbol.hasAnnotation<Widener>(), symbol) {
                                "Outer interface '${symbol.simpleName.asString()}' " +
                                        "must be annotated with ${Widener::class.atName} " +
                                        "to contain nested ${Widener::class.atName} interfaces."
                            }
                        }
                    }

                    else -> logger.kspError(declaration) {
                        "Only properties, functions, and nested interfaces " +
                                "are allowed inside ${KAccessor::class.atName} interfaces."
                    }
                }
            }
            val mixinPackageName = symbol.packageName.asString()
            val mixinInterface = buildJavaInterface(mixinClassName) {
                addAnnotation<Mixin> {
                    addClassMember("value", javaTargetClass)
                }
                addModifiers(Modifier.PUBLIC)
                addMethods(mixinJavaMethods)
            }.toJavaFile(mixinPackageName)
            mixinInterface.writeTo(generator, symbol.toDependencies())

            val extensionsPackageName = mixinPackageName.substringBefore(".mixins.") + ".extensions"
            val factoryPackageName = mixinPackageName.substringBefore(".mixins.") + ".factory"
            val kotlinExtensionFile = buildKotlinFile(extensionsPackageName, kotlinTargetClass.simpleName + "Ext") {
                addAnnotation<Suppress> {
                    addStringMember("CAST_NEVER_SUCCEEDS")
                    addStringMember("UnusedImport")
                    addStringMember("UnusedReceiverParameter")
                    addStringMember("RedundantVisibilityModifier")
                    addStringMember("unused")
                }
                addImport(mixinPackageName, mixinClassName)
                addImport(factoryPackageName, factoryObjectName)
                addFunctions(topLevelKotlinFunctions)
                addProperties(kotlinExtensionProperties)
                addFunctions(kotlinExtensionFunctions)
            }
            kotlinExtensionFile.writeTo(generator, symbol.toDependencies())

            val kotlinFactoryObjectFile = buildKotlinObject(factoryObjectName) {
                addProperties(kotlinFactoryProperties)
                addFunctions(kotlinFactoryFunctions)
            }.toKotlinFile(factoryPackageName) {
                addAnnotation<Suppress> {
                    addStringMember("CAST_NEVER_SUCCEEDS")
                    addStringMember("UnusedImport")
                    addStringMember("RedundantVisibilityModifier")
                    addStringMember("unused")
                }
                addImport(mixinPackageName, mixinClassName)
            }
            kotlinFactoryObjectFile.writeTo(generator, symbol.toDependencies())
        }
        return emptyList()
    }

    private fun buildAccessor(
        methodType: AccessorMethodType,
        propertyType: JavaType,
        propertyName: String,
        target: String,
        isStatic: Boolean,
    ): JavaFunction =
        buildJavaMethod(methodType.buildMethodName(propertyName)) {
            addAnnotation<Accessor> {
                addStringMember("value", target)
            }
            if (methodType == AccessorMethodType.SETTER) {
                addAnnotation<Mutable>()
            }
            addModifiers(
                Modifier.PUBLIC,
                if (isStatic) Modifier.STATIC else Modifier.ABSTRACT
            )
            if (methodType == AccessorMethodType.SETTER) {
                addParameter(propertyType, propertyName)
            }
            if (isStatic && methodType == AccessorMethodType.GETTER) {
                addStubStatement()
            }
            returns(
                when (methodType) {
                    AccessorMethodType.GETTER -> propertyType
                    AccessorMethodType.SETTER -> JavaType.VOID
                }
            )
        }

    private fun buildInvoker(
        name: String,
        target: String,
        isStatic: Boolean,
        parameters: List<JavaParameter>,
        returnType: JavaType,
    ): JavaFunction =
        buildJavaMethod(name) {
            addAnnotation<Invoker> {
                addStringMember("value", target)
            }
            addModifiers(
                Modifier.PUBLIC,
                if (isStatic) Modifier.STATIC else Modifier.ABSTRACT
            )
            addParameters(parameters)
            if (isStatic) {
                addStubStatement()
            }
            returns(returnType)
        }

    private enum class AccessorMethodType(val namePrefix: String) {

        GETTER("get"),
        SETTER("set");

        fun buildMethodName(propertyName: String): String =
            namePrefix + propertyName.capitalized()
    }
}

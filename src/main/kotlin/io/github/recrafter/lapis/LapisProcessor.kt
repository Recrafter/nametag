package io.github.recrafter.lapis

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSValueParameter
import com.llamalad7.mixinextras.expression.Definition
import com.llamalad7.mixinextras.expression.Definitions
import com.llamalad7.mixinextras.expression.Expression
import com.llamalad7.mixinextras.expression.Expressions
import com.llamalad7.mixinextras.injector.ModifyExpressionValue
import com.llamalad7.mixinextras.injector.ModifyReceiver
import com.llamalad7.mixinextras.injector.ModifyReturnValue
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.asClassName
import io.github.recrafter.lapis.annotations.accessors.AccessConstructor
import io.github.recrafter.lapis.annotations.accessors.AccessField
import io.github.recrafter.lapis.annotations.accessors.AccessMethod
import io.github.recrafter.lapis.annotations.accessors.Accessor
import io.github.recrafter.lapis.annotations.patches.Patch
import io.github.recrafter.lapis.annotations.patches.hooks.Hook
import io.github.recrafter.lapis.annotations.patches.hooks.Kind
import io.github.recrafter.lapis.annotations.patches.hooks.Method
import io.github.recrafter.lapis.annotations.patches.hooks.Original
import io.github.recrafter.lapis.api.patches.LapisPatch
import io.github.recrafter.lapis.extensions.addIfNotNull
import io.github.recrafter.lapis.extensions.atName
import io.github.recrafter.lapis.extensions.common.nullIfNot
import io.github.recrafter.lapis.extensions.jp.*
import io.github.recrafter.lapis.extensions.kp.*
import io.github.recrafter.lapis.extensions.ksp.*
import io.github.recrafter.lapis.extensions.prefixed
import io.github.recrafter.lapis.extensions.psi.PsiCallable
import io.github.recrafter.lapis.extensions.psi.findPsiFunction
import io.github.recrafter.lapis.kj.KJClassName
import io.github.recrafter.lapis.kj.KJTypeName
import io.github.recrafter.lapis.utils.Descriptors
import io.github.recrafter.lapis.utils.NonDeferringProcessor
import io.github.recrafter.lapis.utils.PsiHelper
import org.spongepowered.asm.mixin.*
import org.spongepowered.asm.mixin.gen.Invoker
import org.spongepowered.asm.mixin.injection.*
import javax.lang.model.element.Modifier
import kotlin.reflect.KClass

typealias AccessorAnnotation = org.spongepowered.asm.mixin.gen.Accessor

internal class LapisProcessor(
    arguments: Map<String, String>,
    private val generator: CodeGenerator,
    private val logger: KSPLogger,
) : NonDeferringProcessor() {

    private val modId: String by arguments
    private val packageName: String by arguments

    private val accessors: MutableMap<KJClassName, GeneratedAccessor> = mutableMapOf()
    private val patches: MutableMap<KJClassName, GeneratedPatch> = mutableMapOf()

    private val extensions: MutableMap<KJClassName, GeneratedExtension> = mutableMapOf()
    private val factories: MutableMap<KJClassName, GeneratedFactory> = mutableMapOf()

    private val wideners: MutableSet<String> = mutableSetOf()

    override fun run(resolver: Resolver) {
        resolveAccessors(resolver)
        resolvePatches(resolver)
    }

    override fun finish() {
        accessors.forEach { (className, accessor) ->
            if (accessor.isEmpty()) {
                return@forEach
            }
            buildJavaInterface(className.simpleName) {
                addAnnotation<Mixin> {
                    addClassMember(DEFAULT_ANNOTATION_ELEMENT_NAME, accessor.targetTypeName)
                }
                addModifiers(Modifier.PUBLIC)
                addMethods(accessor.methods)
            }.toJavaFile(className.packageName).writeTo(generator, accessor.symbols.toDependencies())
        }
        patches.forEach { (mixinClassName, patch) ->
            if (patch.isEmpty()) {
                return@forEach
            }
            buildKotlinClass(patch.implClassName.simpleName) {
                val propertyName = LapisPatch<*>::target.name
                setConstructor(propertyName to patch.targetTypeName)
                addProperty(buildKotlinProperty(propertyName, patch.targetTypeName) {
                    addModifiers(KModifier.OVERRIDE)
                    initializer(propertyName)
                })
                setSuperClassType(patch.patchTypeName)
            }.toKotlinFile(patch.implClassName.packageName) {

            }.writeTo(generator, patch.symbols.toDependencies())
            buildJavaClass(mixinClassName.simpleName) {
                addAnnotation<Mixin> {
                    addClassArrayMember(DEFAULT_ANNOTATION_ELEMENT_NAME, patch.targetTypeName)
                }
                addAnnotation<SuppressWarnings> {
                    addStringMember(DEFAULT_ANNOTATION_ELEMENT_NAME, "DataFlowIssue")
                }
                addSuperinterface(patch.bridgeClassName.javaVersion)
                val implFieldName = "patch"
                val implGetterName = implFieldName.prefixed("getOrInit")
                addField(buildJavaField(patch.implClassName.javaVersion, implFieldName) {
                    addAnnotation<Unique>()
                    addModifiers(Modifier.PRIVATE)
                })
                addMethod(buildJavaMethod(implGetterName) {
                    addAnnotation<Unique>()
                    addModifiers(Modifier.PRIVATE)
                    setReturnType(patch.implClassName.javaVersion)
                    addIfStatement(JPCodeBlock.of("$implFieldName == ${null.toString()}")) {
                        val javaObject = KJClassName("java.lang", "Object")
                        val objectCast = buildJavaCast(to = javaObject.typeName)
                        val targetCast = buildJavaCast(to = patch.targetTypeName, from = objectCast)
                        addStatement(
                            "\$L = new \$T(\$L)",
                            implFieldName,
                            patch.implClassName.javaVersion,
                            targetCast
                        )
                    }
                    addReturnStatement(implFieldName)
                })
                addMethods(patch.methods)
            }.toJavaFile(mixinClassName.packageName).writeTo(generator, patch.symbols.toDependencies())
        }
        extensions.forEach { (className, extension) ->
            if (extension.isEmpty()) {
                return@forEach
            }
            buildKotlinFile(className.packageName, className.simpleName + "Ext") {
                addAnnotation<Suppress> {
                    addStringArrayMember(
                        Suppress::names.name,
                        listOf(
                            "CAST_NEVER_SUCCEEDS",
                            "NOTHING_TO_INLINE",
                            "UnusedImport",
                            "UnusedReceiverParameter",
                            "RedundantVisibilityModifier",
                            "unused",
                        )
                    )
                }
                extension.typeAliases.forEach {
                    addTypeAlias(it)
                }
                addFunctions(extension.topLevelFunctions)
                addProperties(extension.properties)
                addFunctions(extension.functions)
            }.writeTo(generator, extension.symbols.toDependencies())
        }
        factories.forEach { (className, factory) ->
            if (factory.isEmpty()) {
                return@forEach
            }
            buildKotlinObject(className.simpleName) {
                addProperties(factory.properties)
                addFunctions(factory.functions)
            }.toKotlinFile(className.packageName) {
                addAnnotation<Suppress> {
                    addStringArrayMember(
                        Suppress::names.name,
                        listOf(
                            "CAST_NEVER_SUCCEEDS",
                            "UnusedImport",
                            "RedundantVisibilityModifier",
                            "unused",
                        )
                    )
                }
            }.writeTo(generator, factory.symbols.toDependencies())
        }
        generator.createResourceFile(
            path = "META-INF/lapis/wideners.txt",
            contents = wideners.joinToString("\n"),
            aggregating = true,
        )
        reset()
    }

    override fun onError() {
        reset()
    }

    private fun resolveAccessors(resolver: Resolver) {
        resolver.forEachSymbolsAnnotatedWith<Accessor> { symbol, accessor, annotation ->
            logger.kspRequire(symbol is KspClass && symbol.isInterface, symbol) {
                "Annotation ${Accessor::class.atName} can only be applied to interfaces."
            }
            logger.kspRequire(symbol.superInterfaceTypes.isEmpty(), symbol) {
                "Interface annotated with ${Accessor::class.atName} must not extends interfaces."
            }
            symbol.parentDeclaration?.let { parent ->
                logger.kspRequire(parent is KspClass && parent.isInterface, symbol) {
                    "Interface annotated with ${Accessor::class.atName} must be nested inside another interface."
                }
                val parentAccessorAnnotation = parent.getSingleAnnotationOrNull<Accessor>()
                logger.kspRequire(parentAccessorAnnotation != null, symbol) {
                    "Outer interface '${parent.name}' must be annotated with ${Accessor::class.atName} " +
                        "to contain nested ${Accessor::class.atName} interfaces."
                }
                if (accessor.widener.isNotEmpty()) {
                    logger.kspRequire(parentAccessorAnnotation.widener.isNotEmpty(), symbol) {
                        "Outer interface '${parent.name}' must be have non-empty ${Accessor::widener.name} parameter " +
                            "to contain nested ${Accessor::widener.name} parameter."
                    }
                }
            }
            val targetClassName = annotation.getClassDeclarationArgument(Accessor::target.name).asKJClassName()
            if (accessor.widener.isNotEmpty()) {
                wideners += if (symbol.parentDeclaration != null) {
                    generateSequence(symbol) { it.parentDeclaration as? KspClass }
                        .toList()
                        .mapNotNull { it.getSingleAnnotationOrNull<Accessor>() }
                        .asReversed()
                        .joinToString("$") { it.widener.removePrefix(".") }
                } else {
                    accessor.widener
                }
            }

            val mixinClassName = KJClassName(symbol.packageName.asString(), symbol.name + "_Mixin")
            val mixinCast = buildKotlinCast(to = mixinClassName)
            val factoryClassName = KJClassName(targetClassName.packageName, targetClassName.simpleName + "KFactory")

            val mixinMethods = mutableListOf<JPMethod>()
            val topLevelFunctions = mutableListOf<KPFunction>()
            val extensionProperties = mutableListOf<KPProperty>()
            val extensionFunctions = mutableListOf<KPFunction>()
            val factoryProperties = mutableListOf<KPProperty>()
            val factoryFunctions = mutableListOf<KPFunction>()

            symbol.declarations.forEach { declaration ->
                when {
                    declaration is KspProperty -> {
                        val property = declaration
                        restrictMixinAnnotations(property)
                        val accessFieldAnnotation = property.getSingleAnnotationOrNull<AccessField>()
                        logger.kspRequire(accessFieldAnnotation != null, property) {
                            "Properties inside ${Accessor::class.atName} interfaces " +
                                "must be annotated with ${AccessField::class.atName}."
                        }
                        logger.kspRequire(property.getter?.isAbstract == true, property) {
                            "Properties inside ${Accessor::class.atName} interfaces must not declare a getter."
                        }
                        val propertyTypeName = property.type.asKJTypeName()
                        val nameByUser = property.name
                        val vanillaName = accessFieldAnnotation.vanillaName.ifEmpty { nameByUser }
                        val accessorGetter = buildAccessorMethod(
                            AccessorMethodType.GETTER,
                            vanillaName,
                            accessFieldAnnotation.isStatic,
                            propertyTypeName,
                            nameByUser
                        )
                        mixinMethods += accessorGetter
                        val accessorSetter = nullIfNot(property.isMutable) {
                            buildAccessorMethod(
                                AccessorMethodType.SETTER,
                                vanillaName,
                                accessFieldAnnotation.isStatic,
                                propertyTypeName,
                                nameByUser,
                            )
                        }
                        mixinMethods.addIfNotNull(accessorSetter)
                        val factoryProperty = nullIfNot(accessFieldAnnotation.isStatic) {
                            buildKotlinProperty(nameByUser, propertyTypeName) {
                                getter(buildKotlinGetter {
                                    addInvokeFunctionStatement(true, mixinClassName, accessorGetter.name())
                                })
                                if (accessorSetter != null) {
                                    mutable(true)
                                    setter(buildKotlinSetter {
                                        setParameters(SETTER_ARGUMENT_NAME to propertyTypeName)
                                        addInvokeFunctionStatement(
                                            false, mixinClassName, accessorSetter.name(), listOf(SETTER_ARGUMENT_NAME)
                                        )
                                    })
                                }
                            }
                        }
                        factoryProperties.addIfNotNull(factoryProperty)
                        extensionProperties += buildKotlinProperty(nameByUser, propertyTypeName) {
                            setReceiverType(targetClassName)
                            getter(buildKotlinGetter {
                                addModifiers(KModifier.INLINE)
                                if (factoryProperty != null) {
                                    addGetterStatement(factoryClassName, factoryProperty.name)
                                } else {
                                    addInvokeFunctionStatement(true, mixinCast, accessorGetter.name())
                                }
                            })
                            if (accessorSetter != null) {
                                mutable(true)
                                setter(buildKotlinSetter {
                                    addModifiers(KModifier.INLINE)
                                    setParameters(SETTER_ARGUMENT_NAME to propertyTypeName)
                                    if (factoryProperty != null) {
                                        addSetterStatement(factoryClassName, factoryProperty.name, SETTER_ARGUMENT_NAME)
                                    } else {
                                        addInvokeFunctionStatement(
                                            true, mixinCast, accessorSetter.name(), listOf(SETTER_ARGUMENT_NAME)
                                        )
                                    }
                                })
                            }
                        }
                    }

                    declaration is KspFunction -> {
                        val function = declaration
                        val nameByUser = function.name
                        restrictMixinAnnotations(function)
                        logger.kspRequire(function.isAbstract, function) {
                            "Functions inside ${Accessor::class.atName} interfaces must not have a body."
                        }
                        val parameterList = function.parameters.asKJParameterList()
                        val returnType = function.getReturnTypeOrNull()
                        val hasReturnType = returnType != null
                        val accessMethodAnnotation = function.getSingleAnnotationOrNull<AccessMethod>()
                        val accessConstructorAnnotation = function.getSingleAnnotationOrNull<AccessConstructor>()
                        when {
                            accessMethodAnnotation != null && accessConstructorAnnotation == null -> {
                                val invoker = buildInvokerMethod(
                                    accessMethodAnnotation.vanillaName.ifEmpty { nameByUser },
                                    accessMethodAnnotation.isStatic,
                                    returnType,
                                    nameByUser,
                                    parameterList.javaVersion
                                )
                                mixinMethods += invoker

                                val factoryFunction = nullIfNot(accessMethodAnnotation.isStatic) {
                                    buildKotlinFunction(nameByUser) {
                                        setParameters(parameterList.kotlinVersion)
                                        addInvokeFunctionStatement(
                                            hasReturnType, mixinClassName, invoker.name(), parameterList.names
                                        )
                                        setReturnType(returnType)
                                    }
                                }
                                factoryFunctions.addIfNotNull(factoryFunction)

                                extensionFunctions += buildKotlinFunction(nameByUser) {
                                    addModifiers(KModifier.INLINE)
                                    setReceiverType(targetClassName)
                                    setParameters(parameterList.kotlinVersion)
                                    if (factoryFunction != null) {
                                        addInvokeFunctionStatement(
                                            hasReturnType, factoryClassName, factoryFunction.name, parameterList.names
                                        )
                                    } else {
                                        addInvokeFunctionStatement(
                                            hasReturnType, mixinCast, invoker.name(), parameterList.names
                                        )
                                    }
                                    setReturnType(returnType)
                                }
                            }

                            accessConstructorAnnotation != null && accessMethodAnnotation == null -> {
                                logger.kspRequire(!hasReturnType, function) {
                                    "Functions annotated with ${AccessConstructor::class.atName} " +
                                        "must not have a return type."
                                }
                                val invokerMethod = buildInvokerMethod(
                                    Descriptors.CONSTRUCTOR_METHOD_NAME,
                                    true,
                                    targetClassName.typeName,
                                    nameByUser,
                                    parameterList.javaVersion
                                )
                                mixinMethods += invokerMethod
                                val factoryFunction = buildKotlinFunction(nameByUser) {
                                    setParameters(parameterList.kotlinVersion)
                                    setReturnType(targetClassName)
                                    addInvokeFunctionStatement(
                                        true, mixinClassName, invokerMethod.name(), parameterList.names
                                    )
                                }
                                factoryFunctions += factoryFunction
                                topLevelFunctions += buildKotlinFunction(targetClassName.simpleName) {
                                    addModifiers(KModifier.INLINE)
                                    setParameters(parameterList.kotlinVersion)
                                    setReturnType(targetClassName)
                                    addInvokeFunctionStatement(
                                        true, factoryClassName, factoryFunction.name, parameterList.names
                                    )
                                }
                            }

                            else -> {
                                logger.kspError(declaration) {
                                    "Functions inside ${Accessor::class.atName} interfaces " +
                                        "must be annotated with " +
                                        "${AccessMethod::class.atName} or ${AccessConstructor::class.atName}."
                                }
                            }
                        }
                    }

                    declaration is KspClass && declaration.isInterface -> {
                        val nestedInterface = declaration
                        logger.kspRequire(nestedInterface.hasAnnotation<Accessor>(), nestedInterface) {
                            "Nested interface '${nestedInterface.name}' " +
                                "must be annotated with ${Accessor::class.atName}."
                        }
                    }

                    else -> logger.kspError(declaration) {
                        "Only properties, functions, and nested interfaces " +
                            "are allowed inside ${Accessor::class.atName} interfaces."
                    }
                }
            }
            accumulateAccessor(mixinClassName, targetClassName.typeName) {
                symbols += symbol
                methods += mixinMethods
            }
            accumulateExtension(targetClassName) {
                symbols += symbol
                this.topLevelFunctions += topLevelFunctions
                properties += extensionProperties
                functions += extensionFunctions
            }
            accumulateFactory(factoryClassName) {
                symbols += symbol
                properties += factoryProperties
                functions += factoryFunctions
            }
        }
    }

    private fun resolvePatches(resolver: Resolver) {
        resolver.forEachSymbolsAnnotatedWith<Patch> { symbol, _, annotation ->
            logger.kspRequire(symbol is KspClass && symbol.isClass, symbol) {
                "Annotation ${Patch::class.atName} can only be applied to class."
            }
            logger.kspRequire(symbol.isAbstract, symbol) {
                "Class annotated with ${Patch::class.atName} must be abstract."
            }
            val targetClassName = annotation.getClassDeclarationArgument(Patch::target.name).asKJClassName()
            val patchSuperType = symbol.getSuperClassTypeOrNull()
            logger.kspRequire(patchSuperType?.declaration?.isInstance<LapisPatch<*>>() == true, symbol) {
                "Class annotated with ${Patch::class.atName} must extend ${LapisPatch::class.simpleName}."
            }
            val patchGenericType = patchSuperType.genericTypes().singleOrNull()?.asKJTypeName()
            logger.kspRequire(patchGenericType == targetClassName.typeName, symbol) {
                "${LapisPatch::class.simpleName} generic type " +
                    "does not match ${Patch::class.atName} ${Patch::target.name}."
            }

            val patchPackageName = symbol.packageName.asString()
            val implClassName = KJClassName(packageName, symbol.name + "_Impl")
            val mixinClassName = KJClassName(patchPackageName, symbol.name + "_Mixin")
            val bridgeClassName = KJClassName(packageName, symbol.name + "_Bridge")
            val bridgeCast = buildKotlinCast(to = bridgeClassName)

            val bridgeFunctions = mutableListOf<KPFunction>()
            val mixinMethods = mutableListOf<JPMethod>()
            val extensionProperties = mutableListOf<KPProperty>()
            val extensionFunctions = mutableListOf<KPFunction>()

            val lazyPatchGetterCall = JPCodeBlock.of("getOrInitPatch()")
            symbol.properties.forEach { property ->
                restrictMixinAnnotations(property)
                if (property.isPrivate) {
                    return@forEach
                }
                val nameByUser = property.name
                val getterName = nameByUser.prefixed("get")
                val setterName = nameByUser.prefixed("set")
                val bridgeGetterName = getterName.withModId()
                val bridgeSetterName = setterName.withModId()
                val propertyTypeName = property.type.asKJTypeName()
                bridgeFunctions += buildKotlinFunction(bridgeGetterName) {
                    addModifiers(KModifier.ABSTRACT)
                    setReturnType(propertyTypeName)
                }
                mixinMethods += buildJavaMethod(bridgeGetterName) {
                    addAnnotation<Override>()
                    addModifiers(Modifier.PUBLIC)
                    setReturnType(propertyTypeName)
                    addInvokeFunctionStatement(true, lazyPatchGetterCall, getterName)
                }
                if (property.isMutable) {
                    bridgeFunctions += buildKotlinFunction(bridgeSetterName) {
                        addModifiers(KModifier.ABSTRACT)
                        setParameters(SETTER_ARGUMENT_NAME to propertyTypeName)
                    }
                    mixinMethods += buildJavaMethod(bridgeSetterName) {
                        addAnnotation<Override>()
                        addModifiers(Modifier.PUBLIC)
                        setParameters(propertyTypeName to SETTER_ARGUMENT_NAME)
                        addInvokeFunctionStatement(false, lazyPatchGetterCall, setterName, listOf(SETTER_ARGUMENT_NAME))
                    }
                }
                extensionProperties += buildKotlinProperty(nameByUser, propertyTypeName) {
                    setReceiverType(targetClassName)
                    getter(buildKotlinGetter {
                        addModifiers(KModifier.INLINE)
                        addInvokeFunctionStatement(true, bridgeCast, bridgeGetterName)
                    })
                    if (property.isMutable) {
                        mutable(true)
                        setter(buildKotlinSetter {
                            addModifiers(KModifier.INLINE)
                            setParameters(SETTER_ARGUMENT_NAME to propertyTypeName)
                            addInvokeFunctionStatement(
                                false,
                                bridgeCast,
                                bridgeSetterName,
                                listOf(SETTER_ARGUMENT_NAME)
                            )
                        })
                    }
                }
            }
            symbol.functions.forEach { function ->
                restrictMixinAnnotations(function)
                val hookAnnotation = function.getSingleAnnotationOrNull<Hook>()
                if (hookAnnotation != null) {
                    logger.kspRequire(function.isPublic, function) {
                        "Functions annotated with ${Hook::class.atName} must be public."
                    }
                    mixinMethods += resolveHook(function, hookAnnotation, lazyPatchGetterCall)
                } else if (function.isPublic) {
                    val nameByUser = function.name
                    val bridgeName = nameByUser.withModId()
                    val parameterList = function.parameters.asKJParameterList()
                    val returnType = function.getReturnTypeOrNull()
                    val hasReturnType = returnType != null
                    bridgeFunctions += buildKotlinFunction(bridgeName) {
                        addModifiers(KModifier.ABSTRACT)
                        setParameters(parameterList.kotlinVersion)
                        setReturnType(returnType)
                    }
                    mixinMethods += buildJavaMethod(bridgeName) {
                        addAnnotation<Override>()
                        addModifiers(Modifier.PUBLIC)
                        setReturnType(returnType)
                        setParameters(parameterList.javaVersion)
                        addInvokeFunctionStatement(hasReturnType, lazyPatchGetterCall, nameByUser, parameterList.names)
                    }
                    extensionFunctions += buildKotlinFunction(nameByUser) {
                        addModifiers(KModifier.INLINE)
                        setReceiverType(targetClassName)
                        setParameters(parameterList.kotlinVersion)
                        setReturnType(returnType)
                        addInvokeFunctionStatement(hasReturnType, bridgeCast, bridgeName, parameterList.names)
                    }
                    return@forEach
                }
            }
            buildKotlinInterface(bridgeClassName.simpleName) {
                addFunctions(bridgeFunctions)
            }.toKotlinFile(bridgeClassName.packageName) {
                addAnnotation<Suppress> {
                    addStringArrayMember(
                        Suppress::names.name,
                        listOf(
                            "RedundantVisibilityModifier",
                            "ClassName",
                            "unused",
                        )
                    )
                }
            }.writeTo(generator, symbol.toDependencies())
            accumulatePatch(
                mixinClassName,
                targetClassName.typeName,
                symbol.asKJClassName().typeName,
                implClassName,
                bridgeClassName,
            ) {
                symbols += symbol
                methods += mixinMethods
            }
            accumulateExtension(targetClassName) {
                symbols += symbol
                properties += extensionProperties
                functions += extensionFunctions
            }
        }
    }

    private fun resolveHook(function: KspFunction, hookAnnotation: Hook, lazyPatchGetterCall: JPCodeBlock): JPMethod {
        val mixinMethodParameters = mutableListOf<JPParameter>()
        val callArguments = arrayOfNulls<JPCodeBlock>(function.parameters.size)
        val kind = hookAnnotation.kind
        val methodParameter = function.parameters.singleOrNull { it.hasAnnotation<Method>() }
        logger.kspRequire(methodParameter != null, function) {
            "Functions annotated with ${Hook::class.atName} " +
                "must have exactly one parameter annotated with ${Method::class.atName}."
        }
        val methodLambdaType = methodParameter.type.resolve()
        logger.kspRequire(methodLambdaType.isFunctionType, methodParameter) {
            "Hook parameters annotated with ${Method::class.atName} must be a non-suspend function type."
        }
        val methodLambdaGenericTypes = methodLambdaType.genericTypes().map { it.asKJTypeName() }
        val methodLambdaReturnType = methodLambdaGenericTypes.last()
        if (kind == Kind.Method) {
            logger.kspRequire(function.parameters.none { it.hasAnnotation<Original>() }, methodParameter) {
                "Hooks with ${kind.name} kind must not have parameters annotated with ${Original::class.atName}."
            }
            mixinMethodParameters.addLast(
                buildJavaParameter(
                    Operation::class.asClassName().asKJClassName()
                        .parameterizedBy(methodLambdaReturnType.boxed)
                        .javaVersion,
                    "_original"
                )
            )
            callArguments[function.parameters.indexOf(methodParameter)] = JPCodeBlock.of("null")
        } else {
            val originalParameter = function.parameters.singleOrNull { it.hasAnnotation<Original>() }
            logger.kspRequire(originalParameter != null, function) {
                "Hooks with ${kind.name} kind must have exactly one parameter annotated with ${Original::class.atName}."
            }
            val originalType = originalParameter.type.resolve()
            if (kind == Kind.Operation) {
                logger.kspRequire(originalType.isFunctionType, originalParameter) {
                    "Parameters annotated with ${Original::class.atName} in hooks with ${kind.name} kind " +
                        "must be a non-suspend function type."
                }
            }
            callArguments[function.parameters.indexOf(methodParameter)] = JPCodeBlock.of("null")
        }
        val signatureParameters = mutableListOf<KSValueParameter>()
        callArguments.forEachIndexed { index, argument ->
            if (argument == null) {
                val parameter = function.parameters[index]
                callArguments[index] = JPCodeBlock.of(parameter.requireName())
                signatureParameters += parameter
            }
        }
        if (kind == Kind.Method) {
            val lambdaParameterNames = signatureParameters.joinToString { "_" + it.requireName() }
            callArguments[function.parameters.indexOf(methodParameter)] = JPCodeBlock.of(
                buildString {
                    append("(_instance, \$L) -> ")
                    if (methodLambdaReturnType.kotlinVersion == KPUnit) {
                        append("{\n")
                    }
                    append("_original.call(\$L)")
                    if (methodLambdaReturnType.kotlinVersion == KPUnit) {
                        append(";\nreturn null;\n}")
                    }
                },
                lambdaParameterNames,
                lambdaParameterNames,
            )
        }
        mixinMethodParameters.addAll(0, signatureParameters.map { signatureParameter ->
            buildJavaParameter(signatureParameter.type.asKJTypeName(), signatureParameter.requireName())
        })
        val psiMethodLambdaParameter = PsiHelper.loadPsiFile(function)
            .findPsiFunction { it.name == function.name }
            ?.valueParameters
            ?.firstOrNull { it.name == methodParameter.requireName() }
        logger.kspRequire(psiMethodLambdaParameter != null, function) {
            "Unable to resolve parameter from KSP in PSI system."
        }
        val psiCallable = psiMethodLambdaParameter.defaultValue
        logger.kspRequire(psiCallable is PsiCallable, function) {
            "Parameter ${methodParameter.requireName()} must have callable reference in default value."
        }
        val annotation = when {
            kind == Kind.Method -> {
                buildJavaAnnotation<WrapMethod> {
                    addStringMember(
                        "method",
                        Descriptors.forMethod(
                            psiCallable.callableReference.text,
                            null,
                            methodLambdaGenericTypes.drop(1).dropLast(1),
                            methodLambdaReturnType
                        )
                    )
                }
            }

            else -> TODO()
        }
        return buildJavaMethod(function.name.withModId()) {
            addAnnotation(annotation)
            addModifiers(Modifier.PRIVATE)
            addParameters(mixinMethodParameters)
            addStatement(
                "\$L.\$L(\$L)",
                lazyPatchGetterCall,
                function.name,
                callArguments.joinToString { it.toString() },
            )
        }
    }

    private fun buildAccessorMethod(
        methodType: AccessorMethodType, target: String, isStatic: Boolean, type: KJTypeName, propertyName: String,
    ): JPMethod =
        buildJavaMethod(methodType.buildMethodName(propertyName)) {
            val isSetter = methodType == AccessorMethodType.SETTER
            addAnnotation<AccessorAnnotation> {
                addStringMember(DEFAULT_ANNOTATION_ELEMENT_NAME, target)
            }
            if (isSetter) {
                addAnnotation<Mutable>()
            }
            addModifiers(
                Modifier.PUBLIC,
                if (isStatic) Modifier.STATIC else Modifier.ABSTRACT
            )
            if (isSetter) {
                setParameters(type to propertyName)
            }
            if (isStatic && !isSetter) {
                addStubStatement()
            }
            setReturnType(type.takeUnless { isSetter })
        }

    private fun buildInvokerMethod(
        target: String, isStatic: Boolean, returnType: KJTypeName?, name: String, parameters: List<JPParameter>,
    ): JPMethod =
        buildJavaMethod(name.prefixed("invoke")) {
            addAnnotation<Invoker> {
                addStringMember(DEFAULT_ANNOTATION_ELEMENT_NAME, target)
            }
            addModifiers(
                Modifier.PUBLIC,
                if (isStatic) Modifier.STATIC else Modifier.ABSTRACT
            )
            setParameters(parameters)
            if (isStatic) {
                addStubStatement()
            }
            setReturnType(returnType)
        }

    private fun accumulateExtension(targetClassName: KJClassName, block: GeneratedExtension.() -> Unit) {
        extensions
            .getOrPut(targetClassName) { GeneratedExtension() }
            .apply(block)
    }

    private fun accumulateFactory(targetClassName: KJClassName, block: GeneratedFactory.() -> Unit) {
        factories
            .getOrPut(targetClassName) { GeneratedFactory() }
            .apply(block)
    }

    private fun accumulateAccessor(
        className: KJClassName,
        targetTypeName: KJTypeName,
        block: GeneratedAccessor.() -> Unit,
    ) {
        accessors
            .getOrPut(className) { GeneratedAccessor(targetTypeName) }
            .apply(block)
    }

    private fun accumulatePatch(
        className: KJClassName,
        targetTypeName: KJTypeName,
        patchTypeName: KJTypeName,
        implClassName: KJClassName,
        bridgeClassName: KJClassName,
        block: GeneratedPatch.() -> Unit,
    ) {
        patches
            .getOrPut(className) {
                GeneratedPatch(
                    targetTypeName = targetTypeName,
                    patchTypeName = patchTypeName,
                    implClassName = implClassName,
                    bridgeClassName = bridgeClassName,
                )
            }
            .apply(block)
    }

    private fun restrictMixinAnnotations(declaration: KspDeclaration) {
        mixinAnnotations.forEach { annotation ->
            logger.kspRequire(!declaration.hasAnnotation(annotation), declaration) {
                """
                Direct use of Mixin or MixinExtras annotations is restricted.
                The ${annotation.atName} annotation is managed internally by Lapis Hooks.
                Please use the corresponding @Hook* annotation instead.
                """.trimIndent()
            }
        }
    }

    private fun reset() {
        extensions.clear()
        factories.clear()
        accessors.clear()
        patches.clear()

        wideners.clear()

        PsiHelper.destroy()
    }

    private fun String.withModId(): String =
        "${modId}_$this"

    private enum class AccessorMethodType(val prefix: String) {

        GETTER("get"),
        SETTER("set");

        fun buildMethodName(originalName: String): String =
            originalName.prefixed(prefix)
    }

    class GeneratedExtension(
        val symbols: MutableSet<KspAnnotated> = mutableSetOf(),
        val typeAliases: MutableList<KPTypeAlias> = mutableListOf(),
        val topLevelFunctions: MutableList<KPFunction> = mutableListOf(),
        val properties: MutableList<KPProperty> = mutableListOf(),
        val functions: MutableList<KPFunction> = mutableListOf(),
    ) {
        fun isEmpty(): Boolean =
            typeAliases.isEmpty() && topLevelFunctions.isEmpty() && properties.isEmpty() && functions.isEmpty()
    }

    class GeneratedFactory(
        val symbols: MutableSet<KspAnnotated> = mutableSetOf(),
        val properties: MutableList<KPProperty> = mutableListOf(),
        val functions: MutableList<KPFunction> = mutableListOf(),
    ) {
        fun isEmpty(): Boolean =
            properties.isEmpty() && functions.isEmpty()
    }

    class GeneratedAccessor(
        val targetTypeName: KJTypeName,
        val symbols: MutableSet<KspAnnotated> = mutableSetOf(),
        val methods: MutableList<JPMethod> = mutableListOf(),
    ) {
        fun isEmpty(): Boolean =
            methods.isEmpty()
    }

    class GeneratedPatch(
        val targetTypeName: KJTypeName,
        val patchTypeName: KJTypeName,
        val implClassName: KJClassName,
        val bridgeClassName: KJClassName,
        val symbols: MutableSet<KspAnnotated> = mutableSetOf(),
        val methods: MutableList<JPMethod> = mutableListOf(),
    ) {
        fun isEmpty(): Boolean =
            methods.isEmpty()
    }

    companion object {
        private const val DEFAULT_ANNOTATION_ELEMENT_NAME: String = "value"
        private const val SETTER_ARGUMENT_NAME: String = "newValue"

        private val mixinAnnotations: List<KClass<out Annotation>> = listOf(
            Overwrite::class,
            Debug::class,
            Dynamic::class,
            Final::class,
            Intrinsic::class,
            Mutable::class,
            Shadow::class,
            SoftOverride::class,
            Unique::class,
            AccessorAnnotation::class,
            Invoker::class,
            Inject::class,
            ModifyArg::class,
            ModifyArgs::class,
            ModifyConstant::class,
            ModifyVariable::class,
            Redirect::class,
            Surrogate::class,

            Expression::class,
            Definition::class,
            Definitions::class,
            Expressions::class,
            ModifyExpressionValue::class,
            ModifyReceiver::class,
            ModifyReturnValue::class,
            @Suppress("DEPRECATION") com.llamalad7.mixinextras.injector.WrapWithCondition::class,
            WrapWithCondition::class,
            WrapMethod::class,
            WrapOperation::class,
        )
    }
}

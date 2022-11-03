package com.ktomek.yamv.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import com.ktomek.yamv.annotations.AutoBaseOutcome
import com.ktomek.yamv.annotations.AutoOutcome
import com.ktomek.yamv.annotations.AutoReducer
import com.ktomek.yamv.annotations.AutoState
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import getFeatureClasses
import processFeatures

class YamvProcessor(
    internal val codeGenerator: CodeGenerator,
    internal val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
//        val allFiles = resolver.getAllFiles().toList()
//        val dirtyFiles = resolver.getNewFiles().toList()
//        logger.warn("ktomek dirtyFiles: ${dirtyFiles.size} allFiles: ${allFiles.size}")
//        logger.warn("ktomek dirtyFiles: ${dirtyFiles.joinToString(",")}")

        val autoStateClasses =
            resolver.getSymbolsWithAnnotation(AutoState::class.qualifiedName.toString())
                .filterIsInstance<KSClassDeclaration>()

        if (autoStateClasses.toList().isNotEmpty()) {
            autoStateClasses.forEach { autoStateClass ->
                if (!autoStateClass.validate()) {
                    return@forEach
                }

                val packageName = autoStateClass.packageName.asString()
                val stateName = autoStateClass.simpleName.asString()
                val stateOutcomeName = "${stateName.replace("State", "")}Outcome"
                val effectOutcomeName = "${stateName.replace("State", "")}EffectOutcome"
                val intentionOutcomeName = "${stateName.replace("State", "")}IntentionOutcome"

                val stateOutcomeClassQualifiedName = "$packageName.$stateOutcomeName"

                // Check if the Outcome interface already exists.
                val existingOutcomeClass = resolver.getClassDeclarationByName(
                    resolver.getKSNameFromString(stateOutcomeClassQualifiedName)
                )
                if (existingOutcomeClass == null) {
                    generateOutcomeInterface(
                        packageName,
                        stateName,
                        stateOutcomeName,
                        listOf<KSDeclaration>(autoStateClass)
                    )
                    generateEffectOutcomeInterface(
                        packageName,
                        stateName,
                        stateOutcomeName,
                        effectOutcomeName,
                        listOf<KSDeclaration>(autoStateClass)
                    )
                }
            }

            autoStateClasses.forEach { autoStateClass ->
                if (!autoStateClass.validate()) return@forEach

                val packageName = autoStateClass.packageName.asString()
                val stateName = autoStateClass.simpleName.asString()
                val stateOutcomeName = "${stateName.replace("State", "")}Outcome"
                val effectOutcomeName = "${stateName.replace("State", "")}EffectOutcome"

                val stateOutcomeClassQualifiedName =
                    "${autoStateClass.packageName.asString()}.$stateOutcomeName"

                // Check if the Outcome interface already exists.
                val existingOutcomeClass = resolver.getClassDeclarationByName(
                    resolver.getKSNameFromString(stateOutcomeClassQualifiedName)
                )

                val annotation = autoStateClass
                    .annotations
                    .first { it.shortName.asString() == AutoState::class.simpleName } // "AutoState" }
                val defaultState = annotation
                    .arguments
                    .firstOrNull { it.name?.asString() == "defaultState" }
                    ?.value as? KSType

                if (existingOutcomeClass == null) {
                    generateStoreClass(stateName, stateOutcomeName, effectOutcomeName, packageName)
                    generateStateContainerClass(
                        stateName,
                        stateOutcomeName,
                        effectOutcomeName,
                        packageName,
                        defaultState
                    )
                    generateFactoryInterface(
                        stateName,
                        stateOutcomeName,
                        effectOutcomeName,
                        packageName
                    )
                    generateDaggerModule(stateName, stateOutcomeName, packageName)
                }
            }
            val reducerClasses = resolver
                .getSymbolsWithAnnotation(AutoReducer::class.qualifiedName.toString())
                .filterIsInstance<KSDeclaration>()
            val autoOutcomeClasses = resolver
                .getSymbolsWithAnnotation(AutoOutcome::class.qualifiedName.toString())
                .filterIsInstance<KSDeclaration>()
            val defferClasses = getFeatureClasses(resolver) + reducerClasses.toList() +
                autoOutcomeClasses.toList()
            return defferClasses
        } else {
            val resultsClasses = resolver
                .getSymbolsWithAnnotation(AutoBaseOutcome::class.qualifiedName.toString())
                .filterIsInstance<KSClassDeclaration>()

            if (resultsClasses.toList().isNotEmpty()) {
                processFeatures(resolver)
                processReducers(resolver)
                return resolver
                    .getSymbolsWithAnnotation(AutoReducer::class.qualifiedName.toString())
                    .filterIsInstance<KSDeclaration>()
                    .toList() +
                    resolver
                        .getSymbolsWithAnnotation(AutoOutcome::class.qualifiedName.toString())
                        .filterIsInstance<KSDeclaration>()
                        .toList()
            }
        }
        return processReducer(resolver)
    }

    private fun generateStoreClass(
        stateName: String,
        resultName: String,
        resultEffectName: String,
        packageName: String
    ) {
        val className = "${stateName}Store"
        val fileBuilder = FileSpec.builder(packageName, className)
        val classBuilder = TypeSpec.classBuilder(className)
            .addAnnotation(ClassName("dagger.hilt.android.lifecycle", "HiltViewModel"))
            .primaryConstructor(
                FunSpec
                    .constructorBuilder()
                    .addAnnotation(
                        AnnotationSpec.builder(ClassName("javax.inject", "Inject"))
                            .build()
                    )
                    .addParameter(
                        "factory",
                        ClassName(packageName, "${stateName}ContainerFactory")
                    )
                    .build()
            )
            .superclass(
                ClassName("com.ktomek.yamv.state", "ViewModelStateStore").parameterizedBy(
                    ClassName(packageName, stateName),
                    ClassName(packageName.replace("state", "outcome"), resultName),
                    ClassName(packageName.replace("state", "outcome"), resultEffectName)
                )
            )
            .addSuperclassConstructorParameter("factory")

        fileBuilder.addType(classBuilder.build()).build()
            .writeTo(codeGenerator, Dependencies(false))
    }

    private fun generateStateContainerClass(
        stateName: String,
        resultName: String,
        resultEffectName: String,
        packageName: String,
        defaultState: KSType?
    ) {
        val className = "${stateName}Container"
        val fileBuilder = FileSpec.builder(packageName, className)
        val classBuilder = TypeSpec.classBuilder(className)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addAnnotation(
                        AnnotationSpec.builder(ClassName("dagger.assisted", "AssistedInject"))
                            .build()
                    )
                    .addParameter(
                        "intentionDispatcher",
                        ClassName(
                            "com.ktomek.yamv.intention",
                            "DefaultIntentionDispatcher"
                        ).parameterizedBy(
                            ClassName(packageName.replace("state", "outcome"), resultName)
                        )
                    )
                    .addParameter(
                        "reducer",
                        ClassName("com.ktomek.yamv.core", "Reducer").parameterizedBy(
                            ClassName(
                                packageName,
                                stateName
                            ),
                            ClassName(packageName.replace("state", "outcome"), resultName)
                        )
                    )
                    .addParameter(
                        "store",
                        ClassName("com.ktomek.yamv.state", "DefaultStore"),
                    )
                    .addParameter(
                        ParameterSpec.builder(
                            "scope",
                            ClassName("kotlinx.coroutines", "CoroutineScope")
                        )
                            .addAnnotation(ClassName("dagger.assisted", "Assisted"))
                            .build()
                    )
                    .build()
            )
            .superclass(
                ClassName("com.ktomek.yamv.state", "StateContainer")
                    .parameterizedBy(
                        ClassName(
                            packageName,
                            stateName
                        ),
                        ClassName(packageName.replace(".state", ".outcome"), resultName),
                        ClassName(packageName.replace(".state", ".outcome"), resultEffectName),
                    )
            )
            .addSuperclassConstructorParameter("intentionDispatcher, reducer, store, scope")
            .addProperty(
                PropertySpec.builder(
                    "defaultState",
                    ClassName(packageName, stateName)
                )
                    .addModifiers(KModifier.OVERRIDE)
                    .getter(
                        FunSpec.getterBuilder()
                            .apply {
                                if (defaultState != null && !isNoDefaultState(defaultState.declaration)) {
                                    addStatement(
                                        "return %T",
                                        ClassName(
                                            packageName,
                                            "$stateName.${defaultState.declaration.simpleName.asString()}"
                                        )
                                    )
                                } else {
                                    addStatement("return %T()", ClassName(packageName, stateName))
                                }
                            }
                            .build()
                    )
                    .build()
            )
            .addProperty(
                PropertySpec
                    .builder(
                        "stateType",
                        Class::class.asTypeName().parameterizedBy(ClassName(packageName, stateName))
                    )
                    .addModifiers(KModifier.OVERRIDE)
                    .getter(
                        FunSpec.getterBuilder()
                            .addCode("return $stateName::class.java")
                            .build()
                    )
                    .build()
            )

        fileBuilder.addType(classBuilder.build()).build()
            .writeTo(codeGenerator, Dependencies(false))
    }

    private fun isNoDefaultState(declaration: KSDeclaration): Boolean =
        declaration.simpleName.asString() == "NoDefaultState"

    private fun generateFactoryInterface(
        stateName: String,
        resultName: String,
        resultEffectName: String,
        packageName: String
    ) {
        val interfaceName = "${stateName}ContainerFactory"
        val fileBuilder = FileSpec.builder(packageName, interfaceName)
        val interfaceBuilder = TypeSpec.interfaceBuilder(interfaceName)
            .addSuperinterface(
                ClassName("com.ktomek.yamv.state", "StateContainerFactory")
                    .parameterizedBy(
                        ClassName(packageName, stateName),
                        ClassName(packageName.replace(".state", ".outcome"), resultName),
                        ClassName(packageName.replace(".state", ".outcome"), resultEffectName)
                    )
            )
            .addAnnotation(ClassName("dagger.assisted", "AssistedFactory"))
            .addFunction(
                FunSpec.builder("create")
                    .addParameter("scope", ClassName("kotlinx.coroutines", "CoroutineScope"))
                    .returns(ClassName(packageName, "${stateName}Container"))
                    .addModifiers(KModifier.ABSTRACT, KModifier.OVERRIDE)
                    .build()
            )

        fileBuilder.addType(interfaceBuilder.build()).build()
            .writeTo(codeGenerator, Dependencies(false))
    }

    private fun generateDaggerModule(
        stateName: String,
        outcomeName: String,
        packageName: String
    ) {
        val moduleName = "${stateName}Module"
        val fileBuilder = FileSpec.builder(packageName, moduleName)
        val outcomePackage = packageName.replace(".state", ".outcome")
        val moduleBuilder = TypeSpec.interfaceBuilder(moduleName)
            .addAnnotation(ClassName("dagger", "Module"))
            .addAnnotation(
                AnnotationSpec.builder(ClassName("dagger.hilt", "InstallIn"))
                    .addMember(
                        "%T::class",
                        ClassName("dagger.hilt.android.components", "ViewModelComponent")
                    )
                    .build()
            )

        val companion = TypeSpec.companionObjectBuilder()
            .addFunction(
                FunSpec.builder("provideDefaults")
                    .addAnnotation(ClassName("dagger", "Provides"))
                    .addAnnotation(ClassName("dagger.multibindings", "ElementsIntoSet"))
                    .addAnnotation(ClassName("dagger.hilt.android.scopes", "ViewModelScoped"))
                    .returns(
                        Set::class.asClassName().parameterizedBy(
//                            WildcardTypeName.producerOf(
                            ClassName(
                                "com.ktomek.yamv.feature",
                                "Feature"
                            ).parameterizedBy(ClassName(outcomePackage, outcomeName))
//                            )
                        )
                    )
                    .addStatement("return emptySet()")
                    .build()
            )
            .build()

        val bindsIntentionDispatcher = FunSpec.builder("bindsIntentionDispatcher")
            .addAnnotation(ClassName("dagger", "Binds"))
            .addAnnotation(ClassName("dagger.hilt.android.scopes", "ViewModelScoped"))
            .returns(
                ClassName("com.ktomek.yamv.intention", "IntentionDispatcher").parameterizedBy(
                    ClassName(outcomePackage, outcomeName)
                )
            )
            .addParameter(
                "it",
                ClassName(
                    "com.ktomek.yamv.intention",
                    "DefaultIntentionDispatcher"
                ).parameterizedBy(ClassName(outcomePackage, outcomeName))
            )
            .addModifiers(KModifier.ABSTRACT)
            .build()

        val bindsReducer = FunSpec.builder("binds${stateName}Reducer")
            .addAnnotation(ClassName("dagger", "Binds"))
            .addAnnotation(ClassName("dagger.hilt.android.scopes", "ViewModelScoped"))
            .returns(
                ClassName("com.ktomek.yamv.core", "Reducer").parameterizedBy(
                    ClassName(packageName, stateName),
                    ClassName(outcomePackage, outcomeName)
                )
            )
            .addParameter(
                "it",
                ClassName(
                    "com.ktomek.yamv.reducer",
                    "DefaultReducer"
                ).parameterizedBy(
                    ClassName(packageName, stateName),
                    ClassName(outcomePackage, outcomeName)
                )
            )
            .addModifiers(KModifier.ABSTRACT)
            .build()

        moduleBuilder.addType(companion)
        moduleBuilder.addFunction(bindsIntentionDispatcher)
        moduleBuilder.addFunction(bindsReducer)

        fileBuilder.addType(moduleBuilder.build()).build()
            .writeTo(codeGenerator, Dependencies(false))
    }
}

class YamvProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        YamvProcessor(environment.codeGenerator, environment.logger)
}

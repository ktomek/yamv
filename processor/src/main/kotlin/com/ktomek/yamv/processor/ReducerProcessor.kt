package com.ktomek.yamv.processor

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate
import com.ktomek.yamv.annotations.AutoOutcome
import com.ktomek.yamv.annotations.AutoReducer
import com.ktomek.yamv.core.Reducer
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

internal fun YamvProcessor.processReducer(resolver: Resolver): List<KSDeclaration> {
    val resultsClasses = resolver
        .getSymbolsWithAnnotation(AutoOutcome::class.qualifiedName.toString())
        .filterIsInstance<KSDeclaration>()
        .toList()

    val reducerClasses = resolver
        .getSymbolsWithAnnotation(AutoReducer::class.qualifiedName.toString())
        .filterIsInstance<KSDeclaration>()
        .toList()

    resultsClasses.forEach {
        logger.warn("ktomek AutoOutcome: $it is valid ${it.validate()}")
    }

    reducerClasses.forEach {
        logger.warn("ktomek AutoReducer: $it is valid ${it.validate()}")
    }

    if (resultsClasses.isEmpty()) {
        return reducerClasses.toList()
    }

    val notValid = reducerClasses.filter { !it.validate() }
    if (notValid.isNotEmpty()) {
        return notValid
    }

    val stateOutcomes = resultsClasses.groupBy {
        findStateType(it)
    }
        .filterKeys { it != null }
        .mapKeys { it.key!! }

    val stateReducers = reducerClasses
        .groupBy {
            when (it) {
                is KSClassDeclaration -> {
                    logger.warn("ktomek reducerClass: $it")
                    it.superTypes
                        .map {
                            logger.warn("ktomek reducer supertype: $it")
                            it.resolve().arguments.forEach {
                                logger.warn("ktomek reducer supertype argument: $it")
                            }
                            it.resolve().arguments[0].type?.resolve()?.declaration
                        }
                        .firstOrNull()
                }

                is KSPropertyDeclaration -> {
                    logger.warn("ktomek reducerProperty: $it")
                    it.type.resolve()
                        .arguments[0]
                        .type
                        ?.resolve()
                        ?.declaration
                }

                else -> null
            }
        }

    stateOutcomes.forEach {
        logger.warn("ktomek states: ${it.key}")
        generateOutcomeReducers(
            it.key,
            stateOutcomes[it.key] ?: emptyList(),
            stateReducers[it.key] ?: emptyList(),
        )
    }

    return emptyList()
}

private fun YamvProcessor.generateOutcomeReducers(
    state: KSDeclaration,
    outcomes: List<KSDeclaration>,
    reducers: List<KSDeclaration>
) {
    val packageName = state.packageName.asString()
    val outcomePackage = packageName.replace(".state", ".outcome")
    val outcomeReducer = "${state.simpleName.asString()}Reducer"
    val fileBuilder = FileSpec.builder(packageName, outcomeReducer)

    val stateType = ClassName(packageName, state.simpleName.asString())
    val outcomeName = "${state.simpleName.asString().replace("State", "")}Outcome"
    val outcomeType = ClassName(outcomePackage, outcomeName)

    logger.warn("ktomek packageName: $packageName, stateType: $stateType, outcomeType: $outcomeType")
    logger.warn("ktomek outcomeReducer: $outcomeReducer")
    val reducerBuilder = TypeSpec
        .classBuilder(outcomeReducer)
        .addSuperinterface(
            Reducer::class
                .asClassName()
                .parameterizedBy(stateType, outcomeType)
        )
        .addFunction(
            FunSpec.builder("reduce")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("prevState", stateType)
                .addParameter("outcome", outcomeType)
                .returns(stateType)
                .apply {
                    val whenStatement = CodeBlock.builder()
                        .beginControlFlow("return when (outcome)")
                        .apply {
                            addStatement(
                                "    is %T -> outcome.reduce(prevState)",
                                ClassName(outcomePackage, "${outcomeName}WithReducer")
                            )
                            outcomes.forEach { outcome ->
                                when (outcome) {
                                    is KSClassDeclaration -> {
                                        when (hasSupertypeOutcomeWithReducer(outcome)) {
                                            true -> Unit

                                            false -> {
                                                when (
                                                    val reducer =
                                                        findReducerType(reducers, outcome)
                                                ) {
                                                    is KSClassDeclaration -> {
                                                        if (reducer.classKind == ClassKind.OBJECT) {
                                                            addStatement(
                                                                "    is %T -> %T.reduce(prevState, outcome)",
                                                                outcome.toClassName(),
                                                                reducer.toClassName(),
                                                            )
                                                        } else {
                                                            addStatement(
                                                                "    is %T -> %T().reduce(prevState, outcome)",
                                                                outcome.toClassName(),
                                                                reducer.toClassName(),
                                                            )
                                                        }
                                                    }

                                                    is KSPropertyDeclaration -> {
                                                        addStatement(
                                                            "    is %T -> %T.reduce(prevState, outcome)",
                                                            outcome.toClassName(),
                                                            reducer.toPropertyName(),
                                                        )
                                                    }

                                                    else -> logger.error("$reducer reducer for outcome: $outcome")
                                                }
                                            }
                                        }
                                    }

                                    is KSPropertyDeclaration -> {
                                        when (hasSupertypeOutcomeWithReducer(outcome)) {
                                            true -> Unit
//                                                addStatement(
//                                                "      %T -> %T.reduce(prevState)",
//                                                outcome.toPropertyName(),
//                                                outcome.toPropertyName()
//                                            )

                                            false -> Unit
                                        }
                                    }
                                }
                            }
                        }
                        .addStatement("    else -> prevState") // Default case
                        .endControlFlow()
                        .build()

                    addStatement(whenStatement.toString())
                }
                .build()
        )

    fileBuilder
        .addType(reducerBuilder.build())
        .build()
        .writeTo(codeGenerator, Dependencies(false))
}

fun YamvProcessor.findStateType(declaration: KSDeclaration): KSDeclaration? =
    when (declaration) {
        is KSClassDeclaration -> findStateTypeFromClass(declaration)
        is KSPropertyDeclaration -> findStateTypeFromProperty(declaration)
        else -> null // Other types are not handled
    }

fun YamvProcessor.findStateTypeFromClass(declaration: KSClassDeclaration): KSDeclaration? =
    declaration.getAllSuperTypes()
        .find { it.declaration.qualifiedName?.asString()?.contains("StateOutcome") ?: false }
        ?.arguments
        ?.firstOrNull()
        ?.type
        ?.resolve()
        ?.declaration

fun YamvProcessor.findReducerType(
    reducers: List<KSDeclaration>,
    outcome: KSClassDeclaration
): KSDeclaration? = reducers.firstOrNull {
    when (it) {
        is KSClassDeclaration -> findReducerTypeFromClass(it, outcome)
        is KSPropertyDeclaration -> findReducerTypeFromProperty(it, outcome)
        else -> false
    }
}

fun YamvProcessor.findReducerTypeFromClass(
    declaration: KSClassDeclaration,
    outcome: KSClassDeclaration
): Boolean =
    if (declaration.simpleName.asString() == "Reducer") {
        declaration
            .typeParameters[1] == outcome
    } else {
        declaration
            .getAllSuperTypes()
            .any {
                if (it.declaration.simpleName.asString() == "Reducer") {
                    it.arguments[1]
                        .type
                        ?.resolve()
                        ?.declaration == outcome
                } else {
                    false
                }
            }
    }

fun YamvProcessor.findReducerTypeFromProperty(
    declaration: KSPropertyDeclaration,
    outcome: KSClassDeclaration
): Boolean {
    val resolvedType = declaration.type.resolve()
    val superDeclaration = resolvedType.declaration
    return if (superDeclaration is KSClassDeclaration &&
        superDeclaration.simpleName.asString() == "Reducer"
    ) {
        resolvedType
            .arguments[1]
            .type
            ?.resolve()
            ?.declaration == outcome
    } else if (superDeclaration is KSClassDeclaration) {
        findReducerTypeFromClass(superDeclaration, outcome)
    } else {
        false
    }
}

fun YamvProcessor.findStateTypeFromProperty(declaration: KSPropertyDeclaration): KSDeclaration? {
    val resolvedType = declaration.type.resolve()
    return when (val superDeclaration = resolvedType.declaration) {
        is KSClassDeclaration -> findStateTypeFromClass(superDeclaration)
        is KSFunctionDeclaration -> superDeclaration.returnType?.resolve()?.declaration
        else -> null // Adjust this based on your specific model or use case
    }
}

fun YamvProcessor.hasSupertypeOutcomeWithReducer(declaration: KSDeclaration): Boolean =
    when (declaration) {
        is KSClassDeclaration ->
            declaration
                .getAllSuperTypes()
                .any { it.declaration.qualifiedName?.asString() == "com.ktomek.yamv.reducer.OutcomeWithReducer" }

        is KSPropertyDeclaration -> checkSupertypes(declaration)
        else -> false
    }

private fun YamvProcessor.checkSupertypes(declaration: KSPropertyDeclaration): Boolean {
    val resolvedType = declaration.type.resolve()
    return when (val superDeclaration = resolvedType.declaration) {
        is KSClassDeclaration -> {
            superDeclaration
                .getAllSuperTypes()
                .any {
                    it.declaration.qualifiedName?.asString()?.contains("OutcomeWithReducer")
                        ?: false
                }
        }

        else -> false // Adjust this based on your specific model or use case
    }
}

fun KSPropertyDeclaration.toPropertyName(): ClassName =
    ClassName(packageName.asString(), simpleName.asString())

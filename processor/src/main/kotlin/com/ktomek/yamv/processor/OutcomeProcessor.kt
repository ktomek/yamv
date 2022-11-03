package com.ktomek.yamv.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSDeclaration
import com.ktomek.yamv.annotations.AutoBaseOutcome
import com.ktomek.yamv.core.EffectOutcome
import com.ktomek.yamv.core.StateOutcome
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlin.reflect.KClass

internal fun YamvProcessor.generateOutcomeInterface(
    packageName: String,
    stateName: String,
    resultName: String,
    dependencies: List<KSDeclaration>
) {
    val outcomePackage = packageName.replace("state", "outcome")
    val fileBuilder = FileSpec.builder(outcomePackage, resultName)

    val resultInterfaceBuilder = TypeSpec.interfaceBuilder(resultName)
        .addAnnotation(AutoBaseOutcome::class.asTypeName())
        .addSuperinterface(
            StateOutcome::class.asTypeName().parameterizedBy(
                ClassName(
                    packageName,
                    stateName
                )
            )
        )

    // Adding an empty result object inside the interface
    val emptyOutcomeObject = TypeSpec.objectBuilder("EmptyOutcome")
        .addSuperinterface(ClassName(outcomePackage, resultName))
        .build()

    resultInterfaceBuilder.addType(emptyOutcomeObject)

    fileBuilder
        .addType(resultInterfaceBuilder.build())
        .build()
        .writeTo(
            codeGenerator,
            Dependencies(true, *dependencies.mapNotNull { it.containingFile }.toTypedArray())
        )

    generateOutcomeKeyAnnotation(codeGenerator, outcomePackage, resultName)
    generateOutcomeWithReducerInterface(codeGenerator, outcomePackage, resultName, stateName)
}

private fun generateOutcomeKeyAnnotation(
    codeGenerator: CodeGenerator,
    packageName: String,
    resultName: String
) {
    val resultKey = TypeSpec.annotationBuilder("${resultName}Key")
        .addModifiers(KModifier.INTERNAL)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter(
                    "value",
                    KClass::class.asTypeName().parameterizedBy(
                        WildcardTypeName.producerOf(
                            ClassName(packageName, resultName)
                        )
                    )
                )
                .build()
        )
        .addAnnotation(
            AnnotationSpec.builder(Retention::class)
                .addMember("%T.RUNTIME", AnnotationRetention::class)
                .build()
        )
        .addAnnotation(
            AnnotationSpec.builder(Target::class)
                .addMember(
                    "%T.FUNCTION, %T.PROPERTY_GETTER, %T.PROPERTY_SETTER",
                    AnnotationTarget::class,
                    AnnotationTarget::class,
                    AnnotationTarget::class
                )
                .build()
        )
        .addAnnotation(ClassName("dagger", "MapKey"))
        .addProperty(
            PropertySpec.builder(
                "value",
                KClass::class.asTypeName().parameterizedBy(
                    WildcardTypeName.producerOf(
                        ClassName(packageName, resultName)
                    )
                )
            )
                .initializer("value")
                .build()
        )
        .build()

    val fileSpec = FileSpec.builder(packageName, "${resultName}Key")
        .addType(resultKey)
        .build()

    fileSpec.writeTo(codeGenerator, Dependencies(false))
}

private fun generateOutcomeWithReducerInterface(
    codeGenerator: CodeGenerator,
    packageName: String,
    resultName: String,
    stateName: String
) {
    val statePackage = packageName.replace(".outcome", ".state")
    val stateClassName = ClassName(statePackage, stateName)
    val resultClassName = ClassName(packageName, resultName)
    val resultWithReducerClassName = ClassName("com.ktomek.yamv.reducer", "OutcomeWithReducer")
    val interfaceName = "${resultName}WithReducer"

    val funInterfaceSpec = TypeSpec.funInterfaceBuilder(interfaceName)
        .addSuperinterface(resultWithReducerClassName.parameterizedBy(stateClassName))
        .addSuperinterface(resultClassName)
        .addFunction(
            FunSpec.builder("reduce")
                .addModifiers(KModifier.ABSTRACT)
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(ParameterSpec.builder("prevState", stateClassName).build())
                .returns(stateClassName)
                .build()
        )
        .build()

    val fileSpec = FileSpec.builder(packageName, interfaceName)
        .addType(funInterfaceSpec)
        .build()

    fileSpec.writeTo(codeGenerator, Dependencies(false))
}

internal fun YamvProcessor.generateEffectOutcomeInterface(
    packageName: String,
    stateName: String,
    resultName: String,
    resultEffectName: String,
    dependencies: List<KSDeclaration>
) {
    val outcomePackage = packageName.replace("state", "outcome")
    val fileBuilder = FileSpec.builder(outcomePackage, resultEffectName)

    val resultInterfaceBuilder = TypeSpec.interfaceBuilder(resultEffectName)
        .addModifiers(KModifier.SEALED)
        .addSuperinterface(ClassName(outcomePackage, resultName))
        .addSuperinterface(
            EffectOutcome::class.asTypeName().parameterizedBy(
                ClassName(
                    packageName,
                    stateName
                )
            )
        )

    fileBuilder
        .addType(resultInterfaceBuilder.build())
        .build()
        .writeTo(
            codeGenerator,
            Dependencies(true, *dependencies.mapNotNull { it.containingFile }.toTypedArray())
        )
}

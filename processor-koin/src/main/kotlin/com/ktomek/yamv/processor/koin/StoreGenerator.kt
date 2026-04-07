package com.ktomek.yamv.processor.koin

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.ktomek.yamv.annotations.AutoState
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * Generates a plain Store class (no ViewModel, no Hilt) for each `@AutoState` class.
 *
 * Generated output example:
 * ```kotlin
 * class CounterStateStore(
 *     private val features: Set<Feature<CounterState>>,
 * ) {
 *     val store: MviStore<CounterState, Any> = MviRuntime(
 *         features = features,
 *         defaultState = CounterState(),
 *     )
 * }
 * ```
 */
internal class StoreGenerator(private val codeGenerator: CodeGenerator) {

    fun generate(stateClass: KSClassDeclaration) {
        val packageName = stateClass.packageName.asString()
        val stateName = stateClass.simpleName.asString()
        val stateClassName = ClassName(packageName, stateName)
        val defaultStateClass = resolveDefaultState(stateClass, packageName, stateName)
        val storeClassName = "${stateName}Store"

        FileSpec.builder(packageName, storeClassName)
            .addType(buildStoreClass(storeClassName, stateClassName, defaultStateClass))
            .build()
            .writeTo(codeGenerator, Dependencies(false))
    }

    private fun resolveDefaultState(
        stateClass: KSClassDeclaration,
        packageName: String,
        stateName: String,
    ): ClassName {
        val defaultStateType = stateClass.findAutoStateDefaultType()
        return if (defaultStateType != null && !defaultStateType.declaration.isNoDefaultState()) {
            ClassName(packageName, "$stateName.${defaultStateType.declaration.simpleName.asString()}")
        } else {
            ClassName(packageName, stateName)
        }
    }

    private fun buildStoreClass(
        className: String,
        stateClass: ClassName,
        defaultStateClass: ClassName,
    ): TypeSpec {
        val featuresType = ClassName("kotlin.collections", "Set").parameterizedBy(
            YamvKoinClassNames.Feature.parameterizedBy(stateClass)
        )
        val featuresParam = ParameterSpec.builder("features", featuresType).build()
        val featuresProp = PropertySpec.builder("features", featuresType)
            .addModifiers(KModifier.PRIVATE)
            .initializer("features")
            .build()

        return TypeSpec.classBuilder(className)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(featuresParam)
                    .build()
            )
            .addProperty(featuresProp)
            .addProperty(buildStoreProperty(stateClass, defaultStateClass))
            .build()
    }

    private fun buildStoreProperty(stateClass: ClassName, defaultStateClass: ClassName): PropertySpec =
        PropertySpec.builder(
            "store",
            YamvKoinClassNames.MviStore.parameterizedBy(stateClass, ClassName("kotlin", "Any"))
        )
            .initializer(
                "%T(\n  features = features,\n  defaultState = %T(),\n)",
                YamvKoinClassNames.MviRuntime,
                defaultStateClass,
            )
            .build()
}

private fun KSClassDeclaration.findAutoStateDefaultType(): KSType? =
    annotations
        .firstOrNull { it.shortName.asString() == AutoState::class.simpleName }
        ?.arguments
        ?.firstOrNull { it.name?.asString() == "defaultState" }
        ?.value as? KSType

private fun KSDeclaration.isNoDefaultState(): Boolean =
    simpleName.asString() == "NoDefaultState"

package com.ktomek.yamv.processor.hilt

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.ktomek.yamv.annotations.AutoState
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.jvm.jvmSuppressWildcards
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * Generates a `@HiltViewModel`-annotated subclass of [MviRetainedStore] for each `@AutoState` class.
 *
 * Generated output example:
 * ```kotlin
 * @HiltViewModel
 * class CounterStateStore @Inject constructor(
 *     features: Set<@JvmSuppressWildcards Feature<CounterState>>,
 *     defaultConfig: Optional<CoroutineDispatcherConfig>,
 *     @MviDispatcherConfig(CounterState::class) optionalDispatcherConfig: Optional<CoroutineDispatcherConfig>,
 * ) : MviRetainedStore<CounterState, Any>() {
 *     override val dispatcherConfig: CoroutineDispatcherConfig =
 *         optionalDispatcherConfig.orElseGet { defaultConfig.orElseGet { DefaultCoroutineDispatcherConfig() } }
 *     override val store: MviStore<CounterState, Any> = MviRuntime(
 *         features = features,
 *         defaultState = CounterState(),
 *         dispatcherConfig = dispatcherConfig,
 *     )
 * }
 * ```
 */
internal class ViewModelGenerator(private val codeGenerator: CodeGenerator) {

    fun generate(stateClass: KSClassDeclaration) {
        val packageName = stateClass.packageName.asString()
        val stateName = stateClass.simpleName.asString()
        val stateClassName = ClassName(packageName, stateName)
        val defaultStateClass = resolveDefaultState(stateClass, packageName, stateName)
        val viewModelClassName = "${stateName}Store"

        FileSpec.builder(packageName, viewModelClassName)
            .addType(buildViewModelClass(viewModelClassName, stateClassName, defaultStateClass))
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

    private fun buildViewModelClass(
        className: String,
        stateClass: ClassName,
        defaultStateClass: ClassName,
    ): TypeSpec = TypeSpec.classBuilder(className)
        .addAnnotation(HiltClassNames.HiltViewModel)
        .primaryConstructor(buildConstructor(stateClass))
        .superclass(YamvClassNames.MviRetainedStore.parameterizedBy(stateClass, ClassName("kotlin", "Any")))
        .addProperty(buildDispatcherConfigProperty())
        .addProperty(buildStoreProperty(stateClass, defaultStateClass))
        .build()

    private fun buildConstructor(stateClass: ClassName): FunSpec =
        FunSpec.constructorBuilder()
            .addAnnotation(AnnotationSpec.builder(HiltClassNames.Inject).build())
            .addParameter(
                "features",
                ClassName("kotlin.collections", "Set").parameterizedBy(
                    YamvClassNames.Feature.parameterizedBy(stateClass).jvmSuppressWildcards()
                )
            )
            .addParameter(
                "defaultConfig",
                HiltClassNames.Optional.parameterizedBy(YamvClassNames.CoroutineDispatcherConfig),
            )
            .addParameter(
                com.squareup.kotlinpoet.ParameterSpec.builder(
                    "optionalDispatcherConfig",
                    HiltClassNames.Optional.parameterizedBy(YamvClassNames.CoroutineDispatcherConfig),
                )
                    .addAnnotation(
                        AnnotationSpec.builder(HiltClassNames.MviDispatcherConfig)
                            .addMember("%T::class", stateClass)
                            .build()
                    )
                    .build()
            )
            .build()

    private fun buildDispatcherConfigProperty(): PropertySpec =
        PropertySpec.builder("dispatcherConfig", YamvClassNames.CoroutineDispatcherConfig)
            .addModifiers(KModifier.OVERRIDE)
            .initializer(
                "optionalDispatcherConfig.orElseGet·{·defaultConfig.orElseGet·{·%T()·}·}",
                YamvClassNames.DefaultCoroutineDispatcherConfig,
            )
            .build()

    private fun buildStoreProperty(stateClass: ClassName, defaultStateClass: ClassName): PropertySpec =
        PropertySpec.builder(
            "store",
            YamvClassNames.MviStore.parameterizedBy(stateClass, ClassName("kotlin", "Any"))
        )
            .addModifiers(KModifier.OVERRIDE)
            .initializer(
                "%T(\n  features = features,\n  defaultState = %T(),\n  dispatcherConfig = dispatcherConfig,\n)",
                YamvClassNames.MviRuntime,
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
    simpleName.asString() == NO_DEFAULT_STATE_NAME

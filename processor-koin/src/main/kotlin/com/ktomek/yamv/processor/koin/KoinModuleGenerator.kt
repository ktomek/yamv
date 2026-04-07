package com.ktomek.yamv.processor.koin

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.ktomek.yamv.processor.koin.KoinFeatureFqns.FUNCTION_TYPED_FEATURE
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * Generates two Koin module files for each `@AutoState` class:
 *
 * 1. `{StateName}KoinModule.kt` — provides the Store factory
 * 2. `{StateName}FeaturesKoinModule.kt` — provides each `@AutoFeature` as a factory
 *
 * Generated output example for `CounterState`:
 * ```kotlin
 * // CounterStateKoinModule.kt
 * val counterStateKoinModule = module {
 *     factory { CounterStateStore(getAll<Feature<CounterState>>().toSet()) }
 * }
 *
 * // CounterStateFeaturesKoinModule.kt
 * val counterStateFeaturesKoinModule = module {
 *     factory<Feature<CounterState>> { AutoIncreaseFeature() }
 *     factory<Feature<CounterState>> { DecreaseFeature().wrap() }
 * }
 * ```
 */
internal class KoinModuleGenerator(private val codeGenerator: CodeGenerator) {

    fun generate(stateClass: KSClassDeclaration, features: List<KSDeclaration>) {
        val stateClassName = stateClass.toClassName()
        generateStoreModule(stateClassName)
        generateFeaturesModule(stateClassName, features)
    }

    private fun generateStoreModule(stateClass: ClassName) {
        val moduleName = "${stateClass.simpleName}KoinModule"
        val propertyName = moduleName.replaceFirstChar { it.lowercaseChar() }
        val storeClassName = ClassName(stateClass.packageName, "${stateClass.simpleName}Store")
        val featureType = YamvKoinClassNames.Feature.parameterizedBy(stateClass)

        val moduleBody = CodeBlock.builder()
            .beginControlFlow("%M", KoinClassNames.KoinModule)
            .addStatement(
                "factory { %T(getAll<%T>().toSet()) }",
                storeClassName,
                featureType
            )
            .endControlFlow()
            .build()

        FileSpec.builder(stateClass.packageName, moduleName)
            .addImport("org.koin.dsl", "module")
            .addProperty(
                PropertySpec.builder(propertyName, KoinClassNames.Module)
                    .initializer(moduleBody)
                    .build()
            )
            .build()
            .writeTo(codeGenerator, Dependencies(false))
    }

    private fun generateFeaturesModule(stateClass: ClassName, features: List<KSDeclaration>) {
        val moduleName = "${stateClass.simpleName}FeaturesKoinModule"
        val propertyName = moduleName.replaceFirstChar { it.lowercaseChar() }
        val featureType = YamvKoinClassNames.Feature.parameterizedBy(stateClass)

        val classFeatures = features.filterIsInstance<KSClassDeclaration>()
        val propertyFeatures = features.filterIsInstance<KSPropertyDeclaration>()

        val (directFeatures, wrapFeatures) = classFeatures.partition { !it.requiresWrap() }

        val moduleBodyBuilder = CodeBlock.builder()
            .beginControlFlow("%M", KoinClassNames.KoinModule)

        directFeatures.forEach { feature ->
            moduleBodyBuilder.addStatement(
                "factory<%T> { %T() }",
                featureType,
                feature.toClassName(),
            )
        }

        wrapFeatures.forEach { feature ->
            moduleBodyBuilder.addStatement(
                "factory<%T> { %T().wrap() }",
                featureType,
                feature.toClassName(),
            )
        }

        propertyFeatures.forEach { property ->
            moduleBodyBuilder.addStatement(
                "factory<%T> { %L.%L.wrap() }",
                featureType,
                property.packageName.asString(),
                property.simpleName.asString(),
            )
        }

        val moduleBody = moduleBodyBuilder.endControlFlow().build()

        val fileBuilder = FileSpec.builder(stateClass.packageName, moduleName)
            .addImport("org.koin.dsl", "module")

        if (wrapFeatures.isNotEmpty() || propertyFeatures.isNotEmpty()) {
            fileBuilder.addImport(
                YamvKoinClassNames.WrapImportPackage,
                YamvKoinClassNames.WrapImportName
            )
        }

        fileBuilder
            .addProperty(
                PropertySpec.builder(propertyName, KoinClassNames.Module)
                    .initializer(moduleBody)
                    .build()
            )
            .build()
            .writeTo(codeGenerator, Dependencies(false))
    }

    /** Returns true if this class feature needs `.wrap()` (i.e. it is a FunctionTypedFeature). */
    private fun KSClassDeclaration.requiresWrap(): Boolean = superTypes
        .any { it.resolve().declaration.qualifiedName?.asString() == FUNCTION_TYPED_FEATURE }
}

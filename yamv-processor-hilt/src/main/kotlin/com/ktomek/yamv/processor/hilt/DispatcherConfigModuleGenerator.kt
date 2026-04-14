package com.ktomek.yamv.processor.hilt

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * Generates a Dagger `@Module` that provides a [CoroutineDispatcherConfig] implementation
 * annotated with `@AutoDispatcherConfig`.
 *
 * - **No state args (global default):**
 * ```kotlin
 * @Module
 * @InstallIn(ViewModelComponent::class)
 * object IoDispatcherConfigModule {
 *     @Provides
 *     fun provideDefault(): CoroutineDispatcherConfig = IoDispatcherConfig()
 * }
 * ```
 *
 * - **Per-state / multi-state:**
 * ```kotlin
 * @Module
 * @InstallIn(ViewModelComponent::class)
 * object CounterDispatcherConfigModule {
 *     @Provides @MviDispatcherConfig(CounterState::class)
 *     fun provideForCounterState(): CoroutineDispatcherConfig = CounterDispatcherConfig()
 * }
 * ```
 */
internal class DispatcherConfigModuleGenerator(private val codeGenerator: CodeGenerator) {

    fun generate(classDecl: KSClassDeclaration) {
        val configClassName = classDecl.toClassName()
        val moduleName = "${configClassName.simpleName}Module"

        val annotation = classDecl.annotations
            .first { it.shortName.asString() == "AutoDispatcherConfig" }

        @Suppress("UNCHECKED_CAST")
        val stateTypes = annotation.arguments
            .firstOrNull { it.name?.asString() == "value" }
            ?.value as? List<KSType> ?: emptyList()

        val moduleSpec = if (stateTypes.isEmpty()) {
            buildGlobalModule(moduleName, configClassName)
        } else {
            buildPerStateModule(moduleName, configClassName, stateTypes)
        }

        FileSpec.builder(configClassName.packageName, moduleName)
            .addType(moduleSpec)
            .build()
            .writeTo(codeGenerator, Dependencies(false))
    }

    private fun buildGlobalModule(
        moduleName: String,
        configClassName: com.squareup.kotlinpoet.ClassName,
    ): TypeSpec = TypeSpec.objectBuilder(moduleName)
        .addAnnotation(HiltClassNames.Module)
        .addAnnotation(buildInstallInAnnotation())
        .addFunction(
            FunSpec.builder("provideDefault")
                .addAnnotation(HiltClassNames.Provides)
                .returns(YamvClassNames.CoroutineDispatcherConfig)
                .addStatement("return %T()", configClassName)
                .build()
        )
        .build()

    private fun buildPerStateModule(
        moduleName: String,
        configClassName: com.squareup.kotlinpoet.ClassName,
        stateTypes: List<KSType>,
    ): TypeSpec {
        val builder = TypeSpec.objectBuilder(moduleName)
            .addAnnotation(HiltClassNames.Module)
            .addAnnotation(buildInstallInAnnotation())

        stateTypes.forEach { stateType ->
            val stateClassDecl = stateType.declaration as KSClassDeclaration
            val stateClassName = stateClassDecl.toClassName()
            val stateName = stateClassName.simpleName

            builder.addFunction(
                FunSpec.builder("provideFor$stateName")
                    .addAnnotation(HiltClassNames.Provides)
                    .addAnnotation(
                        AnnotationSpec.builder(HiltClassNames.MviDispatcherConfig)
                            .addMember("%T::class", stateClassName)
                            .build()
                    )
                    .returns(YamvClassNames.CoroutineDispatcherConfig)
                    .addStatement("return %T()", configClassName)
                    .build()
            )
        }

        return builder.build()
    }

    private fun buildInstallInAnnotation(): AnnotationSpec =
        AnnotationSpec.builder(HiltClassNames.InstallIn)
            .addMember("%T::class", HiltClassNames.ViewModelComponent)
            .build()
}

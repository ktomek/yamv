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
import com.ktomek.yamv.annotations.AutoState
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.jvm.jvmSuppressWildcards
import com.squareup.kotlinpoet.ksp.writeTo
import processFeatures

class YamvProcessor(
    internal val codeGenerator: CodeGenerator,
    internal val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val autoStateClasses = resolver
            .getSymbolsWithAnnotation(AutoState::class.qualifiedName.toString())
            .filterIsInstance<KSClassDeclaration>()

        processFeatures(resolver, autoStateClasses)
        if (autoStateClasses.toList().isNotEmpty()) {
            autoStateClasses
                .filter { it.validate() }
                .forEach { autoStateClass ->
                    val packageName = autoStateClass.packageName.asString()
                    val stateName = autoStateClass.simpleName.asString()

                    val annotation = autoStateClass
                        .annotations
                        .first { it.shortName.asString() == AutoState::class.simpleName }
                    val defaultState = annotation
                        .arguments
                        .firstOrNull { it.name?.asString() == "defaultState" }
                        ?.value as? KSType

                    generateStoreClass(stateName, packageName, defaultState)
                }
        }
        return emptyList()
    }

    private fun generateStoreClass(
        stateName: String,
        packageName: String,
        defaultState: KSType?
    ) {

        val defaultState =
            if (defaultState != null && !isNoDefaultState(defaultState.declaration)) {
                ClassName(
                    packageName,
                    "$stateName.${defaultState.declaration.simpleName.asString()}"
                )
            } else {
                ClassName(packageName, stateName)
            }

        val className = "${stateName}ContainerHost"
        val fileBuilder = FileSpec.builder(packageName, className)
        val classBuilder = TypeSpec.classBuilder(className)
            .addAnnotation(ClassName("dagger.hilt.android.lifecycle", "HiltViewModel"))
            .primaryConstructor(
                FunSpec
                    .constructorBuilder()
                    .addAnnotation(
                        AnnotationSpec
                            .builder(ClassName("javax.inject", "Inject"))
                            .build()
                    )
                    .addParameter(
                        "features",
                        ClassName("kotlin.collections", "Set")
                            .parameterizedBy(
                                ClassName("com.ktomek.yamv.feature", "Feature")
                                    .parameterizedBy(ClassName(packageName, stateName))
                                    .jvmSuppressWildcards(true)
                            )
                    )
                    .build()
            )
            .superclass(
                ClassName("com.ktomek.yamv.state", "StateContainerHost")
                    .parameterizedBy(ClassName(packageName, stateName))
            )
            .addSuperclassConstructorParameter(
                """features = features, 
                   defaultState = %T(),""",
                defaultState
            )

        fileBuilder.addType(classBuilder.build()).build()
            .writeTo(codeGenerator, Dependencies(false))
    }

    private fun isNoDefaultState(declaration: KSDeclaration): Boolean =
        declaration.simpleName.asString() == "NoDefaultState"

}

class YamvProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        YamvProcessor(environment.codeGenerator, environment.logger)
}

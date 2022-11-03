package com.ktomek.yamv.processor

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate
import com.ktomek.yamv.annotations.AutoBaseOutcome
import com.ktomek.yamv.annotations.AutoReducer
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

internal fun YamvProcessor.processReducers(resolver: Resolver): List<KSDeclaration> {
    val baseResultsClasses = resolver
        .getSymbolsWithAnnotation(AutoBaseOutcome::class.qualifiedName.toString())
        .filterIsInstance<KSClassDeclaration>()
        .toList()

    val reducerClasses = resolver
        .getSymbolsWithAnnotation(AutoReducer::class.qualifiedName.toString())
        .filterIsInstance<KSPropertyDeclaration>()
        .toList()

    if (baseResultsClasses.isEmpty()) {
        return reducerClasses.toList()
    }

    baseResultsClasses.forEach {
        logger.warn("ktomek result: $it is valid ${it.validate()}")
    }

    reducerClasses.forEach {
        logger.warn("ktomek reducer: $it is valid ${it.validate()}")
    }

    val notValid = reducerClasses.filter { !it.validate() }
    if (notValid.isNotEmpty()) {
        return notValid
    }

    baseResultsClasses.forEach { resultClass ->
        val packageName = resultClass.packageName.asString()
        val stateName = resultClass.superTypes.first()
            .resolve()
            .arguments[0]
            .type
            ?.resolve()
            ?.declaration
            ?.simpleName
            ?.asString()!!

        val filteredReducers = reducerClasses
            .filter { reducerClass ->
                val reducerState = reducerClass.type.resolve()
                    .arguments[0]
                    .type?.resolve()
                    ?.declaration
                    ?.simpleName
                    ?.asString()
                logger.warn("ktomek reducerState: $reducerState == $stateName")
                reducerState == stateName
            }
        val resultName = resultClass.simpleName.asString()
        reducerClasses.forEach {
            logger.warn("ktomek reducer: $it is valid ${it.validate()}")
        }

        logger.warn("ktomek resultName: $resultName packageName: $packageName stateName: $stateName")
        filteredReducers.forEach {
            logger.warn("ktomek filtered reducer: $it is valid ${it.validate()}")
        }

        generateReducersModule(
            resultName,
            stateName,
            packageName,
            filteredReducers
        )
    }

    return emptyList()
}

private fun YamvProcessor.generateReducersModule(
    resultName: String,
    stateName: String,
    packageName: String,
    reducerClasses: List<KSPropertyDeclaration>
) {
    val moduleName = "${resultName}ReducersModule"
    val fileBuilder = FileSpec.builder(packageName, moduleName)

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
        .apply {
            if (reducerClasses.isEmpty()) {
                addFunction(
                    FunSpec.builder("provideDefaults")
                        .addAnnotation(ClassName("dagger", "Provides"))
                        .addAnnotation(ClassName("dagger.hilt.android.scopes", "ViewModelScoped"))
                        .returns(
                            Map::class.asClassName().parameterizedBy(
                                Class::class.asClassName().parameterizedBy(
                                    WildcardTypeName.producerOf(
                                        ClassName(
                                            packageName,
                                            resultName
                                        )
                                    )
                                ),
                                ClassName(
                                    "com.ktomek.yamv.core",
                                    "Reducer"
                                )
                                    .parameterizedBy(
                                        ClassName(packageName.replace(".outcome", ".state"), stateName),
                                        WildcardTypeName.producerOf(
                                            ClassName(
                                                packageName,
                                                resultName
                                            )
                                        ),
                                    )
                                    .copy(
                                        annotations = listOf(
                                            AnnotationSpec.builder(JvmSuppressWildcards::class)
                                                .build()
                                        )
                                    )
                            )
                        )
                        .addStatement("return emptyMap()")
                        .build()
                )
            }
        }

    reducerClasses.forEach { reducerClass ->
        val reducerPropertyName = reducerClass.simpleName.asString()

        val providesFunction = FunSpec.builder("provides$reducerPropertyName")
            .addAnnotation(ClassName("dagger", "Provides"))
            .addAnnotation(ClassName("dagger.multibindings", "IntoMap"))
            .addAnnotation(ClassName("dagger.hilt.android.scopes", "ViewModelScoped"))
            .addAnnotation(
                AnnotationSpec.builder(ClassName(packageName, "${resultName}Key"))
                    .addMember(
                        "%T::class",
                        reducerClass.type.resolve().arguments[1].type!!.resolve().toTypeName()
                    )
                    .build()
            )
            .returns(
                ClassName("com.ktomek.yamv.core", "Reducer")
                    .parameterizedBy(
                        ClassName(packageName.replace(".outcome", ".state"), stateName),
                        WildcardTypeName.producerOf(ClassName(packageName, resultName)),
                    )
            )
            .addStatement("return  ${reducerClass.packageName.asString()}.$reducerClass")
            .build()
        companion.addFunction(providesFunction)
    }

    moduleBuilder.addType(companion.build())

    fileBuilder
        .addType(moduleBuilder.build())
        .build()
        .writeTo(codeGenerator, Dependencies(false))
}

package com.ktomek.yamv.processor.hilt

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * Generates a `@Qualifier` annotation class per `@AutoState` class.
 *
 * Generated output example for `CounterState`:
 * ```kotlin
 * @Qualifier
 * @Retention(AnnotationRetention.BINARY)
 * annotation class CounterStateDispatcherConfig
 * ```
 *
 * The qualifier is used by the generated `*Store` constructor and `*FeaturesModule` so that
 * users can provide a per-state [com.ktomek.yamv.state.CoroutineDispatcherConfig] binding
 * without string-based `@Named` qualifiers.
 */
internal class DispatcherQualifierGenerator(private val codeGenerator: CodeGenerator) {

    /**
     * Generates the qualifier file and returns its [ClassName] for use in other generators.
     */
    fun generate(stateClass: KSClassDeclaration): ClassName {
        val stateClassName = stateClass.toClassName()
        val qualifierName = "${stateClassName.simpleName}DispatcherConfig"
        val qualifierClass = ClassName(stateClassName.packageName, qualifierName)

        FileSpec.builder(stateClassName.packageName, qualifierName)
            .addType(
                TypeSpec.annotationBuilder(qualifierName)
                    .addModifiers(KModifier.PUBLIC)
                    .addAnnotation(HiltClassNames.Qualifier)
                    .addAnnotation(
                        AnnotationSpec.builder(ClassName("kotlin.annotation", "Retention"))
                            .addMember(
                                "%T.%L",
                                ClassName("kotlin.annotation", "AnnotationRetention"),
                                "BINARY",
                            )
                            .build()
                    )
                    .build()
            )
            .build()
            .writeTo(codeGenerator, Dependencies(false))

        return qualifierClass
    }
}

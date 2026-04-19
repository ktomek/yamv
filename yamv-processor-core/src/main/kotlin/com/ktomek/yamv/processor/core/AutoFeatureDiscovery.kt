package com.ktomek.yamv.processor.core

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate
import com.ktomek.yamv.annotations.AutoFeature

object AutoFeatureDiscovery {

    private val annotationFqn = AutoFeature::class.qualifiedName!!

    fun findAnnotatedClasses(resolver: Resolver): Sequence<KSClassDeclaration> =
        resolver
            .getSymbolsWithAnnotation(annotationFqn)
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }

    fun findAnnotatedProperties(resolver: Resolver): Sequence<KSPropertyDeclaration> =
        resolver
            .getSymbolsWithAnnotation(annotationFqn)
            .filterIsInstance<KSPropertyDeclaration>()
            .filter { it.validate() }
}

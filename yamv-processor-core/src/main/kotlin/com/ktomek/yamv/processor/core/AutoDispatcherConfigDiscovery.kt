package com.ktomek.yamv.processor.core

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.ktomek.yamv.annotations.AutoDispatcherConfig

object AutoDispatcherConfigDiscovery {

    private val annotationFqn = AutoDispatcherConfig::class.qualifiedName!!

    fun findAnnotatedClasses(resolver: Resolver): Sequence<KSClassDeclaration> =
        resolver
            .getSymbolsWithAnnotation(annotationFqn)
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }
}

package com.ktomek.yamv.processor.core

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.ktomek.yamv.annotations.AutoState

object AutoStateDiscovery {

    private val annotationFqn = AutoState::class.qualifiedName!!

    fun findAnnotatedClasses(resolver: Resolver): Sequence<KSClassDeclaration> =
        resolver
            .getSymbolsWithAnnotation(annotationFqn)
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }
}

package com.ktomek.yamv.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.ktomek.yamv.processor.core.AutoStateDiscovery
import com.ktomek.yamv.processor.koin.FeatureFilter
import com.ktomek.yamv.processor.koin.KoinModuleGenerator
import com.ktomek.yamv.processor.koin.StoreGenerator

class YamvKoinProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val stateClasses = AutoStateDiscovery.findAnnotatedClasses(resolver).toList()
        if (stateClasses.isEmpty()) return emptyList()

        val featureFilter = FeatureFilter(resolver)
        val allFeatures = featureFilter.findAll()

        val storeGenerator = StoreGenerator(codeGenerator)
        val koinModuleGenerator = KoinModuleGenerator(codeGenerator)

        stateClasses.forEach { stateClass ->
            storeGenerator.generate(stateClass)
            val stateFeatures = featureFilter.forState(stateClass, allFeatures)
            koinModuleGenerator.generate(stateClass, stateFeatures)
        }

        return emptyList()
    }
}

class YamvKoinProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        YamvKoinProcessor(environment.codeGenerator, environment.logger)
}

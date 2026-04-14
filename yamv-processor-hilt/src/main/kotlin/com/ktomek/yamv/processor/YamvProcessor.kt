package com.ktomek.yamv.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.ktomek.yamv.processor.core.AutoDispatcherConfigDiscovery
import com.ktomek.yamv.processor.core.AutoStateDiscovery
import com.ktomek.yamv.processor.hilt.DispatcherConfigModuleGenerator
import com.ktomek.yamv.processor.hilt.FeatureFilter
import com.ktomek.yamv.processor.hilt.FeaturesModuleGenerator
import com.ktomek.yamv.processor.hilt.ViewModelGenerator

class YamvProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val stateClasses = AutoStateDiscovery.findAnnotatedClasses(resolver).toList()
        if (stateClasses.isEmpty()) return emptyList()

        val featureFilter = FeatureFilter(resolver)
        val allFeatures = featureFilter.findAll()

        val viewModelGenerator = ViewModelGenerator(codeGenerator)
        val featuresModuleGenerator = FeaturesModuleGenerator(codeGenerator)

        stateClasses.forEach { stateClass ->
            viewModelGenerator.generate(stateClass)
            val stateFeatures = featureFilter.forState(stateClass, allFeatures)
            featuresModuleGenerator.generate(stateClass, stateFeatures)
        }

        val dispatcherConfigs = AutoDispatcherConfigDiscovery.findAnnotatedClasses(resolver).toList()
        if (dispatcherConfigs.isNotEmpty()) {
            val dispatcherConfigGenerator = DispatcherConfigModuleGenerator(codeGenerator)
            dispatcherConfigs.forEach { configClass ->
                dispatcherConfigGenerator.generate(configClass)
            }
        }

        return emptyList()
    }
}

class YamvProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        YamvProcessor(environment.codeGenerator, environment.logger)
}

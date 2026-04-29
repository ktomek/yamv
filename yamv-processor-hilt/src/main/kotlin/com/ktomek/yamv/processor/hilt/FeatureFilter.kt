package com.ktomek.yamv.processor.hilt

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate
import com.ktomek.yamv.processor.core.AutoFeatureDiscovery
import com.squareup.kotlinpoet.ksp.toClassName

/**
 * Discovers and filters `@AutoFeature`-annotated symbols by their associated state type.
 */
internal class FeatureFilter(private val resolver: Resolver) {

    /**
     * Returns all valid `@AutoFeature` declarations that are recognized feature types —
     * any direct [Feature][FeatureFqns.FEATURE] subtype (for classes: [FeatureFqns.FLOW_FEATURE] or
     * [FeatureFqns.FLOW_UNIT_FEATURE]) or any of the typed feature wrappers
     * ([FeatureFqns.WRAP_REQUIRED]).
     */
    fun findAll(): List<KSDeclaration> {
        val classes = AutoFeatureDiscovery.findAnnotatedClasses(resolver).filter { it.isKnownFeatureClass() }
        val properties = AutoFeatureDiscovery.findAnnotatedProperties(resolver).filter { it.isKnownFeatureProperty() }
        return (classes + properties).toList()
    }

    /**
     * Filters [allFeatures] to only those that match the state type of [stateClass].
     */
    fun forState(stateClass: KSClassDeclaration, allFeatures: List<KSDeclaration>): List<KSDeclaration> {
        val stateName = stateClass.toClassName().simpleName
        return allFeatures.filter { it.validate() && it.matchesState(stateName) }
    }

    private fun KSClassDeclaration.isKnownFeatureClass(): Boolean =
        superTypes.any { it.resolve().declaration.qualifiedName?.asString() in KNOWN_CLASS_FEATURE_FQNS }

    private fun KSPropertyDeclaration.isKnownFeatureProperty(): Boolean =
        type.resolve().declaration.qualifiedName?.asString() in KNOWN_PROPERTY_FEATURE_FQNS

    private fun KSDeclaration.matchesState(stateName: String): Boolean = when (this) {
        is KSClassDeclaration -> superTypes.any { it.stateTypeNameMatches(stateName) }
        is KSPropertyDeclaration -> type.stateTypeNameMatches(stateName)
        else -> false
    }

    private fun com.google.devtools.ksp.symbol.KSTypeReference.stateTypeNameMatches(stateName: String): Boolean =
        resolve().arguments.firstOrNull()?.type?.resolve()?.declaration?.simpleName?.asString() == stateName

    companion object {
        private val KNOWN_CLASS_FEATURE_FQNS: Set<String> = buildSet {
            add(FeatureFqns.FLOW_FEATURE)
            add(FeatureFqns.FLOW_UNIT_FEATURE)
            addAll(FeatureFqns.WRAP_REQUIRED)
        }
        private val KNOWN_PROPERTY_FEATURE_FQNS: Set<String> = buildSet {
            add(FeatureFqns.FEATURE)
            addAll(FeatureFqns.WRAP_REQUIRED)
        }
    }
}

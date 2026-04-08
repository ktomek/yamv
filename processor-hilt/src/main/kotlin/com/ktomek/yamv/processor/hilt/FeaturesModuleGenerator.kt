package com.ktomek.yamv.processor.hilt

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * Generates a Hilt `@Module` that contributes all `@AutoFeature`-annotated features for a given state
 * into a `Set<Feature<S>>` multibinding.
 *
 * Generated output example:
 * ```kotlin
 * @Module
 * @InstallIn(ViewModelComponent::class)
 * interface CounterStateFeaturesModule {
 *     @Binds @IntoSet @ViewModelScoped
 *     fun bindsAutoIncreaseFeature(it: AutoIncreaseFeature): Feature<CounterState>
 *
 *     companion object {
 *         @Provides @ElementsIntoSet @ViewModelScoped
 *         fun provideDefaults(): Set<Feature<CounterState>> = emptySet()
 *
 *         @Provides @IntoSet @ViewModelScoped
 *         fun providesDecreaseFeature(it: DecreaseFeature): Feature<CounterState> = it.wrap()
 *     }
 * }
 * ```
 */
internal class FeaturesModuleGenerator(private val codeGenerator: CodeGenerator) {

    fun generate(stateClass: KSClassDeclaration, features: List<KSDeclaration>) {
        val stateClassName = stateClass.toClassName()
        val moduleName = "${stateClassName.simpleName}FeaturesModule"

        FileSpec.builder(stateClassName.packageName, moduleName)
            .addImport(YamvClassNames.WrapImportPackage, YamvClassNames.WrapImportName)
            .addType(buildModuleInterface(moduleName, stateClassName, features))
            .build()
            .writeTo(codeGenerator, Dependencies(false))
    }

    private fun buildModuleInterface(
        moduleName: String,
        stateClass: ClassName,
        features: List<KSDeclaration>,
    ): TypeSpec {
        val featureSetType = Set::class.asClassName().parameterizedBy(
            YamvClassNames.Feature.parameterizedBy(stateClass)
        )

        val classFeatures = features.filterIsInstance<KSClassDeclaration>()
        val propertyFeatures = features.filterIsInstance<KSPropertyDeclaration>()

        // Abstract @Binds methods go in the interface; @Provides (needing .wrap()) go in companion object
        val (bindsFeatures, providesFeatures) = classFeatures.partition { !it.requiresWrap() }

        return TypeSpec.interfaceBuilder(moduleName)
            .addAnnotation(HiltClassNames.Module)
            .addAnnotation(buildInstallInAnnotation())
            .apply { bindsFeatures.forEach { addFunction(buildAbstractBindsForClass(it, stateClass)) } }
            .addType(buildCompanionObject(stateClass, featureSetType, providesFeatures, propertyFeatures))
            .build()
    }

    /** Returns true if this class feature needs `.wrap()` (i.e. it is a FunctionTypedFeature). */
    private fun KSClassDeclaration.requiresWrap(): Boolean =
        superTypes.any { it.resolve().declaration.qualifiedName?.asString() == FeatureFqns.FUNCTION_TYPED_FEATURE }

    private fun buildInstallInAnnotation(): AnnotationSpec =
        AnnotationSpec.builder(HiltClassNames.InstallIn)
            .addMember("%T::class", HiltClassNames.ViewModelComponent)
            .build()

    private fun buildAbstractBindsForClass(feature: KSClassDeclaration, stateClass: ClassName): FunSpec =
        buildAbstractBinds(
            name = "binds${feature.toClassName().simpleName}",
            paramType = feature.toClassName(),
            returnType = YamvClassNames.Feature.parameterizedBy(stateClass),
        )

    private fun buildCompanionObject(
        stateClass: ClassName,
        featureSetType: com.squareup.kotlinpoet.TypeName,
        classFeaturesToWrap: List<KSClassDeclaration>,
        propertyFeatures: List<KSPropertyDeclaration>,
    ): TypeSpec = TypeSpec.companionObjectBuilder()
        .addFunction(buildEmptySetProvider(featureSetType))
        .apply { classFeaturesToWrap.forEach { addFunction(buildClassProvidesWithWrap(it, stateClass)) } }
        .apply { propertyFeatures.forEach { addFunction(buildPropertyBinding(it, stateClass)) } }
        .build()

    private fun buildClassProvidesWithWrap(feature: KSClassDeclaration, stateClass: ClassName): FunSpec {
        val featureClassName = feature.toClassName()
        return buildProvidesWithWrap(
            name = "provides${featureClassName.simpleName}",
            paramType = featureClassName,
            returnType = YamvClassNames.Feature.parameterizedBy(stateClass),
            statement = "return it.wrap()",
        )
    }

    private fun buildEmptySetProvider(featureSetType: com.squareup.kotlinpoet.TypeName): FunSpec =
        FunSpec.builder("provideDefaults")
            .addAnnotation(HiltClassNames.Provides)
            .addAnnotation(HiltClassNames.ElementsIntoSet)
            .addAnnotation(HiltClassNames.ViewModelScoped)
            .returns(featureSetType)
            .addStatement("return emptySet()")
            .build()

    private fun buildPropertyBinding(property: KSPropertyDeclaration, stateClass: ClassName): FunSpec {
        val fqn = property.type.resolve().declaration.qualifiedName?.asString()
        val alreadyWrapped = fqn == FeatureFqns.FEATURE
        val ref = "${property.packageName.asString()}.${property.simpleName.asString()}"
        val statement = if (alreadyWrapped) "return $ref" else "return $ref.wrap()"
        return buildProvidesWithWrap(
            name = "provides${property.simpleName.asString().replaceFirstChar { it.uppercaseChar() }}",
            paramType = null,
            returnType = YamvClassNames.Feature.parameterizedBy(stateClass),
            statement = statement,
        )
    }

    private fun buildAbstractBinds(name: String, paramType: ClassName, returnType: com.squareup.kotlinpoet.TypeName): FunSpec =
        FunSpec.builder(name)
            .addAnnotation(HiltClassNames.Binds)
            .addAnnotation(HiltClassNames.IntoSet)
            .addAnnotation(HiltClassNames.ViewModelScoped)
            .addParameter("it", paramType)
            .returns(returnType)
            .addModifiers(KModifier.ABSTRACT)
            .build()

    private fun buildProvidesWithWrap(
        name: String,
        paramType: ClassName?,
        returnType: com.squareup.kotlinpoet.TypeName,
        statement: String,
    ): FunSpec = FunSpec.builder(name)
        .addAnnotation(HiltClassNames.Provides)
        .addAnnotation(HiltClassNames.IntoSet)
        .addAnnotation(HiltClassNames.ViewModelScoped)
        .apply { if (paramType != null) addParameter("it", paramType) }
        .returns(returnType)
        .addStatement(statement)
        .build()
}

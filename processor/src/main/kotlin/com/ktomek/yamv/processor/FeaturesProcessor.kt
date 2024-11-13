import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate
import com.ktomek.yamv.annotations.AutoFeature
import com.ktomek.yamv.processor.YamvProcessor
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

internal fun YamvProcessor.processFeatures(
    resolver: Resolver,
    autoStateClasses: Sequence<KSClassDeclaration>
): List<KSDeclaration> {
    val featureClasses = getFeatureClasses(resolver)

    if (featureClasses.any { !it.validate() }) {
        return featureClasses
    }

    if (featureClasses.isEmpty()) {
        return emptyList()
    }

    autoStateClasses.forEach { stateClass ->
        val stateType = stateClass.toClassName()

        val relevantFeatureClasses = featureClasses
            .filter { feature ->
                val isOk = when (feature) {
                    is KSClassDeclaration -> {
                        feature.superTypes.any { superType ->
                            superType.resolve()
                                .arguments
                                .firstOrNull()
                                ?.type
                                ?.resolve()
                                ?.declaration
                                ?.simpleName
                                ?.asString() == stateType.simpleName
                        }
                    }

                    is KSPropertyDeclaration -> {
                        feature.type.resolve()
                            .arguments
                            .firstOrNull()
                            ?.type
                            ?.resolve()
                            ?.declaration
                            ?.simpleName
                            ?.asString() == stateType.simpleName
                    }

                    else -> false
                }
                feature.validate() && isOk
            }
        generateFeaturesModule(stateType.simpleName, stateType.packageName, relevantFeatureClasses)
    }
    return featureClasses.filter { !it.validate() }
}

fun YamvProcessor.getFeatureClasses(resolver: Resolver) =
    resolver.getSymbolsWithAnnotation(AutoFeature::class.qualifiedName.toString())
        .filterIsInstance<KSDeclaration>()
        .filter { feature ->
            when (feature) {
                is KSClassDeclaration -> {
                    feature.superTypes.any { superType ->
                        val superTypeDecl = superType.resolve().declaration
                        val qualifiedName = superTypeDecl.qualifiedName?.asString()
                        val isFeatureOrFeatureFlow = qualifiedName in listOf(
                            "com.ktomek.yamv.feature.Feature",
                            "com.ktomek.yamv.feature.FeatureFlow",
                            "com.ktomek.yamv.feature.TypedFeature"
                        )
                        isFeatureOrFeatureFlow
                    }
                }

                is KSPropertyDeclaration -> {
                    val superTypeDecl = feature.type.resolve().declaration
                    val qualifiedName = superTypeDecl.qualifiedName?.asString()
                    val isFeatureOrFeatureFlow = qualifiedName in listOf(
                        "com.ktomek.yamv.feature.TypedFeature",
                    )
                    isFeatureOrFeatureFlow
                }

                else -> false
            }
        }
        .toList()

private fun YamvProcessor.generateFeaturesModule(
    stateName: String,
    packageName: String,
    featureClasses: List<KSDeclaration>
) {
    val moduleName = "${stateName}FeaturesModule"
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
        .addFunction(
            FunSpec.builder("provideDefaults")
                .addAnnotation(ClassName("dagger", "Provides"))
                .addAnnotation(ClassName("dagger.multibindings", "ElementsIntoSet"))
                .addAnnotation(ClassName("dagger.hilt.android.scopes", "ViewModelScoped"))
                .returns(
                    Set::class.asClassName().parameterizedBy(
                        ClassName(
                            "com.ktomek.yamv.feature",
                            "FeatureFlow"
                        ).parameterizedBy(ClassName(packageName, stateName))
                    )
                )
                .addStatement("return emptySet()")
                .build()
        )

    featureClasses.forEach { featureClass ->
        when (featureClass) {
            is KSClassDeclaration -> {
                val featureClassName = featureClass.toClassName()
                val returnType = determineFeatureType(featureClass, packageName, stateName)

                val superTypeDeclaration = featureClass.superTypes.first().resolve().declaration
                val superTypeName = superTypeDeclaration.qualifiedName?.asString() ?: ""

                when (superTypeName) {
                    "com.ktomek.yamv.feature.TypedFeature" -> {
                        val bindFunction = FunSpec.builder("provides$featureClass")
                            .addAnnotation(ClassName("dagger", "Provides"))
                            .addAnnotation(ClassName("dagger.multibindings", "IntoSet"))
                            .addAnnotation(
                                ClassName(
                                    "dagger.hilt.android.scopes",
                                    "ViewModelScoped"
                                )
                            )
                            .addParameter("it", featureClassName)
                            .returns(returnType)
                            .addStatement("return  it.wrap()")
//                    .addModifiers(KModifier.ABSTRACT)
                            .build()

                        companion.addFunction(bindFunction)
                    }

                    else -> {
                        val bindFunction = FunSpec.builder("binds${featureClassName.simpleName}")
                            .addAnnotation(ClassName("dagger", "Binds"))
                            .addAnnotation(ClassName("dagger.multibindings", "IntoSet"))
                            .addAnnotation(
                                ClassName(
                                    "dagger.hilt.android.scopes",
                                    "ViewModelScoped"
                                )
                            )
                            .returns(returnType)
                            .addParameter("it", featureClassName)
                            .addModifiers(KModifier.ABSTRACT)
                            .build()

                        moduleBuilder.addFunction(bindFunction)
                    }
                }
            }

            is KSPropertyDeclaration -> {
                val returnType = determinePropertyFeatureType(featureClass, packageName, stateName)

                val bindFunction = FunSpec.builder("provides$featureClass")
                    .addAnnotation(ClassName("dagger", "Provides"))
                    .addAnnotation(ClassName("dagger.multibindings", "IntoSet"))
                    .addAnnotation(ClassName("dagger.hilt.android.scopes", "ViewModelScoped"))
                    .returns(returnType)
                    .addStatement("return  ${featureClass.packageName.asString()}.$featureClass.wrap()")
//                    .addModifiers(KModifier.ABSTRACT)
                    .build()

                companion.addFunction(bindFunction)
            }
        }
    }

    moduleBuilder.addType(companion.build())
    fileBuilder
        .addImport("com.ktomek.yamv.feature", "wrap")
        .addType(moduleBuilder.build())
        .build()
        .writeTo(codeGenerator, Dependencies(false)) // , *files.toTypedArray()))
}

private fun determineFeatureType(
    featureClass: KSClassDeclaration,
    packageName: String,
    stateName: String
): TypeName {
    // Iterate over the superinterfaces to determine whether it's Feature or FeatureFlow
    featureClass.superTypes.forEach { superType ->
        val superTypeDeclaration = superType.resolve().declaration
        val superTypeName = superTypeDeclaration.qualifiedName?.asString() ?: ""

        // Check if the superinterface is Feature or FeatureFlow
        when (superTypeName) {
            "com.ktomek.yamv.feature.Feature" ->
                return ClassName("com.ktomek.yamv.feature", "Feature")
                    .parameterizedBy(ClassName(packageName, stateName))

            "com.ktomek.yamv.feature.FeatureFlow" ->
                return ClassName("com.ktomek.yamv.feature", "FeatureFlow")
                    .parameterizedBy(ClassName(packageName, stateName))

            "com.ktomek.yamv.feature.TypedFeature" -> {
                return ClassName("com.ktomek.yamv.feature", "FeatureFlow")
                    .parameterizedBy(ClassName(packageName, stateName))
            }
        }
    }

    // Default to FeatureFlow if no matching superinterface is found (or adjust as necessary)
    return ClassName("com.ktomek.yamv.feature", "FeatureFlow")
        .parameterizedBy(ClassName(packageName, stateName))
}

private fun determinePropertyFeatureType(
    featureProperty: KSPropertyDeclaration,
    packageName: String,
    stateName: String
): TypeName {
    // Iterate over the superinterfaces to determine whether it's Feature or FeatureFlow
    val superType = featureProperty.type
    val superTypeDeclaration = superType.resolve().declaration
    val superTypeName = superTypeDeclaration.qualifiedName?.asString() ?: ""

    // Check if the superinterface is Feature or FeatureFlow
    when (superTypeName) {
        "com.ktomek.yamv.feature.Feature" ->
            return ClassName("com.ktomek.yamv.feature", "Feature")
                .parameterizedBy(ClassName(packageName, stateName))

        "com.ktomek.yamv.feature.FeatureFlow" ->
            return ClassName("com.ktomek.yamv.feature", "FeatureFlow")
                .parameterizedBy(ClassName(packageName, stateName))

        "com.ktomek.yamv.feature.TypedFeature" -> {
            return ClassName("com.ktomek.yamv.feature", "FeatureFlow")
                .parameterizedBy(ClassName(packageName, stateName))
        }
    }

    // Default to FeatureFlow if no matching superinterface is found (or adjust as necessary)
    return ClassName("com.ktomek.yamv.feature", "FeatureFlow")
        .parameterizedBy(ClassName(packageName, stateName))
}

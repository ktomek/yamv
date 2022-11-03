
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate
import com.ktomek.yamv.annotations.AutoBaseOutcome
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

internal fun YamvProcessor.processFeatures(resolver: Resolver): List<KSDeclaration> {
    val featureClasses = getFeatureClasses(resolver)
    val resultsClasses = resolver
        .getSymbolsWithAnnotation(AutoBaseOutcome::class.qualifiedName.toString())
        .filterIsInstance<KSClassDeclaration>()

    if (featureClasses.any { !it.validate() }) {
        return featureClasses
    }

    if (featureClasses.isEmpty()) {
        return emptyList()
    }

    resultsClasses.forEach { resultClass ->
        if (featureClasses.isEmpty()) return@forEach
        val packageName = resultClass.packageName.asString()
        val resultName = resultClass.simpleName.asString()
        val relevantFeatureClasses = featureClasses
            .filter { featureClass ->
                val isOk = when (featureClass) {
                    is KSClassDeclaration -> {
                        featureClass.superTypes.any { superType ->
                            val typeArgument =
                                superType.resolve()
                                    .arguments
                                    .firstOrNull()
                                    ?.type
                                    ?.resolve()
                                    ?.declaration
                            (typeArgument as? KSClassDeclaration)?.simpleName?.asString() == resultName
                        }
                    }

                    is KSPropertyDeclaration -> {
                        val typeArgument =
                            featureClass.type.resolve()
                                .arguments
                                .firstOrNull()
                                ?.type
                                ?.resolve()
                                ?.declaration
                        (typeArgument as? KSClassDeclaration)?.simpleName?.asString() == resultName
                    }

                    else -> false
                }
                featureClass.validate() && isOk
            }
        generateFeaturesModule(
            resultName,
            packageName,
            relevantFeatureClasses
        )
    }
    return featureClasses.filter { !it.validate() }
}

fun YamvProcessor.getFeatureClasses(resolver: Resolver) =
    resolver.getSymbolsWithAnnotation(AutoFeature::class.qualifiedName.toString())
        .filterIsInstance<KSDeclaration>()
        .filter { featureClass ->
            when (featureClass) {
                is KSClassDeclaration -> {
                    featureClass.superTypes.any { superType ->
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
                    val superTypeDecl = featureClass.type.resolve().declaration
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
    resultName: String,
    packageName: String,
    featureClasses: List<KSDeclaration>
) {
    val moduleName = "${resultName}FeaturesModule"
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
                        ).parameterizedBy(ClassName(packageName, resultName))
                    )
                )
                .addStatement("return emptySet()")
                .build()
        )

    featureClasses.forEach { featureClass ->
        logger.warn("Generating module bind/provider for feature class: $featureClass")
        when (featureClass) {
            is KSClassDeclaration -> {
                val featureClassName = featureClass.toClassName()
                val returnType = determineFeatureType(featureClass, packageName, resultName)

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
                val returnType = determinePropertyFeatureType(featureClass, packageName, resultName)

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
    resultName: String
): TypeName {
    // Iterate over the superinterfaces to determine whether it's Feature or FeatureFlow
    featureClass.superTypes.forEach { superType ->
        val superTypeDeclaration = superType.resolve().declaration
        val superTypeName = superTypeDeclaration.qualifiedName?.asString() ?: ""

        // Check if the superinterface is Feature or FeatureFlow
        when (superTypeName) {
            "com.ktomek.yamv.feature.Feature" ->
                return ClassName("com.ktomek.yamv.feature", "Feature")
                    .parameterizedBy(ClassName(packageName, resultName))

            "com.ktomek.yamv.feature.FeatureFlow" ->
                return ClassName("com.ktomek.yamv.feature", "FeatureFlow")
                    .parameterizedBy(ClassName(packageName, resultName))

            "com.ktomek.yamv.feature.TypedFeature" -> {
                return ClassName("com.ktomek.yamv.feature", "FeatureFlow")
                    .parameterizedBy(ClassName(packageName, resultName))
            }
        }
    }

    // Default to FeatureFlow if no matching superinterface is found (or adjust as necessary)
    return ClassName("com.ktomek.yamv.feature", "FeatureFlow")
        .parameterizedBy(ClassName(packageName, resultName))
}

private fun determinePropertyFeatureType(
    featureProperty: KSPropertyDeclaration,
    packageName: String,
    resultName: String
): TypeName {
    // Iterate over the superinterfaces to determine whether it's Feature or FeatureFlow
    val superType = featureProperty.type
    val superTypeDeclaration = superType.resolve().declaration
    val superTypeName = superTypeDeclaration.qualifiedName?.asString() ?: ""

    // Check if the superinterface is Feature or FeatureFlow
    when (superTypeName) {
        "com.ktomek.yamv.feature.Feature" ->
            return ClassName("com.ktomek.yamv.feature", "Feature")
                .parameterizedBy(ClassName(packageName, resultName))

        "com.ktomek.yamv.feature.FeatureFlow" ->
            return ClassName("com.ktomek.yamv.feature", "FeatureFlow")
                .parameterizedBy(ClassName(packageName, resultName))

        "com.ktomek.yamv.feature.TypedFeature" -> {
            return ClassName("com.ktomek.yamv.feature", "FeatureFlow")
                .parameterizedBy(ClassName(packageName, resultName))
        }
    }

    // Default to FeatureFlow if no matching superinterface is found (or adjust as necessary)
    return ClassName("com.ktomek.yamv.feature", "FeatureFlow")
        .parameterizedBy(ClassName(packageName, resultName))
}

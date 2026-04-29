package com.ktomek.yamv.processor.hilt

import com.squareup.kotlinpoet.ClassName

internal object HiltClassNames {
    val HiltViewModel = ClassName("dagger.hilt.android.lifecycle", "HiltViewModel")
    val Inject = ClassName("javax.inject", "Inject")
    val Module = ClassName("dagger", "Module")
    val InstallIn = ClassName("dagger.hilt", "InstallIn")
    val ViewModelComponent = ClassName("dagger.hilt.android.components", "ViewModelComponent")
    val ViewModelScoped = ClassName("dagger.hilt.android.scopes", "ViewModelScoped")
    val Provides = ClassName("dagger", "Provides")
    val Binds = ClassName("dagger", "Binds")
    val IntoSet = ClassName("dagger.multibindings", "IntoSet")
    val ElementsIntoSet = ClassName("dagger.multibindings", "ElementsIntoSet")
    val BindsOptionalOf = ClassName("dagger", "BindsOptionalOf")
    val Optional = ClassName("java.util", "Optional")
    val MviDispatcherConfig = ClassName("com.ktomek.yamv.hilt", "MviDispatcherConfig")
}

internal object YamvClassNames {
    val MviRetainedStore = ClassName("com.ktomek.yamv.retainer", "MviRetainedStore")
    val MviRuntime = ClassName("com.ktomek.yamv.state", "MviRuntime")
    val MviStore = ClassName("com.ktomek.yamv.state", "MviStore")
    val Feature = ClassName("com.ktomek.yamv.feature", "Feature")
    val CoroutineDispatcherConfig = ClassName("com.ktomek.yamv.state", "CoroutineDispatcherConfig")
    val DefaultCoroutineDispatcherConfig = ClassName("com.ktomek.yamv.state", "DefaultCoroutineDispatcherConfig")
    const val WrapImportPackage = "com.ktomek.yamv.feature"
    const val WrapImportName = "wrap"
}

internal object FeatureFqns {
    const val FEATURE = "com.ktomek.yamv.feature.Feature"
    const val FLOW_FEATURE = "com.ktomek.yamv.feature.Feature.FlowFeature"
    const val FLOW_UNIT_FEATURE = "com.ktomek.yamv.feature.Feature.FlowUnitFeature"
    const val FUNCTION_TYPED_FEATURE = "com.ktomek.yamv.feature.FunctionTypedFeature"
    const val TYPED_FEATURE = "com.ktomek.yamv.feature.TypedFeature"
    const val ACTION_TYPED_FEATURE = "com.ktomek.yamv.feature.ActionTypedFeature"
    const val TYPED_UNIT_FEATURE = "com.ktomek.yamv.feature.TypedUnitFeature"

    val WRAP_REQUIRED: Set<String> = setOf(
        FUNCTION_TYPED_FEATURE,
        TYPED_FEATURE,
        ACTION_TYPED_FEATURE,
        TYPED_UNIT_FEATURE,
    )
}

internal const val NO_DEFAULT_STATE_NAME = "NoDefaultState"

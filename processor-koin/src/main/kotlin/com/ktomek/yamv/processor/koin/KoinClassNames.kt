package com.ktomek.yamv.processor.koin

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

internal object KoinClassNames {
    val Module = ClassName("org.koin.core.module", "Module")
    val KoinModule = MemberName("org.koin.dsl", "module")
}

internal object YamvKoinClassNames {
    val MviRuntime = ClassName("com.ktomek.yamv.state", "MviRuntime")
    val MviStore = ClassName("com.ktomek.yamv.state", "MviStore")
    val Feature = ClassName("com.ktomek.yamv.feature", "Feature")
    val WrapImportPackage = "com.ktomek.yamv.feature"
    val WrapImportName = "wrap"
}

internal object KoinFeatureFqns {
    const val FLOW_FEATURE = "com.ktomek.yamv.feature.Feature.FlowFeature"
    const val FUNCTION_TYPED_FEATURE = "com.ktomek.yamv.feature.FunctionTypedFeature"
}

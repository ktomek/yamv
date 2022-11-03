package com.ktomek.yamv.annotations

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class AutoReducer

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class AutoOutcome

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class AutoBaseOutcome

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class AutoInjectState

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class AutoFeature

// @Target(AnnotationTarget.CLASS)
// @Retention(AnnotationRetention.SOURCE)
// annotation class AutoState

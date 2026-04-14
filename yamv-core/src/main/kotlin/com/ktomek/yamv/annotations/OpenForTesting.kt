package com.ktomek.yamv.annotations

/**
 * Marks a class as intentionally open for test subclassing.
 *
 * The [allopen] compiler plugin is configured to honor this annotation only in test compilations,
 * so the class remains final in production bytecode.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class OpenForTesting

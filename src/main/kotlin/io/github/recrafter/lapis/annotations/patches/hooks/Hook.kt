package io.github.recrafter.lapis.annotations.patches.hooks

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Hook(val kind: Kind)

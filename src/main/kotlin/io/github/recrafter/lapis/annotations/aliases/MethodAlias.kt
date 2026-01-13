package io.github.recrafter.lapis.annotations.aliases

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class MethodAlias(val vanillaName: String)

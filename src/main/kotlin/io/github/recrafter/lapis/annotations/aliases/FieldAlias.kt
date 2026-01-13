package io.github.recrafter.lapis.annotations.aliases

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class FieldAlias(val vanillaName: String)

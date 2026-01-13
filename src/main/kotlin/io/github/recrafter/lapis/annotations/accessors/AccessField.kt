package io.github.recrafter.lapis.annotations.accessors

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class AccessField(
    val vanillaName: String = "",
    val isStatic: Boolean = false,
)

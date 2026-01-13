package io.github.recrafter.lapis.annotations.accessors

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class AccessMethod(
    val vanillaName: String = "",
    val isStatic: Boolean = false,
)

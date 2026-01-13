package io.github.recrafter.lapis.annotations.patches.targets

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class MethodTarget(val isStatic: Boolean = false)

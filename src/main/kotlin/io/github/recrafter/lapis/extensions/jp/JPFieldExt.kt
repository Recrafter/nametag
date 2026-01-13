package io.github.recrafter.lapis.extensions.jp

inline fun <reified A : Annotation> JPFieldBuilder.addAnnotation(builder: JPAnnotationBuilder.() -> Unit = {}) {
    addAnnotation(buildJavaAnnotation<A>(builder))
}

package io.github.recrafter.lapis.extensions.jp

inline fun <reified A : Annotation> JPTypeBuilder.addAnnotation(builder: JPAnnotationBuilder.() -> Unit = {}) {
    addAnnotation(buildJavaAnnotation<A>(builder))
}

fun JPType.toJavaFile(packageName: String, builder: JPFileBuilder.() -> Unit = {}): JPFile =
    JPFile.builder(packageName, this).apply(builder).indent("    ").skipJavaLangImports(true).build()

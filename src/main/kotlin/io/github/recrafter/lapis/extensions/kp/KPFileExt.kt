package io.github.recrafter.lapis.extensions.kp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies

inline fun <reified A : Annotation> KPFileBuilder.addAnnotation(builder: KPAnnotationBuilder.() -> Unit = {}) {
    addAnnotation(buildKotlinAnnotation<A>(builder))
}

fun KPFile.writeTo(codeGenerator: CodeGenerator, dependencies: Dependencies) {
    codeGenerator.createNewFile(dependencies, packageName, name).writer().use { writeTo(it) }
}

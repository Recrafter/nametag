package io.github.recrafter.lapis.utils

import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSNode
import io.github.recrafter.lapis.extensions.psi.PsiFile
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.io.File

object PsiHelper {

    private val factory: KtPsiFactory by lazy {
        val environment = KotlinCoreEnvironment.createForTests(
            disposable,
            CompilerConfiguration(),
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        KtPsiFactory(environment.project)
    }

    private val disposable: Disposable = Disposer.newDisposable()
    private val cache: MutableMap<String, PsiFile> = mutableMapOf()

    fun loadPsiFile(node: KSNode): PsiFile {
        val location = node.location as? FileLocation
            ?: error("KSNode does not have a file location: $node")
        val file = File(location.filePath)
        return cache.getOrPut(file.canonicalPath) {
            factory.createFile(file.name, file.readText())
        }
    }

    fun destroy() {
        Disposer.dispose(disposable)
        cache.clear()
    }
}

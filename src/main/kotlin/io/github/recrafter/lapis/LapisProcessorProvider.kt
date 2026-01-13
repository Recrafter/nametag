package io.github.recrafter.lapis

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

@AutoService(SymbolProcessorProvider::class)
internal class LapisProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val optionNamespacePrefix = "lapis."
        return LapisProcessor(
            environment.options
                .filterKeys { it.startsWith(optionNamespacePrefix) }
                .mapKeys { it.key.substringAfter(optionNamespacePrefix) },
            environment.codeGenerator,
            environment.logger
        )
    }
}

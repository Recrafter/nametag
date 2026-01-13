package io.github.recrafter.lapis.kj

import io.github.recrafter.lapis.extensions.common.unsafeLazy
import io.github.recrafter.lapis.extensions.jp.JPParameter
import io.github.recrafter.lapis.extensions.jp.buildJavaParameter
import io.github.recrafter.lapis.extensions.kp.KPParameter
import io.github.recrafter.lapis.extensions.kp.buildKotlinParameter

class KJParameterList(val parameters: List<KJParameter>) {

    val names: List<String> by unsafeLazy {
        parameters.map { it.name }
    }

    val kotlinVersion: List<KPParameter> by unsafeLazy {
        parameters.map { buildKotlinParameter(it.name, it.type) }
    }

    val javaVersion: List<JPParameter> by unsafeLazy {
        parameters.map {
            buildJavaParameter(it.type, it.name) {

            }
        }
    }
}

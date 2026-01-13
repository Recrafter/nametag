package io.github.recrafter.lapis.kj

import io.github.recrafter.lapis.extensions.common.unsafeLazy
import io.github.recrafter.lapis.extensions.jp.JPClassName
import io.github.recrafter.lapis.extensions.jp.JPCodeBlock
import io.github.recrafter.lapis.extensions.kp.KPClassName
import io.github.recrafter.lapis.extensions.kp.KPCodeBlock

class KJClassName(val packageName: String, vararg val names: String) {

    val simpleName: String = names.first()
    val name: String = names.joinToString(".")
    val qualifiedName: String = "$packageName.$name"

    val kotlinVersion: KPClassName by unsafeLazy {
        KPClassName(packageName, *names)
    }

    val javaVersion: JPClassName by unsafeLazy {
        JPClassName.get(packageName, simpleName, *names.drop(1).toTypedArray())
    }

    val kotlinCodeBlock: KPCodeBlock by unsafeLazy {
        KPCodeBlock.of("%T", kotlinVersion)
    }

    val javaCodeBlock: JPCodeBlock by unsafeLazy {
        JPCodeBlock.of("\$T", kotlinVersion)
    }

    val typeName: KJTypeName by unsafeLazy {
        KJTypeName(kotlinVersion)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is KJClassName) {
            return false
        }
        return kotlinVersion == other.kotlinVersion
    }

    override fun hashCode(): Int =
        kotlinVersion.hashCode()
}

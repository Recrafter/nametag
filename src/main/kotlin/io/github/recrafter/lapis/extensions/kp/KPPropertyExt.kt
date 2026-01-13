package io.github.recrafter.lapis.extensions.kp

import io.github.recrafter.lapis.kj.KJClassName
import io.github.recrafter.lapis.kj.KJTypeName

fun KPPropertyBuilder.setReceiverType(type: KJTypeName) {
    receiver(type.kotlinVersion)
}

fun KPPropertyBuilder.setReceiverType(type: KJClassName) {
    setReceiverType(type.typeName)
}

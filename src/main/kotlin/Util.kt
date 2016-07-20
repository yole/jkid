package ru.yole.jkid

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass

inline fun <reified T> KAnnotatedElement.findAnnotation(): T?
        = annotations.filterIsInstance<T>().firstOrNull()

internal fun <T : Any> KClass<T>.createInstance(): T {
    val noArgConstructor = constructors.find {
        it.parameters.isEmpty()
    }
    noArgConstructor ?: throw IllegalArgumentException(
            "Class must have a no-argument constructor")

    return noArgConstructor.call()
}

fun Type.asJavaClass(): Class<Any> = when (this) {
    is Class<*> -> this as Class<Any>
    is ParameterizedType -> rawType as? Class<Any>
            ?: throw UnsupportedOperationException("Unknown type $this")
    else -> throw UnsupportedOperationException("Unknown type $this")
}

fun <T> Iterable<T>.joinToStringBuilder(stringBuilder: StringBuilder, separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", callback: ((T) -> Unit)? = null): StringBuilder {
    return joinTo(stringBuilder, separator, prefix, postfix, limit, truncated) {
        if (callback == null) return@joinTo it.toString()
        callback(it)
        ""
    }
}

fun Type.isPrimitiveOrString(): Boolean {
    val cls = this as? Class<Any> ?: return false
    return cls.kotlin.javaPrimitiveType != null || cls == String::class.java
}
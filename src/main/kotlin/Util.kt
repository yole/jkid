package ru.yole.jkid

import ru.yole.jkid.deserialization.SchemaMismatchException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass

inline fun <reified T> KAnnotatedElement.findAnnotation(): T?
        = annotations.filterIsInstance<T>().firstOrNull()

internal fun <T : Any> KClass<T>.newInstance(): T {
    val noArgConstructor = constructors.singleOrNull {
        it.parameters.isEmpty()
    } ?: throw IllegalArgumentException(
            "Class must have a no-argument constructor")

    return noArgConstructor.call()
}

fun Type.asJavaClass(): Class<Any> = when(this) {
    is Class<*> -> this as Class<Any>
    is ParameterizedType -> rawType as? Class<Any>
            ?: throw UnsupportedOperationException("Unknown type $this")
    else -> throw UnsupportedOperationException("Unknown type $this")
}

internal fun <T> StringBuilder.appendCommaSeparated(
        items: Collection<T>,
        callback: (T) -> Unit) {

    for ((i, item) in items.withIndex()) {
        if (i > 0) append(", ")
        callback(item)
    }
}

internal fun Any?.expectNumber(): Number {
    if (this !is Number) throw SchemaMismatchException("Number expected")
    return this
}

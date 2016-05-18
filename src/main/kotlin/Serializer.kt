package ru.yole.jkid

import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KProperty
import kotlin.reflect.memberProperties
import kotlin.reflect.primaryConstructor

inline fun <reified T> KAnnotatedElement.findAnnotation(): T?
        = annotations.filterIsInstance<T>().firstOrNull()

private fun StringBuilder.serializeString(s: String) {
    append('\"')
    for (c in s) {
        append(when (c) {
            '\\' -> "\\\\"
            '\"' -> "\\\""
            '\b' -> "\\b"
            '\u000C' -> "\\f"
            '\n' -> "\\n"
            '\r' -> "\\r"
            '\t' -> "\\t"
            else -> c
        })
    }
    append('\"')
}

private fun StringBuilder.serializePropertyValue(value: Any?) = when(value) {
    null -> append("null")
    is String -> serializeString(value)
    is Number, is Boolean -> append(value.toString())
    is List<*> -> serializeArray(value as List<Any>)
    else -> serializeObject(value)
}

private fun StringBuilder.serializeProperty(prop: KProperty<Any?>, value: Any?) {
    serializeString(prop.findAnnotation<JsonName>()?.name ?: prop.name)
    append(": ")

    val jsonSerializer = prop.findAnnotation<JsonSerializer>()
    if (jsonSerializer != null) {
        val primaryConstructor = jsonSerializer.serializerClass.primaryConstructor
                ?: throw IllegalArgumentException("Class specified as @JsonSerializer must have a no-arg primary constructor")
        @Suppress("UNCHECKED_CAST")
        val valueSerializer = primaryConstructor.call() as ValueSerializer<Any?>
        serializePropertyValue(valueSerializer.serializeValue(value))
    }
    else {
        serializePropertyValue(value)
    }
}

private fun <T> StringBuilder.appendCommaSeparated(
        items: Collection<T>,
        callback: (T) -> Unit) {

    for ((i, item) in items.withIndex()) {
        if (i > 0) append(", ")
        callback(item)
    }
}

private fun StringBuilder.serializeObject(obj: Any) {
    append("{")
    val properties = obj.javaClass.kotlin
        .memberProperties
        .filter {
            it.findAnnotation<JsonExclude>() == null
        }

    appendCommaSeparated(properties) { prop ->
        val propertyValue = prop.get(obj)
        serializeProperty(prop, propertyValue)
    }

    append("}")
}

private fun StringBuilder.serializeArray(data: List<Any>) {
    append("[")
    appendCommaSeparated(data) {
        serializePropertyValue(it)
    }
    append("]")

}

fun serialize(obj: Any): String = buildString {
    serializeObject(obj)
}

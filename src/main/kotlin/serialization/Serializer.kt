package ru.yole.jkid.serialization

import ru.yole.jkid.*
import kotlin.reflect.KProperty
import kotlin.reflect.memberProperties

fun serialize(obj: Any): String = buildString {
    serializeObject(obj)
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

private fun StringBuilder.serializeProperty(prop: KProperty<Any?>,
                                            value: Any?) {
    val key = prop.findAnnotation<JsonName>()?.name
            ?: prop.name
    val jsonValue = prop.getSerializer()
            ?.toJsonValue(value)
            ?: value

    serializeString(key)
    append(": ")
    serializePropertyValue(jsonValue)
}

fun KProperty<*>.getSerializer(): ValueSerializer<Any?>? {
    val jsonSerializer = findAnnotation<JsonSerializer>()
            ?: return null
    val serializerClass = jsonSerializer.serializerClass

    @Suppress("UNCHECKED_CAST")
    return (serializerClass.objectInstance
                ?: serializerClass.newInstance())
            as ValueSerializer<Any?>
}

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

private fun StringBuilder.serializePropertyValue(value: Any?) {
    when(value) {
        null -> append("null")
        is String -> serializeString(value)
        is Number, is Boolean -> append(value.toString())
        is List<*> -> serializeArray(value as List<Any>)
        else -> serializeObject(value)
    }
}

private fun StringBuilder.serializeArray(data: List<Any>) {
    append("[")
    appendCommaSeparated(data) {
        serializePropertyValue(it)
    }
    append("]")

}

package ru.yole.jkid

import kotlin.reflect.KClass

annotation class JsonName(val value: String)

annotation class JsonSerializer(val serializerClass: KClass<out ValueSerializer<*>>)

interface ValueSerializer<T> {
    fun deserializeValue(jsonValue: Any?): T
    fun serializeValue(value: T): Any?
}

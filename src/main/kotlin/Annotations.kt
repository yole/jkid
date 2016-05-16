package ru.yole.jkid

import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY)
annotation class JsonExclude

@Target(AnnotationTarget.PROPERTY)
annotation class JsonName(val value: String)

@Target(AnnotationTarget.PROPERTY)
annotation class JsonSerializer(val serializerClass: KClass<out ValueSerializer<*>>)

interface ValueSerializer<T> {
    fun deserializeValue(jsonValue: Any?): T
    fun serializeValue(value: T): Any?
}

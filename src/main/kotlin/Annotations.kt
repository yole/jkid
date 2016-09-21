package ru.yole.jkid

import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY)
annotation class JsonExclude

@Target(AnnotationTarget.PROPERTY)
annotation class JsonName(val name: String)

interface ValueSerializer<T> {
    fun toJsonValue(value: T): Any?
    fun fromJsonValue(jsonValue: Any?): T
}

@Target(AnnotationTarget.PROPERTY)
annotation class JsonSerializer(val serializerClass: KClass<out ValueSerializer<*>>)

@Target(AnnotationTarget.PROPERTY)
annotation class JsonDeserialize(val targetClass: KClass<out Any>)

package ru.yole.jkid

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.declaredMemberProperties
import kotlin.reflect.primaryConstructor

class ClassReflectionCache(cls: KClass<*>) {
    private val jsonNameToParamMap = hashMapOf<String, KParameter>()
    private val paramToSerializerMap = hashMapOf<KParameter, ValueSerializer<Any?>>()

    init {
        val constructor = cls.primaryConstructor
        if (constructor != null) {
            for (param in constructor.parameters) {
                cacheDataForParameter(cls, param)
            }
        }
    }

    private fun cacheDataForParameter(cls: KClass<*>, param: KParameter) {
        val paramName = param.name
                ?: throw UnsupportedOperationException("Class has constructor parameter without name")

        val prop = cls.declaredMemberProperties.find { it.name == paramName }
        if (prop != null) {
            val jsonName = prop.annotations.filterIsInstance<JsonName>().singleOrNull()
            jsonNameToParamMap[jsonName?.value ?: paramName] = param

            val jsonSerializer = prop.annotations.filterIsInstance<JsonSerializer>().singleOrNull()
            if (jsonSerializer != null) {
                val primaryConstructor = jsonSerializer.serializerClass.primaryConstructor
                    ?: throw IllegalArgumentException("Class specified as @JsonSerializer must have a no-arg primary constructor")
                val valueSerializer = primaryConstructor.call() as ValueSerializer<Any?>

                paramToSerializerMap[param] = valueSerializer
            }
        }
    }

    fun findParameter(jsonName: String): KParameter? = jsonNameToParamMap[jsonName]
    fun valueSerializerFor(param: KParameter): ValueSerializer<Any?>? = paramToSerializerMap[param]
}

class ReflectionCache {
    private val cacheData = hashMapOf<KClass<*>, ClassReflectionCache>()

    operator fun get(cls: KClass<*>): ClassReflectionCache =
            cacheData.getOrPut(cls) { ClassReflectionCache(cls) }
}

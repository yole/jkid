package ru.yole.jkid.deserialization

import ru.yole.jkid.*
import ru.yole.jkid.serialization.getSerializer
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.declaredMemberProperties
import kotlin.reflect.jvm.javaType
import kotlin.reflect.primaryConstructor

class ConstructorParameterCache(cls: KClass<*>) {
    private val jsonNameToParamMap = hashMapOf<String, KParameter>()
    private val paramToSerializerMap = hashMapOf<KParameter, ValueSerializer<out Any?>>()

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
                ?: throw UnsupportedOperationException(
                    "Class has constructor parameter without name")

        val prop = cls.declaredMemberProperties.find {
            it.name == paramName
        }
        if (prop != null) {
            val jsonName = prop.findAnnotation<JsonName>()
            jsonNameToParamMap[jsonName?.name ?: paramName] = param

            val valueSerializer = prop.getSerializer()
                    ?: serializerForType(param.type.javaType)
            if (valueSerializer != null) {
                paramToSerializerMap[param] = valueSerializer
            }
        }
    }

    fun findParameter(jsonName: String): KParameter? = jsonNameToParamMap[jsonName]
    fun valueSerializerFor(param: KParameter): ValueSerializer<out Any?>? = paramToSerializerMap[param]
}

class ReflectionCache {
    private val cacheData =
            mutableMapOf<KClass<*>, ConstructorParameterCache>()

    operator fun get(cls: KClass<*>)  =
            cacheData.getOrPut(cls) { ConstructorParameterCache(cls) }
}

fun serializerForType(type: Type): ValueSerializer<out Any?>? =
        when(type) {
            Byte::class.java -> ByteSerializer
            Short::class.java -> ShortSerializer
            Int::class.java -> IntSerializer
            Long::class.java -> LongSerializer
            Float::class.java -> FloatSerializer
            Boolean::class.java -> BooleanSerializer
            else -> null
        }

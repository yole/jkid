package ru.yole.jkid.deserialization

import ru.yole.jkid.JsonName
import ru.yole.jkid.ValueSerializer
import ru.yole.jkid.findAnnotation
import ru.yole.jkid.serialization.getSerializer
import ru.yole.jkid.serializerForType
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.declaredMemberProperties
import kotlin.reflect.jvm.javaType
import kotlin.reflect.primaryConstructor

class ReflectionCache {
    private val cacheData =
            mutableMapOf<KClass<*>, ConstructorInfo<*>>()

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(cls: KClass<T>): ConstructorInfo<T> =
            cacheData.getOrPut(cls) { ConstructorInfo(cls) } as ConstructorInfo<T>
}

class ConstructorInfo<T : Any>(cls: KClass<T>) {
    private val constructor = cls.primaryConstructor
            ?: throw UnsupportedOperationException("Class ${cls.qualifiedName} doesn't have a primary constructor")
    private val parameters: List<KParameter>
        get() = constructor.parameters

    private val jsonNameToParamMap = hashMapOf<String, KParameter>()
    private val paramToSerializerMap = hashMapOf<KParameter, ValueSerializer<out Any?>>()

    init {
        parameters.forEach { cacheDataForParameter(cls, it) }
    }

    private fun cacheDataForParameter(cls: KClass<*>, param: KParameter) {
        val paramName = param.name
                ?: throw UnsupportedOperationException("Class ${cls.qualifiedName} has constructor parameter without name")

        val property = cls.declaredMemberProperties.find { it.name == paramName } ?: return
        val name = property.findAnnotation<JsonName>()?.name ?: paramName
        jsonNameToParamMap[name] = param

        val valueSerializer = property.getSerializer()
                ?: serializerForType(param.type.javaType)
                ?: return
        paramToSerializerMap[param] = valueSerializer
    }

    operator fun get(jsonName: String): KParameter? = jsonNameToParamMap[jsonName]

    fun deserializeValue(value: Any?, param: KParameter): Any? {
        val serializer = paramToSerializerMap[param]
        if (serializer != null) {
            return serializer.fromJsonValue(value)
        }
        validateArgumentType(param, value)
        return value
    }

    private fun validateArgumentType(param: KParameter, value: Any?) {
        if (value == null && !param.type.isMarkedNullable) {
            throw SchemaMismatchException("Received null value for non-null parameter ${param.name}")
        }
        if (value != null && value.javaClass != param.type.javaType) {
            throw SchemaMismatchException("Type mismatch for parameter ${param.name}: " +
                    "expected ${param.type.javaType}, found ${value.javaClass}")
        }
    }

    fun callBy(arguments: Map<KParameter, Any?>): T {
        ensureAllParametersPresent(arguments)
        return constructor.callBy(arguments)
    }

    private fun ensureAllParametersPresent(arguments: Map<KParameter, Any?>) {
        for (param in parameters) {
            if (arguments[param] == null && !param.isOptional && !param.type.isMarkedNullable) {
                throw SchemaMismatchException("Missing value for parameter ${param.name}")
            }
        }
    }
}

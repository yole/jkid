package ru.yole.jkid.deserialization

import ru.yole.jkid.asJavaClass
import ru.yole.jkid.isPrimitiveOrString
import ru.yole.jkid.serializerForBasicType
import java.io.Reader
import java.io.StringReader
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.javaType

inline fun <reified T: Any> deserialize(json: String): T {
    return deserialize(StringReader(json))
}

inline fun <reified T: Any> deserialize(json: Reader): T {
    return deserialize(json, T::class)
}

fun <T: Any> deserialize(json: Reader, targetClass: KClass<T>): T {
    val seed = ObjectSeed(targetClass, ClassInfoCache())
    Parser(json, seed).parse()
    return seed.spawn()
}

interface JsonObject {
    fun setSimpleProperty(propertyName: String, value: Any?)

    fun createCompositeProperty(propertyName: String, isCollection: Boolean): JsonObject
}

interface Seed: JsonObject {
    val classInfoCache: ClassInfoCache

    fun spawn(): Any?
}

fun Seed.createSeedForType(paramType: Type, isCollection: Boolean): Seed {
    val paramClass = paramType.asJavaClass()

    if (Collection::class.java.isAssignableFrom(paramClass)) {
        if (!isCollection) throw JKidException("An array expected, not a composite object")
        val parameterizedType = paramType as? ParameterizedType
                ?: throw UnsupportedOperationException("Unsupported parameter type $this")

        val elementType = parameterizedType.actualTypeArguments.single()
        if (elementType.isPrimitiveOrString()) {
            return ValueCollectionSeed(elementType, classInfoCache)
        }
        return ObjectCollectionSeed(elementType, classInfoCache)
    }
    if (isCollection) throw JKidException("Object of the type ${paramType.typeName} expected, not an array")
    return ObjectSeed(paramClass.kotlin, classInfoCache)
}


class ObjectSeed<out T: Any>(
        targetClass: KClass<T>,
        override val classInfoCache: ClassInfoCache
) : Seed {

    private val classInfo: ClassInfo<T> = classInfoCache[targetClass]

    private val valueArguments = mutableMapOf<KParameter, Any?>()
    private val seedArguments = mutableMapOf<KParameter, Seed>()

    private val arguments: Map<KParameter, Any?>
        get() = valueArguments + seedArguments.mapValues { it.value.spawn() }

    override fun setSimpleProperty(propertyName: String, value: Any?) {
        val param = classInfo.getConstructorParameter(propertyName)
        valueArguments[param] = classInfo.deserializeConstructorArgument(param, value)
    }

    override fun createCompositeProperty(propertyName: String, isCollection: Boolean): Seed {
        val param = classInfo.getConstructorParameter(propertyName)
        return createSeedForType(param.type.javaType, isCollection).apply { seedArguments[param] = this }
    }

    override fun spawn(): T = classInfo.createInstance(arguments)
}

class ObjectCollectionSeed(
        val elementType: Type,
        override val classInfoCache: ClassInfoCache
) : Seed {
    private val elements = mutableListOf<Seed>()

    override fun setSimpleProperty(propertyName: String, value: Any?) {
        throw JKidException("Found primitive value in collection of object types")
    }

    override fun createCompositeProperty(propertyName: String, isCollection: Boolean) =
            createSeedForType(elementType, isCollection).apply { elements.add(this) }

    override fun spawn(): Collection<*> = elements.map { it.spawn() }
}

class ValueCollectionSeed(
        elementType: Type,
        override val classInfoCache: ClassInfoCache
) : Seed {
    private val elements = mutableListOf<Any?>()
    private val serializerForType = serializerForBasicType(elementType)

    override fun setSimpleProperty(propertyName: String, value: Any?) {
        elements.add(serializerForType.fromJsonValue(value))
    }

    override fun createCompositeProperty(propertyName: String, isCollection: Boolean): Seed {
        throw JKidException("Found object value in collection of primitive types")
    }

    override fun spawn() = elements
}
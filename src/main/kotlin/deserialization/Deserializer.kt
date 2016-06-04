package ru.yole.jkid.deserialization

import ru.yole.jkid.asJavaClass
import java.io.Reader
import java.io.StringReader
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.javaType
import kotlin.reflect.primaryConstructor

inline fun <reified T: Any> deserialize(json: String): T {
    return deserialize(StringReader(json))
}

inline fun <reified T: Any> deserialize(json: Reader): T {
    return deserialize(json, T::class)
}

fun <T: Any> deserialize(json: Reader, targetClass: KClass<T>): T {
    val seed = ObjectSeed(targetClass, ReflectionCache())
    var currentCompositeSeed: Seed = seed
    val seedStack = Stack<Seed>()

    val callback = object : JsonParseCallback {
        override fun enterObject(propertyName: String) {
            seedStack.push(currentCompositeSeed)
            currentCompositeSeed = currentCompositeSeed.createCompositeSeed(propertyName)
        }

        override fun leaveObject() {
            currentCompositeSeed.spawn()
            currentCompositeSeed = seedStack.pop()
        }

        override fun enterArray(propertyName: String) {
            enterObject(propertyName)
        }

        override fun leaveArray() {
            leaveObject()
        }

        override fun visitValue(propertyName: String, value: Any?) {
            currentCompositeSeed.createSimpleSeed(propertyName, value)
        }
    }
    val parser = Parser(json, callback)
    parser.parse()
    return seed.spawn()
}

class SchemaMismatchException(message: String) : Exception(message)

interface Seed {
    fun createSimpleSeed(propertyName: String, value: Any?): Seed
    fun createCompositeSeed(propertyName: String): Seed
    fun spawn(): Any?
}

class SimpleValueSeed(val value: Any?): Seed {

    private fun error(): Nothing = throw IllegalStateException("Simple value seed can't contain inner values")

    override fun createSimpleSeed(propertyName: String, value: Any?) = error()
    override fun createCompositeSeed(propertyName: String) = error()

    override fun spawn() = value
}

class DeadSeed : Seed {
    private fun error(): Nothing = throw IllegalStateException("Dead seed can't contain inner values or spawn")

    override fun createSimpleSeed(propertyName: String, value: Any?) = error()
    override fun createCompositeSeed(propertyName: String) = error()
    override fun spawn() = error()
}

abstract class CompositeSeed(val reflectionCache: ReflectionCache): Seed {
    internal fun createSeedForType(paramType: Type): Seed {
        val paramClass = paramType.asJavaClass()

        if (Collection::class.java.isAssignableFrom(paramClass)) {
            val parameterizedType = paramType as? ParameterizedType
                    ?: throw UnsupportedOperationException(
                        "Unsupported parameter type $this")

            val elementType = parameterizedType.actualTypeArguments.single()
            return CollectionSeed(elementType, reflectionCache)
        }
        return ObjectSeed(paramClass.kotlin, reflectionCache)
    }
}

class ObjectSeed<out T: Any>(
        targetClass: KClass<T>,
        reflectionCache: ReflectionCache
) : CompositeSeed(reflectionCache) {

    private val constructor = targetClass.primaryConstructor
            ?: throw UnsupportedOperationException(
                "Only classes with primary constructor can be deserialized")

    private val constructorParameterCache = reflectionCache[targetClass]

    private val arguments = mutableMapOf<KParameter, Seed>()
    private fun Seed.record(param: KParameter) = apply { arguments[param] = this }

    override fun createSimpleSeed(propertyName: String, value: Any?): Seed {
        val param = constructorParameterCache.findParameter(propertyName) ?: return DeadSeed()
        return SimpleValueSeed(deserializeValue(value, param)).record(param)
    }

    override fun createCompositeSeed(propertyName: String): Seed {
        val param = constructorParameterCache.findParameter(propertyName) ?: return DeadSeed()
        return createSeedForType(param.type.javaType).record(param)
    }

    private fun deserializeValue(value: Any?, param: KParameter): Any? {
        val serializer = constructorParameterCache.valueSerializerFor(param)
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

    private fun ensureAllParametersPresent(arguments: Map<KParameter, Any?>) {
        for (param in constructor.parameters) {
            if (arguments[param] == null && !param.isOptional && !param.type.isMarkedNullable) {
                throw SchemaMismatchException("Missing value for parameter ${param.name}")
            }
        }
    }

    override fun spawn(): T {
        val argumentValues = arguments.mapValues { it.value.spawn() }
        ensureAllParametersPresent(argumentValues)
        return constructor.callBy(argumentValues)
    }
}

class CollectionSeed(
        val elementType: Type,
        reflectionCache: ReflectionCache
) : CompositeSeed(reflectionCache) {

    private val elements = mutableListOf<Seed>()
    private fun Seed.record() = apply { elements.add(this) }

    override fun createSimpleSeed(propertyName: String, value: Any?) =
        SimpleValueSeed(value).record()

    override fun createCompositeSeed(propertyName: String) =
        createSeedForType(elementType).record()

    override fun spawn(): Collection<*> = elements.map { it.spawn() }
}
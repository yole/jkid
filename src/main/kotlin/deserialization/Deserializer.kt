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
    protected fun createSeedForType(paramType: Type): Seed {
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

    private val constructorInfo = reflectionCache[targetClass]

    private val arguments = mutableMapOf<KParameter, Seed>()
    private fun Seed.record(param: KParameter) = apply { arguments[param] = this }

    override fun createSimpleSeed(propertyName: String, value: Any?): Seed {
        val param = constructorInfo[propertyName] ?: return DeadSeed()
        return SimpleValueSeed(constructorInfo.deserializeValue(value, param)).record(param)
    }

    override fun createCompositeSeed(propertyName: String): Seed {
        val param = constructorInfo[propertyName] ?: return DeadSeed()
        return createSeedForType(param.type.javaType).record(param)
    }

    override fun spawn(): T {
        val argumentValues = arguments.mapValues { it.value.spawn() }
        return constructorInfo.callBy(argumentValues)
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
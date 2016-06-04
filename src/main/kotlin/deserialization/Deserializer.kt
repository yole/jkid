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

inline fun <reified T: Any> deserialize(json: String): T {
    return deserialize(StringReader(json))
}

inline fun <reified T: Any> deserialize(json: Reader): T {
    return deserialize(json, T::class)
}

fun <T: Any> deserialize(json: Reader, targetClass: KClass<T>): T {
    val seed = ObjectSeed(targetClass, ConstructorInfoCache())
    var currentCompositeSeed: CompositeSeed = seed
    val seedStack = Stack<CompositeSeed>()

    val callback = object : JsonParseCallback {
        override fun enterObject(propertyName: String) {
            seedStack.push(currentCompositeSeed)
            currentCompositeSeed = currentCompositeSeed.createCompositeSeed(propertyName)
        }

        override fun leaveObject() {
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
    fun spawn(): Any?
}

class SimpleValueSeed(val value: Any?): Seed {
    override fun spawn() = value
}

abstract class CompositeSeed(val constructorInfoCache: ConstructorInfoCache): Seed {
    abstract fun createSimpleSeed(propertyName: String, value: Any?)
    abstract fun createCompositeSeed(propertyName: String): CompositeSeed

    protected fun createSeedForType(paramType: Type): CompositeSeed {
        val paramClass = paramType.asJavaClass()

        if (Collection::class.java.isAssignableFrom(paramClass)) {
            val parameterizedType = paramType as? ParameterizedType
                    ?: throw UnsupportedOperationException(
                        "Unsupported parameter type $this")

            val elementType = parameterizedType.actualTypeArguments.single()
            return CollectionSeed(elementType, constructorInfoCache)
        }
        return ObjectSeed(paramClass.kotlin, constructorInfoCache)
    }
}

class ObjectSeed<out T: Any>(
        targetClass: KClass<T>,
        constructorInfoCache: ConstructorInfoCache
) : CompositeSeed(constructorInfoCache) {

    private val constructorInfo = constructorInfoCache[targetClass]

    private val arguments = mutableMapOf<KParameter, Seed>()

    override fun createSimpleSeed(propertyName: String, value: Any?) {
        val param = constructorInfo[propertyName]
        val seed = SimpleValueSeed(constructorInfo.deserializeValue(value, param))
        arguments[param] = seed
    }

    override fun createCompositeSeed(propertyName: String): CompositeSeed {
        val param = constructorInfo[propertyName]
        return createSeedForType(param.type.javaType).apply { arguments[param] = this }
    }

    override fun spawn(): T {
        val argumentValues = arguments.mapValues { it.value.spawn() }
        return constructorInfo.callBy(argumentValues)
    }
}

class CollectionSeed(
        val elementType: Type,
        constructorInfoCache: ConstructorInfoCache
) : CompositeSeed(constructorInfoCache) {

    private val elements = mutableListOf<Seed>()

    override fun createSimpleSeed(propertyName: String, value: Any?) {
        val seed = SimpleValueSeed(value)
        elements.add(seed)
    }

    override fun createCompositeSeed(propertyName: String) =
        createSeedForType(elementType).apply { elements.add(this) }

    override fun spawn(): Collection<*> = elements.map { it.spawn() }
}
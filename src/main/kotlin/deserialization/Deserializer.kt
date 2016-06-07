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
    val seed = ObjectSeed(targetClass, ClassInfoCache())
    val stack = Stack<CompositeSeed>()
    stack.push(seed)

    val callback = object : JsonParseCallback {
        override fun enterObject(propertyName: String) {
            val compositeSeed = stack.peek().createCompositeSeed(propertyName)
            stack.push(compositeSeed)
        }

        override fun leaveObject() {
            stack.pop()
        }

        override fun enterArray(propertyName: String) {
            enterObject(propertyName)
        }

        override fun leaveArray() {
            leaveObject()
        }

        override fun visitValue(propertyName: String, value: Any?) {
            stack.peek().createSimpleSeed(propertyName, value)
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

abstract class CompositeSeed(val classInfoCache: ClassInfoCache): Seed {
    abstract fun createSimpleSeed(propertyName: String, value: Any?)
    abstract fun createCompositeSeed(propertyName: String): CompositeSeed

    protected fun createSeedForType(paramType: Type): CompositeSeed {
        val paramClass = paramType.asJavaClass()

        if (Collection::class.java.isAssignableFrom(paramClass)) {
            val parameterizedType = paramType as? ParameterizedType
                    ?: throw UnsupportedOperationException(
                        "Unsupported parameter type $this")

            val elementType = parameterizedType.actualTypeArguments.single()
            return CollectionSeed(elementType, classInfoCache)
        }
        return ObjectSeed(paramClass.kotlin, classInfoCache)
    }
}

class ObjectSeed<out T: Any>(
        targetClass: KClass<T>,
        classInfoCache: ClassInfoCache
) : CompositeSeed(classInfoCache) {

    private val classInfo = classInfoCache[targetClass]

    private val valueArguments = mutableMapOf<KParameter, Any?>()
    private val seedArguments = mutableMapOf<KParameter, CompositeSeed>()

    override fun createSimpleSeed(propertyName: String, value: Any?) {
        val param = classInfo.getConstructorParameter(propertyName)
        valueArguments[param] = classInfo.deserializeConstructorArgument(param, value)
    }

    override fun createCompositeSeed(propertyName: String): CompositeSeed {
        val param = classInfo.getConstructorParameter(propertyName)
        return createSeedForType(param.type.javaType).apply { seedArguments[param] = this }
    }

    override fun spawn(): T {
        val argumentValues = valueArguments + seedArguments.mapValues { it.value.spawn() }
        return classInfo.newInstance(argumentValues)
    }
}

class CollectionSeed(
        val elementType: Type,
        classInfoCache: ClassInfoCache
) : CompositeSeed(classInfoCache) {

    private val elements = mutableListOf<Seed>()

    override fun createSimpleSeed(propertyName: String, value: Any?) {
        val seed = SimpleValueSeed(value)
        elements.add(seed)
    }

    override fun createCompositeSeed(propertyName: String) =
        createSeedForType(elementType).apply { elements.add(this) }

    override fun spawn(): Collection<*> = elements.map { it.spawn() }
}
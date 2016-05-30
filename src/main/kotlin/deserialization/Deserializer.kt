package ru.yole.jkid.deserialization

import ru.yole.jkid.asJavaClass
import java.io.Reader
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.javaType
import kotlin.reflect.primaryConstructor

inline fun <reified T: Any> deserialize(json: Reader): T {
    return deserialize(json, T::class)
}

fun <T: Any> deserialize(json: Reader, targetClass: KClass<T>): T {
    val seed = ObjectSeed(targetClass, ReflectionCache())
    var currentSeed: Seed = seed
    val seedStack = Stack<Seed>()

    val callback = object : JsonParseCallback {
        override fun enterObject(propertyName: String) {
            seedStack.push(currentSeed)
            currentSeed = currentSeed.buildCompositeValue(propertyName)
        }

        override fun leaveObject() {
            currentSeed.done()
            currentSeed = seedStack.pop()
        }

        override fun enterArray(propertyName: String) {
            enterObject(propertyName)
        }

        override fun leaveArray() {
            leaveObject()
        }

        override fun visitValue(propertyName: String, value: Any?) {
            currentSeed.setSimpleValue(propertyName, value)
        }
    }
    val parser = Parser(json, callback)
    parser.parse()
    return seed.spawn()
}

class SchemaMismatchException(message: String) : Exception(message)

abstract class Seed(val reflectionCache: ReflectionCache,
                    val onDone: (Any) -> Unit = {}) {

    abstract fun setSimpleValue(propertyName: String, value: Any?)
    abstract fun buildCompositeValue(propertyName: String): Seed
    abstract fun done()

    internal fun seedForType(paramType: Type, onDone: (Any) -> Unit): Seed {
        val paramClass = paramType.asJavaClass()

        if (Collection::class.java.isAssignableFrom(paramClass)) {
            val parameterizedType = paramType as? ParameterizedType
                    ?: throw UnsupportedOperationException(
                        "Unsupported parameter type $this")

            val elementType = parameterizedType.actualTypeArguments.single()
            return CollectionSeed(elementType, reflectionCache, onDone)
        }
        return ObjectSeed(paramClass.kotlin, reflectionCache, onDone)
    }
}

class ObjectSeed<out T: Any>(targetClass: KClass<T>,
                             reflectionCache: ReflectionCache,
                             onDone: (Any) -> Unit = {})
        : Seed(reflectionCache, onDone) {

    private val constructor = targetClass.primaryConstructor
            ?: throw UnsupportedOperationException(
                "Only classes with primary constructor can be deserialized")

    private val constructorParameterCache = reflectionCache[targetClass]

    private val arguments = mutableMapOf<KParameter, Any?>()

    override fun setSimpleValue(propertyName: String, value: Any?) {
        val param = constructorParameterCache.findParameter(propertyName)
                ?: return
        arguments[param] = deserializeValue(value, param)
    }

    override fun buildCompositeValue(propertyName: String): Seed {
        val param = constructorParameterCache.findParameter(propertyName)
                ?: return DeadSeed(this)

        return seedForType(param.type.javaType) { value ->
            arguments[param] = value
        }
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

    fun spawn(): T {
        ensureAllParametersPresent()
        return constructor.callBy(arguments)
    }

    private fun ensureAllParametersPresent() {
        for (param in constructor.parameters) {
            if (arguments[param] == null && !param.isOptional && !param.type.isMarkedNullable) {
                throw SchemaMismatchException("Missing value for parameter ${param.name}")
            }
        }
    }

    override fun done() {
        onDone(spawn())
    }
}

class CollectionSeed(val elementType: Type,
                     reflectionCache: ReflectionCache,
                     onDone: (Any) -> Unit) : Seed(reflectionCache, onDone) {

    private val elements = mutableListOf<Any?>()

    override fun setSimpleValue(propertyName: String, value: Any?) {
        elements.add(value)
    }

    override fun buildCompositeValue(propertyName: String): Seed {
        return seedForType(elementType) { value -> elements.add(value) }
    }

    override fun done() {
        onDone(elements)
    }
}

class DeadSeed(parent: Seed) : Seed(parent.reflectionCache, {}) {
    override fun setSimpleValue(propertyName: String, value: Any?) { }
    override fun buildCompositeValue(propertyName: String): Seed = this
    override fun done() {}
}


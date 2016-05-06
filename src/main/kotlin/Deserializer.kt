package ru.yole.jkid

import java.io.Reader
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.javaType
import kotlin.reflect.primaryConstructor

class SchemaMismatchException(message: String) : Exception(message)

fun Type.asJavaClass(): Class<Any> = when(this) {
    is Class<*> -> this as Class<Any>
    is ParameterizedType -> rawType as? Class<Any>
            ?: throw UnsupportedOperationException("Unsupported parameter type $this")
    else -> throw UnsupportedOperationException("Unsupported parameter type $this")
}

interface IObjectSeed {
    fun setProperty(name: String, value: Any?)
    fun seedForPropertyValue(propertyName: String): IObjectSeed
    fun plantIntoParent(): IObjectSeed
}

interface IParentSeed : IObjectSeed {
    fun plant(index: Int, value: Any?)
}

abstract class SeedWithParent(val parent: IParentSeed? = null,
                              val parentIndex: Int = -1) : IObjectSeed {

    override fun plantIntoParent(): IObjectSeed {
        parent!!.plant(parentIndex, spawn())
        return parent
    }

    abstract fun spawn(): Any?
}

class ObjectSeed<out T: Any>(targetClass: KClass<T>,
                             parent: IParentSeed? = null,
                             parentIndex: Int = -1) : SeedWithParent(parent, parentIndex), IParentSeed {
    private val constructor = targetClass.primaryConstructor
            ?: throw UnsupportedOperationException("Only classes with primary constructor can be deserialized")
    private val parameters = constructor.parameters
    private val arguments = arrayOfNulls<Any?>(parameters.size)

    override fun setProperty(name: String, value: Any?) {
        val (i, param) = findParameter(name) ?: return
        arguments[i] = coerceType(value, param)
    }

    override fun seedForPropertyValue(propertyName: String): IObjectSeed {
        val (i, param) = findParameter(propertyName) ?: return DeadSeed(this)

        val paramType = param.type.javaType
        return seedForType(this, i, paramType)
    }

    private fun findParameter(name: String): Pair<Int, KParameter>? {
        val index = parameters.indexOfFirst { it.name == name }
        return if (index == -1) null else index to parameters[index]
    }

    private fun coerceType(value: Any?, param: KParameter): Any? {
        if (value == null && !param.type.isMarkedNullable) {
            throw SchemaMismatchException("Received null value for non-null parameter ${param.name}")
        }
        if (value != null) {
            if (value.javaClass != param.type.javaType) {
                throw SchemaMismatchException("Type mismatch for parameter ${param.name}: " +
                        "expected ${param.type.javaType}, found ${value.javaClass}")
            }
        }
        return value

    }

    override fun spawn(): T {
        for ((param, arg) in parameters zip arguments) {
            if (arg == null && !param.isOptional && !param.type.isMarkedNullable) {
                throw SchemaMismatchException("Missing value for parameter ${param.name}")
            }
        }
        return constructor.call(*arguments)
    }

    override fun plant(index: Int, value: Any?) {
        arguments[index] = value
    }

    companion object {
        internal fun seedForType(parent: IParentSeed, paramIndex: Int, paramType: Type): SeedWithParent {
            val paramClass = paramType.asJavaClass()

            if (Collection::class.java.isAssignableFrom(paramClass)) {
                val parameterizedType = paramType as? ParameterizedType ?:
                        throw UnsupportedOperationException("Unsupported parameter type $this")

                return CollectionSeed(parameterizedType.actualTypeArguments.single(), parent, paramIndex)
            }
            return ObjectSeed(paramClass.kotlin, parent, paramIndex)
        }

    }
}

class CollectionSeed(val elementType: Type,
                     parent: IParentSeed,
                     parentIndex: Int) : SeedWithParent(parent, parentIndex), IParentSeed {

    private val elements = mutableListOf<Any?>()

    override fun setProperty(name: String, value: Any?) {
        elements.add(value)
    }

    override fun seedForPropertyValue(propertyName: String): IObjectSeed {
        return ObjectSeed.seedForType(this, parentIndex, elementType)
    }

    override fun spawn() = elements

    override fun plant(index: Int, value: Any?) {
        elements.add(value)
    }
}

class DeadSeed(val parent: IObjectSeed) : IObjectSeed {
    override fun setProperty(name: String, value: Any?) { }
    override fun seedForPropertyValue(propertyName: String): IObjectSeed = this
    override fun plantIntoParent(): IObjectSeed = parent
}

fun <T: Any> deserialize(json: Reader, targetClass: KClass<T>): T {

    val seed = ObjectSeed(targetClass)
    var currentSeed: IObjectSeed = seed
    val callback = object : JsonParseCallback {
        override fun enterObject(propertyName: String) {
            currentSeed = currentSeed.seedForPropertyValue(propertyName)
        }

        override fun leaveObject() {
            currentSeed = currentSeed.plantIntoParent()
        }

        override fun enterArray(propertyName: String) {
            currentSeed = currentSeed.seedForPropertyValue(propertyName)
        }

        override fun leaveArray() {
            currentSeed = currentSeed.plantIntoParent()
        }

        override fun visitProperty(propertyName: String, value: Token.ValueToken) {
            currentSeed.setProperty(propertyName, value.value)
        }
    }
    val parser = Parser(json, callback)
    parser.parse()
    return seed.spawn()
}

inline fun <reified T: Any> deserialize(json: Reader): T {
    return deserialize(json, T::class)
}

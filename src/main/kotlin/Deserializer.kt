package ru.yole.jkid

import java.io.Reader
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.javaType
import kotlin.reflect.primaryConstructor

class SchemaMismatchException(message: String) : Exception(message)

interface IObjectSeed {
    fun setProperty(name: String, value: Any?)
    fun seedForPropertyValue(propertyName: String): IObjectSeed
    fun plantIntoParent(): IObjectSeed
}

open class ObjectSeed<out T: Any>(targetClass: KClass<T>,
                                  val parent: ObjectSeed<Any>? = null,
                                  val parentIndex: Int = -1) : IObjectSeed {
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
        val paramClass = (param.type.javaType as? Class<Any>)?.kotlin
                ?: throw UnsupportedOperationException("Unsupported parameter type ${param.type.javaType}")
        return ObjectSeed(paramClass, this, i)
    }

    override fun plantIntoParent(): IObjectSeed {
        parent!!.arguments[parentIndex] = spawn()
        return parent
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

    fun spawn(): T {
        for ((param, arg) in parameters zip arguments) {
            if (arg == null && !param.isOptional && !param.type.isMarkedNullable) {
                throw SchemaMismatchException("Missing value for parmater ${param.name}")
            }
        }
        return constructor.call(*arguments)
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
            throw UnsupportedOperationException()
        }

        override fun leaveArray() {
            throw UnsupportedOperationException()
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

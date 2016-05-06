package ru.yole.jkid

import java.io.Reader
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.javaType
import kotlin.reflect.primaryConstructor

class SchemaMismatchException(message: String) : Exception(message)

class ObjectSeed<T: Any>(val constructor: KFunction<T>) {
    private val parameters = constructor.parameters
    private val arguments = arrayOfNulls<Any?>(parameters.size)

    fun setProperty(name: String, value: Any?) {
        for ((i, param) in parameters.withIndex()) {
            if (param.name == name) {
                arguments[i] = coerceType(value, param)
                break
            }
        }
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

fun <T: Any> deserialize(json: Reader, targetClass: KClass<T>): T {
    val primaryConstructor = targetClass.primaryConstructor
        ?: throw UnsupportedOperationException("Only classes with primary constructor can be deserialized")

    val seed = ObjectSeed(primaryConstructor)
    val callback = object : JsonParseCallback {
        override fun enterObject(propertyName: String) {
            throw UnsupportedOperationException()
        }

        override fun leaveObject() {
            throw UnsupportedOperationException()
        }

        override fun enterArray(propertyName: String) {
            throw UnsupportedOperationException()
        }

        override fun leaveArray() {
            throw UnsupportedOperationException()
        }

        override fun visitProperty(propertyName: String, value: Token.ValueToken) {
            seed.setProperty(propertyName, value.value)
        }
    }
    val parser = Parser(json, callback)
    parser.parse()
    return seed.spawn()
}

inline fun <reified T: Any> deserialize(json: Reader): T {
    return deserialize(json, T::class)
}

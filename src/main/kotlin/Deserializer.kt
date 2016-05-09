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

val KParameter.jsonName: String?
    get() = annotations.filterIsInstance<JsonName>().firstOrNull()?.value ?: name

abstract class Seed(val parent: Seed? = null,
                    val parentParameter: KParameter? = null)  {

    abstract fun setProperty(name: String, value: Any?)
    abstract fun seedForPropertyValue(propertyName: String): Seed
    protected abstract fun plant(param: KParameter, value: Any?)

    open fun plantIntoParent(): Seed {
        parent!!.plant(parentParameter!!, spawn())
        return parent
    }

    abstract fun spawn(): Any?
}

class ObjectSeed<out T: Any>(targetClass: KClass<T>,
                             parent: Seed? = null,
                             parentParameter: KParameter? = null) : Seed(parent, parentParameter){
    private val constructor = targetClass.primaryConstructor
            ?: throw UnsupportedOperationException("Only classes with primary constructor can be deserialized")
    private val parameters = constructor.parameters
    private val arguments = mutableMapOf<KParameter, Any?>()

    override fun setProperty(name: String, value: Any?) {
        val param = findParameter(name) ?: return
        arguments[param] = coerceType(value, param)
    }

    override fun seedForPropertyValue(propertyName: String): Seed {
        val param = findParameter(propertyName) ?: return DeadSeed(this)

        return seedForType(this, param, param.type.javaType)
    }

    private fun findParameter(name: String): KParameter? = parameters.find { it.jsonName == name }

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
        for (param in parameters) {
            if (arguments[param] == null && !param.isOptional && !param.type.isMarkedNullable) {
                throw SchemaMismatchException("Missing value for parameter ${param.name}")
            }
        }
        return constructor.callBy(arguments)
    }

    override fun plant(param: KParameter, value: Any?) {
        arguments[param] = value
    }

    companion object {
        internal fun seedForType(parent: Seed, param: KParameter?, paramType: Type): Seed {
            val paramClass = paramType.asJavaClass()

            if (Collection::class.java.isAssignableFrom(paramClass)) {
                val parameterizedType = paramType as? ParameterizedType ?:
                        throw UnsupportedOperationException("Unsupported parameter type $this")

                return CollectionSeed(parameterizedType.actualTypeArguments.single(), parent, param)
            }
            return ObjectSeed(paramClass.kotlin, parent, param)
        }

    }
}

class CollectionSeed(val elementType: Type,
                     parent: Seed,
                     parentParameter: KParameter?) : Seed(parent, parentParameter) {

    private val elements = mutableListOf<Any?>()

    override fun setProperty(name: String, value: Any?) {
        elements.add(value)
    }

    override fun seedForPropertyValue(propertyName: String): Seed {
        return ObjectSeed.seedForType(this, parentParameter, elementType)
    }

    override fun spawn() = elements

    override fun plant(param: KParameter, value: Any?) {
        elements.add(value)
    }
}

class DeadSeed(parent: Seed) : Seed(parent, null) {
    override fun setProperty(name: String, value: Any?) { }
    override fun seedForPropertyValue(propertyName: String): Seed = this
    override fun plantIntoParent(): Seed = parent!!

    override fun plant(param: KParameter, value: Any?) = throw UnsupportedOperationException()

    override fun spawn(): Any? = throw UnsupportedOperationException()
}

fun <T: Any> deserialize(json: Reader, targetClass: KClass<T>): T {

    val seed = ObjectSeed(targetClass)
    var currentSeed: Seed = seed
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

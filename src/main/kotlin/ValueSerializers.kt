package ru.yole.jkid

import ru.yole.jkid.deserialization.JKidException
import java.lang.reflect.Type

fun serializerForBasicType(type: Type): ValueSerializer<out Any?> {
    assert(type.isPrimitiveOrString()) { "Expected primitive type or String: ${type.typeName}" }
    return serializerForType(type)!!
}

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
fun serializerForType(type: Type): ValueSerializer<out Any?>? =
        when (type) {
            Byte::class.java, java.lang.Byte::class.java -> ByteSerializer
            Short::class.java, java.lang.Short::class.java -> ShortSerializer
            Int::class.java, java.lang.Integer::class.java -> IntSerializer
            Long::class.java, java.lang.Long::class.java -> LongSerializer
            Float::class.java, java.lang.Float::class.java -> FloatSerializer
            Double::class.java, java.lang.Double::class.java -> DoubleSerializer
            Boolean::class.java, java.lang.Boolean::class.java -> BooleanSerializer
            String::class.java -> StringSerializer
            else -> null
        }

private fun Any?.expectNumber(): Number {
    if (this !is Number) throw JKidException("Expected number, was: $this")
    return this
}

object ByteSerializer : ValueSerializer<Byte> {
    override fun fromJsonValue(jsonValue: Any?) = jsonValue.expectNumber().toByte()
    override fun toJsonValue(value: Byte) = value
}

object ShortSerializer : ValueSerializer<Short> {
    override fun fromJsonValue(jsonValue: Any?) = jsonValue.expectNumber().toShort()
    override fun toJsonValue(value: Short) = value
}

object IntSerializer : ValueSerializer<Int> {
    override fun fromJsonValue(jsonValue: Any?) = jsonValue.expectNumber().toInt()
    override fun toJsonValue(value: Int) = value
}

object LongSerializer : ValueSerializer<Long> {
    override fun fromJsonValue(jsonValue: Any?) = jsonValue.expectNumber().toLong()
    override fun toJsonValue(value: Long) = value
}

object FloatSerializer : ValueSerializer<Float> {
    override fun fromJsonValue(jsonValue: Any?) = jsonValue.expectNumber().toFloat()
    override fun toJsonValue(value: Float) = value
}

object DoubleSerializer : ValueSerializer<Double> {
    override fun fromJsonValue(jsonValue: Any?) = jsonValue.expectNumber().toDouble()
    override fun toJsonValue(value: Double) = value
}

object BooleanSerializer : ValueSerializer<Boolean> {
    override fun fromJsonValue(jsonValue: Any?): Boolean {
        if (jsonValue !is Boolean) throw JKidException("Expected boolean, was: $jsonValue")
        return jsonValue
    }

    override fun toJsonValue(value: Boolean) = value
}

object StringSerializer : ValueSerializer<String?> {
    override fun fromJsonValue(jsonValue: Any?): String? {
        if (jsonValue !is String?) throw JKidException("Expected string, was: $jsonValue")
        return jsonValue
    }

    override fun toJsonValue(value: String?) = value
}
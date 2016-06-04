package ru.yole.jkid

import ru.yole.jkid.deserialization.SchemaMismatchException
import java.lang.reflect.Type

fun serializerForType(type: Type): ValueSerializer<out Any?>? =
        when (type) {
            Byte::class.java -> ByteSerializer
            Short::class.java -> ShortSerializer
            Int::class.java -> IntSerializer
            Long::class.java -> LongSerializer
            Float::class.java -> FloatSerializer
            Boolean::class.java -> BooleanSerializer
            else -> null
        }

private fun Any?.expectNumber(): Number {
    if (this !is Number) throw SchemaMismatchException("Number expected")
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

object BooleanSerializer : ValueSerializer<Boolean> {
    override fun fromJsonValue(jsonValue: Any?): Boolean {
        if (jsonValue !is Boolean) throw SchemaMismatchException("Boolean expected")
        return jsonValue
    }

    override fun toJsonValue(value: Boolean) = value
}
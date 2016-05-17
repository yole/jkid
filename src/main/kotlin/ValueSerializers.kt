package ru.yole.jkid

import java.util.*

object ByteSerializer : ValueSerializer<Byte> {
    override fun deserializeValue(jsonValue: Any?) = jsonValue.expectNumber().toByte()
    override fun serializeValue(value: Byte) = value.toDouble()
}

object ShortSerializer : ValueSerializer<Short> {
    override fun deserializeValue(jsonValue: Any?) = jsonValue.expectNumber().toShort()
    override fun serializeValue(value: Short) = value.toDouble()
}

object IntSerializer : ValueSerializer<Int> {
    override fun deserializeValue(jsonValue: Any?) = jsonValue.expectNumber().toInt()
    override fun serializeValue(value: Int) = value.toDouble()
}

object LongSerializer : ValueSerializer<Long> {
    override fun deserializeValue(jsonValue: Any?) = jsonValue.expectNumber().toLong()
    override fun serializeValue(value: Long) = value.toDouble()
}

object FloatSerializer : ValueSerializer<Float> {
    override fun deserializeValue(jsonValue: Any?) = jsonValue.expectNumber().toFloat()
    override fun serializeValue(value: Float) = value.toDouble()
}

object BooleanSerializer : ValueSerializer<Boolean> {
    override fun deserializeValue(jsonValue: Any?): Boolean {
        if (jsonValue !is Boolean) throw SchemaMismatchException("Boolean expected")
        return jsonValue
    }

    override fun serializeValue(value: Boolean) = value
}

private fun Any?.expectNumber(): Number {
    if (this !is Number) throw SchemaMismatchException("Number expected")
    return this
}

class TimestampSerializer : ValueSerializer<Date> {
    override fun serializeValue(value: Date): Any? = value.time

    override fun deserializeValue(jsonValue: Any?): Date = Date(jsonValue.expectNumber().toLong())
}

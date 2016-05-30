package ru.yole.jkid

import ru.yole.jkid.deserialization.SchemaMismatchException
import java.util.*

object ByteSerializer : ValueSerializer<Byte> {
    override fun fromJsonValue(jsonValue: Any?) = jsonValue.expectNumber().toByte()
    override fun toJsonValue(value: Byte) = value.toDouble()
}

object ShortSerializer : ValueSerializer<Short> {
    override fun fromJsonValue(jsonValue: Any?) = jsonValue.expectNumber().toShort()
    override fun toJsonValue(value: Short) = value.toDouble()
}

object IntSerializer : ValueSerializer<Int> {
    override fun fromJsonValue(jsonValue: Any?) = jsonValue.expectNumber().toInt()
    override fun toJsonValue(value: Int) = value.toDouble()
}

object LongSerializer : ValueSerializer<Long> {
    override fun fromJsonValue(jsonValue: Any?) = jsonValue.expectNumber().toLong()
    override fun toJsonValue(value: Long) = value.toDouble()
}

object FloatSerializer : ValueSerializer<Float> {
    override fun fromJsonValue(jsonValue: Any?) = jsonValue.expectNumber().toFloat()
    override fun toJsonValue(value: Float) = value.toDouble()
}

object BooleanSerializer : ValueSerializer<Boolean> {
    override fun fromJsonValue(jsonValue: Any?): Boolean {
        if (jsonValue !is Boolean) throw SchemaMismatchException("Boolean expected")
        return jsonValue
    }

    override fun toJsonValue(value: Boolean) = value
}

object TimestampSerializer : ValueSerializer<Date> {
    override fun toJsonValue(value: Date): Any? = value.time

    override fun fromJsonValue(jsonValue: Any?): Date
        = Date((jsonValue as Number).toLong())
}

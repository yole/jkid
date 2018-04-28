package ru.yole.jkid.serialization

import ru.yole.jkid.ValueSerializer
import java.text.SimpleDateFormat
import java.util.*

class DateSerializer(val format: String = "dd-MM-yyyy"): ValueSerializer<Any?> {

    override fun toJsonValue(value: Any?): Any? {
        val date = value as Date
        val format = SimpleDateFormat(format)
        return format.format(date)
    }

    override fun fromJsonValue(jsonValue: Any?): Date? {
        val jsonString = jsonValue as String
        val format = SimpleDateFormat(format)
        return format.parse(jsonString)
    }
}
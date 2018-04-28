package ru.yole.jkid

import java.text.SimpleDateFormat
import java.util.*

@Target(AnnotationTarget.PROPERTY)
annotation class DateFormat(val format: String)

class DateSerializer(val format: String): ValueSerializer<Any?> {

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
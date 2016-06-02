package ru.yole.jkid.examples.jsonSerializerTest

import ru.yole.jkid.deserialization.deserialize
import ru.yole.jkid.serialization.serialize
import kotlin.test.assertEquals

inline fun <reified T: Any> testJsonSerializer(value: T, json: String) {

    assertEquals(json, serialize(value))

    assertEquals(value, deserialize(json))
}
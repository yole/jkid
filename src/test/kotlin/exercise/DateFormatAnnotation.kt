package ru.yole.jkid.exercise

import org.junit.Test
import ru.yole.jkid.DateFormat
import ru.yole.jkid.deserialization.deserialize
import ru.yole.jkid.serialization.serialize
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.assertEquals

data class Person(
        val name: String,
        @DateFormat("dd-MM-yyyy") val birthDate: Date
)

data class Person3(
        val name: String,
        @DateFormat("MM-dd-yyyy") val birthDate: Date
)


//@Ignore
class DateFormatTest {
    private val value = Person("Alice", SimpleDateFormat("dd-MM-yyyy").parse("13-02-1987"))
    private val json = """{"birthDate": "13-02-1987", "name": "Alice"}"""

    val value2 = Person("Alice", SimpleDateFormat("dd-MM-yyyy").parse("13-02-1988"))
    val json2 = """{"birthDate": "13-02-1988", "name": "Alice"}"""

    val value3 = Person3("Alice", SimpleDateFormat("MM-dd-yyyy").parse("02-13-1987"))
    val json3 = """{"birthDate": "02-13-1987", "name": "Alice"}"""

    @Test
    fun testSerialization() {
        assertEquals(json, serialize(value))
    }

    @Test
    fun testDeserialization() {
        assertEquals(value, deserialize(json))
    }

    @Test
    fun testSerialization2() {
        assertEquals(json2, serialize(value2))
    }

    @Test
    fun testDeserialization2() {
        assertEquals(value2, deserialize(json2))
    }

    @Test
    fun testSerialization3() {
        assertEquals(json3, serialize(value3))
    }

    @Test
    fun testDeserialization3() {
        assertEquals(value3, deserialize(json3))
    }
}

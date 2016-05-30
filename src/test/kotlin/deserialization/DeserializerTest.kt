package ru.yole.jkid.deserialization

import org.junit.Test
import ru.yole.jkid.*
import java.io.StringReader
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DeserializerTest {
    @Test fun testSimple() {
        val result = deserialize<SingleStringProp>(StringReader("""{"s": "x"}"""))
        assertEquals("x", result.s)
    }

    @Test fun testIntLong() {
        val result = deserialize<TwoIntProp>(StringReader("""{"i1": 42, "i2": 239}"""))
        assertEquals(42, result.i1)
        assertEquals(239, result.i2)
    }

    @Test fun testTwoBools() {
        val result = deserialize<TwoBoolProp>(StringReader("""{"b1": true, "b2": false}"""))
        assertEquals(true, result.b1)
        assertEquals(false, result.b2)
    }

    @Test fun testNullableString() {
        val result = deserialize<SingleNullableStringProp>(StringReader("""{"s": null}"""))
        assertNull(result.s)
    }

    @Test fun testObject() {
        val result = deserialize<SingleObjectProp>(StringReader("""{"o": {"s": "x"}}"""))
        assertEquals("x", result.o.s)
    }

    @Test fun testArray() {
        val result = deserialize<SingleListProp>(StringReader("""{"o": ["a", "b"]}"""))
        assertEquals(2, result.o.size)
        assertEquals("b", result.o[1])
    }

    @Test fun testObjectArray() {
        val result = deserialize<SingleObjectListProp>(StringReader("""{"o": [{"s": "a"}, {"s": "b"}]}"""))
        assertEquals(2, result.o.size)
        assertEquals("b", result.o[1].s)
    }

    @Test fun testOptionalArg() {
        val result = deserialize<SingleOptionalProp>(StringReader("{}"))
        assertEquals("foo", result.s)
    }

    @Test fun testJsonName() {
        val result = deserialize<SingleAnnotatedStringProp>(StringReader("""{"q": "x"}"""))
        assertEquals("x", result.s)
    }

    @Test fun testCustomDeserializer() {
        val result = deserialize<SingleCustomSerializedProp>(StringReader("""{"x": "ONE"}"""))
        assertEquals(1, result.x)
    }

    @Test fun testTimestampSerializer() {
        val result = deserialize<SingleDateProp>(StringReader("""{"x": 2000}"""))
        assertEquals(2000, result.x.time)
    }

    @Test fun testPropertyTypeMismatch() {
        assertFailsWith<SchemaMismatchException> {
            deserialize<SingleStringProp>(StringReader("{\"s\": 1}"))
        }
    }

    @Test fun testPropertyTypeMismatchNull() {
        assertFailsWith<SchemaMismatchException> {
            deserialize<SingleStringProp>(StringReader("{\"s\": null}"))
        }
    }

    @Test fun testMissingPropertyException() {
        assertFailsWith<SchemaMismatchException> {
            deserialize<SingleStringProp>(StringReader("{}"))
        }
    }
}

data class SingleStringProp(val s: String)

data class SingleNullableStringProp(val s: String?)

data class TwoIntProp(val i1: Int, val i2: Long)

data class TwoBoolProp(val b1: Boolean, val b2: Boolean)

data class SingleObjectProp(val o: SingleStringProp)

data class SingleListProp(val o: List<String>)

data class SingleObjectListProp(val o: List<SingleStringProp>)

data class SingleOptionalProp(val s: String = "foo")

data class SingleAnnotatedStringProp(@JsonName("q") val s: String)

data class TwoPropsOneExcluded(val s: String, @JsonExclude val x: String = "")

class NumberSerializer: ValueSerializer<Int> {
    override fun fromJsonValue(jsonValue: Any?): Int = when(jsonValue) {
        "ZERO" -> 0
        "ONE" -> 1
        else -> throw SchemaMismatchException("Unexpected value $jsonValue")
    }

    override fun toJsonValue(value: Int): Any? = when(value) {
        0 -> "ZERO"
        1 -> "ONE"
        else -> "?"
    }
}

data class SingleCustomSerializedProp(@JsonSerializer(NumberSerializer::class) val x: Int)

data class SingleDateProp(@JsonSerializer(TimestampSerializer::class) val x: Date)

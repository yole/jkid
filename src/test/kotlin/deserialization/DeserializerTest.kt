package ru.yole.jkid.deserialization

import org.junit.Test
import ru.yole.jkid.*
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DeserializerTest {
    @Test fun testSimple() {
        val result = deserialize<SingleStringProp>("""{"s": "x"}""")
        assertEquals("x", result.s)
    }

    @Test fun testIntLong() {
        val result = deserialize<TwoIntProp>("""{"i1": 42, "i2": 239}""")
        assertEquals(42, result.i1)
        assertEquals(239, result.i2)
    }

    @Test fun testTwoBools() {
        val result = deserialize<TwoBoolProp>("""{"b1": true, "b2": false}""")
        assertEquals(true, result.b1)
        assertEquals(false, result.b2)
    }

    @Test fun testNullableString() {
        val result = deserialize<SingleNullableStringProp>("""{"s": null}""")
        assertNull(result.s)
    }

    @Test fun testObject() {
        val result = deserialize<SingleObjectProp>("""{"o": {"s": "x"}}""")
        assertEquals("x", result.o.s)
    }

    @Test fun testList() {
        val result = deserialize<SingleListProp>("""{"o": ["a", "b"]}""")
        assertEquals(2, result.o.size)
        assertEquals("b", result.o[1])
    }

    @Test fun testNullableList() {
        val result = deserialize<SingleListProp>("""{"o": [null, "b"]}""")
        assertEquals(2, result.o.size)
        assertEquals(null, result.o.first())
    }

    @Test fun testObjectList() {
        val result = deserialize<SingleObjectListProp>("""{"o": [{"s": "a"}, {"s": "b"}]}""")
        assertEquals(2, result.o.size)
        assertEquals("b", result.o[1].s)
    }

    @Test fun testOptionalArg() {
        val result = deserialize<SingleOptionalProp>("{}")
        assertEquals("foo", result.s)
    }

    @Test fun testJsonName() {
        val result = deserialize<SingleAnnotatedStringProp>("""{"q": "x"}""")
        assertEquals("x", result.s)
    }

    @Test fun testCustomDeserializer() {
        val result = deserialize<SingleCustomSerializedProp>("""{"x": "ONE"}""")
        assertEquals(1, result.x)
    }

    @Test fun testTimestampSerializer() {
        val result = deserialize<SingleDateProp>("""{"x": 2000}""")
        assertEquals(2000, result.x.time)
    }

    @Test fun testJsonDeserialize() {
        val result = deserialize<ValueHolder>("""{"value": {"name": "Foo"}}""")
        assertEquals("Foo", result.value.name)
    }

    @Test fun testPropertyTypeMismatch() {
        assertFailsWith<JKidException> {
            deserialize<SingleStringProp>("{\"s\": 1}")
        }
    }

    @Test fun testPropertyTypeMismatchNull() {
        assertFailsWith<JKidException> {
            deserialize<SingleStringProp>("{\"s\": null}")
        }
    }

    @Test fun testMissingPropertyException() {
        assertFailsWith<JKidException> {
            deserialize<SingleStringProp>("{}")
        }
    }

    @Test fun testListOfInts() {
        val result = deserialize<ListOfInts>("""{"ints": [42]}""")
        assertEquals(ListOfInts(listOf(42)), result)
    }

    @Test fun testObjectForListOfInts() {
        assertFailsWith<JKidException> {
            deserialize<ListOfInts>("""{"ints": {"a": 42}}""")
        }
    }
}

data class SingleStringProp(val s: String)

data class SingleNullableStringProp(val s: String?)

data class TwoIntProp(val i1: Int, val i2: Long)

data class TwoBoolProp(val b1: Boolean, val b2: Boolean)

data class SingleObjectProp(val o: SingleStringProp)

data class SingleListProp(val o: List<String?>)

data class SingleObjectListProp(val o: List<SingleStringProp>)

data class SingleOptionalProp(val s: String = "foo")

data class ListOfInts(val ints: List<Int>)

data class SingleAnnotatedStringProp(@JsonName("q") val s: String)

data class TwoPropsOneExcluded(val s: String, @JsonExclude val x: String = "")

class NumberSerializer: ValueSerializer<Int> {
    override fun fromJsonValue(jsonValue: Any?): Int = when(jsonValue) {
        "ZERO" -> 0
        "ONE" -> 1
        else -> throw JKidException("Unexpected value $jsonValue")
    }

    override fun toJsonValue(value: Int): Any? = when(value) {
        0 -> "ZERO"
        1 -> "ONE"
        else -> "?"
    }
}

data class SingleCustomSerializedProp(@CustomSerializer(NumberSerializer::class) val x: Int)

object TimestampSerializer : ValueSerializer<Date> {
    override fun toJsonValue(value: Date): Any? = value.time

    override fun fromJsonValue(jsonValue: Any?): Date
            = Date((jsonValue as Number).toLong())
}

data class SingleDateProp(@CustomSerializer(TimestampSerializer::class) val x: Date)

interface ValueIntf {
    val name: String
}

data class ValueImpl(override val name: String) : ValueIntf

data class ValueHolder(@DeserializeInterface(ValueImpl::class) val value: ValueIntf)

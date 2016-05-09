package ru.yole.jkid

import org.junit.Test
import java.io.StringReader
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeserializerTest {
    @Test fun testSimple() {
        val result = deserialize<SingleStringProp>(StringReader("""{"s": "x"}"""))
        assertEquals("x", result.s)
    }

    @Test fun testObject() {
        val result = deserialize<SingleObjectProp>(StringReader("{\"o\": {\"s\": \"x\"}}"))
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

    data class SingleStringProp(val s: String)

    data class SingleObjectProp(val o: SingleStringProp)

    data class SingleListProp(val o: List<String>)

    data class SingleObjectListProp(val o: List<SingleStringProp>)

    data class SingleOptionalProp(val s: String = "foo")

    data class SingleAnnotatedStringProp(@JsonName("q") val s: String)
}
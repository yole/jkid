package ru.yole.jkid

import org.junit.Test
import java.io.StringReader
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeserializerTest {
    @Test fun testSimple() {
        val result = deserialize<SingleStringProp>(StringReader("{\"s\": \"x\"}"))
        assertEquals("x", result.s)
    }

    @Test fun testObject() {
        val result = deserialize<SingleObjectProp>(StringReader("{\"o\": {\"s\": \"x\"}}"))
        assertEquals("x", result.o.s)
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
}
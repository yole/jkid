package ru.yole.jkid.deserialization

import org.junit.Test
import ru.yole.jkid.deserialization.ParserTest.JsonParseCallbackCall.*
import java.io.StringReader
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ParserTest {
    @Test fun testTrivial() {
        verifyParse("""{"s": "x"}""", VisitValue(0, "s", "x"))
    }

    @Test fun testTwoProperties() {
        verifyParse("""{"s": "x", "f": 1}""",
                VisitValue(0, "s", "x"),
                VisitValue(0, "f", 1L))
    }

    @Test fun testMissingComma() {
        verifyMalformed("""{"s": "x" "f": 1}""")
    }

    @Test fun testNestedObject() {
        verifyParse("""{"s": {"x": 1}}""",
                CreateObject(0, "s"),
                VisitValue(1, "x", 1L))
    }

    @Test fun testArray() {
        verifyParse("""{"s": [1, 2]}""",
                CreateArray(0, "s"),
                VisitValue(1, "s", 1L),
                VisitValue(1, "s", 2L))
    }

    @Test fun testArrayOfObjects() {
        verifyParse("""{"s": [{"x": 1}, {"x": 2}]}""",
                CreateArray(0, "s"),
                CreateObject(1, "s"),
                VisitValue(2, "x", 1L),
                CreateObject(1, "s"),
                VisitValue(3, "x", 2L))
    }

    private fun verifyParse(json: String, vararg expectedCallbackCalls: JsonParseCallbackCall) {
        val reportingCallback = ReportingParseCallback()
        Parser(StringReader(json), 0, reportingCallback).parse()
        assertEquals(expectedCallbackCalls.size, reportingCallback.calls.size)
        for ((expected, actual) in expectedCallbackCalls zip reportingCallback.calls) {
            assertEquals(expected, actual)
        }
    }

    private fun verifyMalformed(text: String) {
        assertFailsWith<MalformedJSONException> {
            Parser(StringReader(text), 0, ReportingParseCallback()).parse()
        }
    }

    interface JsonParseCallbackCall {
        data class CreateObject(val objId: Int, val propertyName: String) : JsonParseCallbackCall
        data class CreateArray(val objId: Int, val propertyName: String) : JsonParseCallbackCall
        data class VisitValue(val objId: Int, val propertyName: String, val value: Any?) : JsonParseCallbackCall
    }

    class ReportingParseCallback: JsonParseCallback<Int> {
        val calls = mutableListOf<JsonParseCallbackCall>()
        var lastObjectId: Int = 0

        override fun createObject(obj: Int, propertyName: String): Int {
            calls.add(CreateObject(obj, propertyName))
            return ++lastObjectId
        }

        override fun createArray(obj: Int, propertyName: String): Int {
            calls.add(CreateArray(obj, propertyName))
            return ++lastObjectId
        }

        override fun visitValue(obj: Int, propertyName: String, value: Any?) {
            calls.add(VisitValue(obj, propertyName, value))
        }
    }
}
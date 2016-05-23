package ru.yole.jkid

import org.junit.Test
import ru.yole.jkid.ParserTest.JsonParseCallbackCall.*
import java.io.StringReader
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ParserTest {
    @Test fun testTrivial() {
        verifyParse("""{"s": "x"}""", VisitProperty("s", "x"))
    }

    @Test fun testTwoProperties() {
        verifyParse("""{"s": "x", "f": 1}""",
                VisitProperty("s", "x"),
                VisitProperty("f", 1.0))
    }

    @Test fun testMissingComma() {
        verifyMalformed("""{"s": "x" "f": 1}""")
    }

    @Test fun testNestedObject() {
        verifyParse("""{"s": {"x": 1}}""",
                EnterObject("s"),
                VisitProperty("x", 1.0),
                LeaveObject)
    }

    @Test fun testArray() {
        verifyParse("""{"s": [1, 2]}""",
                EnterArray("s"),
                VisitProperty("s", 1.0),
                VisitProperty("s", 2.0),
                LeaveArray)
    }

    @Test fun testArrayOfObjects() {
        verifyParse("""{"s": [{"x": 1}, {"x": 2}]}""",
                EnterArray("s"),
                EnterObject("s"),
                VisitProperty("x", 1.0),
                LeaveObject,
                EnterObject("s"),
                VisitProperty("x", 2.0),
                LeaveObject,
                LeaveArray)
    }

    private fun verifyParse(json: String, vararg expectedCallbackCalls: JsonParseCallbackCall) {
        val reportingCallback = ReportingParseCallback()
        Parser(StringReader(json), reportingCallback).parse()
        assertEquals(expectedCallbackCalls.size, reportingCallback.calls.size)
        for ((expected, actual) in expectedCallbackCalls zip reportingCallback.calls) {
            assertEquals(expected, actual)
        }
    }

    private fun verifyMalformed(text: String) {
        assertFailsWith<MalformedJSONException> {
            Parser(StringReader(text), ReportingParseCallback()).parse()
        }
    }

    interface JsonParseCallbackCall {
        data class EnterObject(val propertyName: String) : JsonParseCallbackCall
        data class EnterArray(val propertyName: String) : JsonParseCallbackCall
        data class VisitProperty(val propertyName: String, val value: Any?) : JsonParseCallbackCall
        object LeaveObject : JsonParseCallbackCall
        object LeaveArray : JsonParseCallbackCall
    }

    class ReportingParseCallback: JsonParseCallback {
        val calls = mutableListOf<JsonParseCallbackCall>()

        override fun enterObject(propertyName: String) {
            calls.add(EnterObject(propertyName))
        }

        override fun leaveObject() {
            calls.add(LeaveObject)
        }

        override fun enterArray(propertyName: String) {
            calls.add(EnterArray(propertyName))
        }

        override fun leaveArray() {
            calls.add(LeaveArray)
        }

        override fun visitProperty(propertyName: String, value: Any?) {
            calls.add(VisitProperty(propertyName, value))
        }
    }
}
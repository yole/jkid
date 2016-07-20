package ru.yole.jkid.deserialization

import org.junit.Test
import ru.yole.jkid.deserialization.ParserTest.JsonParserAction.*
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
                CreateObject(1, "s"),
                VisitValue(1, "x", 1L))
    }

    @Test fun testArray() {
        verifyParse("""{"s": [1, 2]}""",
                CreateArray(1, "s"),
                VisitValue(1, "s", 1L),
                VisitValue(1, "s", 2L))
    }

    @Test fun testArrayOfObjects() {
        verifyParse("""{"s": [{"x": 1}, {"x": 2}]}""",
                CreateArray(1, "s"),
                CreateObject(2, "s"),
                VisitValue(2, "x", 1L),
                CreateObject(3, "s"),
                VisitValue(3, "x", 2L))
    }

    private fun verifyParse(json: String, vararg expectedCallbackCalls: JsonParserAction) {
        val reportingObject = ReportingJsonObject(0)
        Parser(StringReader(json), reportingObject).parse()
        assertEquals(expectedCallbackCalls.size, reportingObject.actions.size)
        for ((expected, actual) in expectedCallbackCalls zip reportingObject.actions) {
            assertEquals(expected, actual)
        }
    }

    private fun verifyMalformed(text: String) {
        assertFailsWith<MalformedJSONException> {
            Parser(StringReader(text), ReportingJsonObject(0)).parse()
        }
    }

    interface JsonParserAction {
        data class CreateObject(val objId: Int, val propertyName: String) : JsonParserAction
        data class CreateArray(val objId: Int, val propertyName: String) : JsonParserAction
        data class VisitValue(val objId: Int, val propertyName: String, val value: Any?) : JsonParserAction
    }

    class ReportingData(var maxId: Int, val actions: MutableList<JsonParserAction> = mutableListOf())

    class ReportingJsonObject(
            val id: Int,
            private val reportingData: ReportingData = ReportingData(0, mutableListOf())
    ) : JsonObject {
        val actions: List<JsonParserAction>
            get() = reportingData.actions

        override fun setSimpleProperty(propertyName: String, value: Any?) {
            reportingData.actions.add(VisitValue(id, propertyName, value))
        }

        override fun createObject(propertyName: String) = createCompositeProperty(propertyName, false)

        override fun createArray(propertyName: String) = createCompositeProperty(propertyName, true)

        private fun createCompositeProperty(propertyName: String, isCollection: Boolean): JsonObject {
            reportingData.maxId++
            val newId = reportingData.maxId
            reportingData.actions.add(if (isCollection) CreateArray(newId, propertyName) else CreateObject(newId, propertyName))
            return ReportingJsonObject(newId, reportingData)
        }
    }
}
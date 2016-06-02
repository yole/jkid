package ru.yole.jkid.examples.rule

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.yole.jkid.serialization.serialize
import kotlin.test.assertEquals

data class Person(val name: String, val age: Int)

class WriteJsonToFileTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test fun testUsingTempFolder() {
        val person = Person("Alice", 29)
        val json = """{"age": 29, "name": "Alice"}"""

        val jsonFile = folder.newFile("person.json")
        jsonFile.writeText(serialize(person))
        assertEquals(json, jsonFile.readText())
    }
}
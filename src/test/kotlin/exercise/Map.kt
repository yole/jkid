package exercise

import org.junit.Test
import ru.yole.jkid.deserialization.deserialize
import ru.yole.jkid.serialization.serialize
import kotlin.test.assertEquals

data class BookStore(val bookPrice: Map<String, Double>)

class MapTest {
    private val bookStore = BookStore(mapOf("Catch-22" to 10.92, "The Lord of the Rings" to 11.49))
    private val json = """{"bookPrice": {"Catch-22": 10.92, "The Lord of the Rings": 11.49}}"""

    @Test fun testSerialization() {
        println(serialize(bookStore))
        assertEquals(json, serialize(bookStore))
    }

    @Test fun testDeserialization() {
        assertEquals(bookStore, deserialize(json))
    }
}
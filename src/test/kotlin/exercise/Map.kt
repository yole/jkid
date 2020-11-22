package exercise

import org.junit.Ignore
import org.junit.Test
import ru.yole.jkid.deserialization.deserialize
import ru.yole.jkid.serialization.serialize
import kotlin.test.assertEquals

data class BookStore(val bookPrice: Map<String, Double>)
data class Book(val title: String, val price: Double)
data class TitleToBook(val books: Map<String, Book>)

@Ignore
class MapTest {
    private val bookStore = BookStore(mapOf("Catch-22" to 10.92, "The Lord of the Rings" to 11.49))
    private val json = """{"bookPrice": {"Catch-22": 10.92, "The Lord of the Rings": 11.49}}"""

    private val titleToBook = TitleToBook(
        mapOf(
            "Catch-22" to Book("Catch-22", 10.92),
            "The Lord of the Rings" to Book("The Lord of the Rings", 11.49)
        )
    )
    private val titleToBookJson = """{"books": {"Catch-22": {"price": 10.92, "title": "Catch-22"}, "The Lord of the Rings": {"price": 11.49, "title": "The Lord of the Rings"}}}"""

    @Test fun testSerialization() {
        println(serialize(bookStore))
        assertEquals(json, serialize(bookStore))
    }

    @Test fun testDeserialization() {
        assertEquals(bookStore, deserialize(json))
    }

    @Test fun testObjectMapSerialization() {
        assertEquals(titleToBookJson, serialize(titleToBook))
    }

    @Test fun testObjectMapDeserialization() {
        assertEquals(titleToBook, deserialize(titleToBookJson))
    }
}
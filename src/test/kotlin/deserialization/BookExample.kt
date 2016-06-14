package deserialization

import ru.yole.jkid.deserialization.deserialize

data class Author(val name: String)
data class Book(val title: String, val author: Author)

fun main(args: Array<String>) {
    val json = """{"title": "Catch-22", "author": {"name": "J. Heller"}}"""
    println(deserialize<Book>(json))
}

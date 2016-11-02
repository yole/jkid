package ru.yole.jkid.deserialization

import java.io.Reader

interface Token {
    object COMMA : Token
    object COLON : Token
    object LBRACE : Token
    object RBRACE : Token
    object LBRACKET : Token
    object RBRACKET : Token

    interface ValueToken : Token {
        val value: Any?
    }

    object NullValue : ValueToken {
        override val value: Any?
            get() = null
    }

    data class BoolValue(override val value: Boolean) : ValueToken
    data class StringValue(override val value: String) : ValueToken
    data class LongValue(override val value: Long) : ValueToken
    data class DoubleValue(override val value: Double) : ValueToken

    companion object {
        val TRUE = BoolValue(true)
        val FALSE = BoolValue(false)
    }
}

class MalformedJSONException(message: String): Exception(message)

internal class CharReader(val reader: Reader) {
    private val tokenBuffer = CharArray(4)
    private var nextChar: Char? = null
    var eof = false
        private set

    private fun advance() {
        if (eof) return
        val c = reader.read()
        if (c == -1) {
            eof = true
        }
        else {
            nextChar = c.toChar()
        }
    }

    fun peekNext(): Char? {
        if (nextChar == null) {
            advance()
        }
        return if (eof) null else nextChar
    }

    fun readNext() = peekNext().apply { nextChar = null }

    fun readNextChars(length: Int): String {
        assert(nextChar == null)
        assert(length <= tokenBuffer.size)
        if (reader.read(tokenBuffer, 0, length) != length) {
            throw MalformedJSONException("Premature end of data")
        }
        return String(tokenBuffer, 0, length)
    }

    fun expectText(text: String, followedBy: Set<Char>) {
        if (readNextChars(text.length) != text) {
            throw MalformedJSONException("Expected text $text")
        }
        val next = peekNext()
        if (next != null && next !in followedBy)
            throw MalformedJSONException("Expected text in $followedBy")
    }
}

class Lexer(reader: Reader) {
    private val charReader = CharReader(reader)

    companion object {
        private val valueEndChars = setOf(',', '}', ']', ' ', '\t', '\r', '\n')
    }

    private val tokenMap = hashMapOf<Char, (Char) -> Token> (
        ',' to { c -> Token.COMMA },
        '{' to { c -> Token.LBRACE },
        '}' to { c -> Token.RBRACE },
        '[' to { c -> Token.LBRACKET },
        ']' to { c -> Token.RBRACKET },
        ':' to { c -> Token.COLON },
        't' to { c -> charReader.expectText("rue", valueEndChars); Token.TRUE },
        'f' to { c -> charReader.expectText("alse", valueEndChars); Token.FALSE },
        'n' to { c -> charReader.expectText("ull", valueEndChars); Token.NullValue },
        '"' to { c -> readStringToken() },
        '-' to { c -> readNumberToken(c) }
    ).apply {
        for (i in '0'..'9') {
            this[i] = { c -> readNumberToken(c) }
        }
    }

    fun nextToken(): Token? {
        var c: Char?
        do {
            c = charReader.readNext()
        } while (c != null && c.isWhitespace())
        if (c == null) return null

        return tokenMap[c]?.invoke(c)
                ?: throw MalformedJSONException("Unexpected token $c")
    }

    private fun readStringToken(): Token {
        val result = StringBuilder()
        while (true) {
            val c = charReader.readNext() ?: throw MalformedJSONException("Unterminated string")
            if (c == '"') break
            if (c == '\\') {
                val escaped = charReader.readNext() ?: throw MalformedJSONException("Unterminated escape sequence")
                when(escaped) {
                    '\\', '/', '\"' -> result.append(escaped)
                    'b' -> result.append('\b')
                    'f' -> result.append('\u000C')
                    'n' -> result.append('\n')
                    'r' -> result.append('\r')
                    't' -> result.append('\t')
                    'u' -> {
                        val hexChars = charReader.readNextChars(4)
                        result.append(Integer.parseInt(hexChars, 16).toChar())
                    }
                    else -> throw MalformedJSONException("Unsupported escape sequence \\$escaped")
                }
            }
            else {
                result.append(c)
            }
        }
        return Token.StringValue(result.toString())
    }

    private fun readNumberToken(firstChar: Char): Token {
        val buffer = StringBuilder(firstChar.toString())
        while (true) {
            val c = charReader.peekNext()
            if (c == null || c in valueEndChars) break
            buffer.append(charReader.readNext()!!)
        }
        val value = buffer.toString()
        return if (value.contains(".")) Token.DoubleValue(value.toDouble()) else Token.LongValue(value.toLong())
    }
}
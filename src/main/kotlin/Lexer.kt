package ru.yole.jkid

import java.io.Reader

interface Token {
    object COMMA : Token
    object LBRACE : Token
    object RBRACE : Token
    object LBRACKET : Token
    object RBRACKET : Token
    object COLON : Token

    interface ValueToken : Token {
        val value: Any?
    }

    object NullValue : ValueToken {
        override val value: Any?
            get() = null
    }

    data class BoolValue(override val value: Boolean) : ValueToken
    data class StringValue(override val value: String) : ValueToken
    data class NumberValue(override val value: Double) : ValueToken

    companion object {
        val TRUE = BoolValue(true)
        val FALSE = BoolValue(false)
    }
}

class MalformedJSONException(message: String): Exception(message)

class CharReader(val reader: Reader) {
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

    fun expectText(text: String, followedBy: Set<Char>) {
        assert(nextChar == null)
        if (reader.read(tokenBuffer, 0, text.length) != text.length ||
                String(tokenBuffer, 0, text.length) != text) {
            throw MalformedJSONException("Expected text $text")
        }
        val next = peekNext()
        if (next != null && next !in followedBy)
            throw MalformedJSONException("Expected text in $followedBy")
    }
}

class Lexer(reader: Reader) {
    private val charReader = CharReader(reader)

    val valueEndChars = setOf(',', '}', ']', ' ', '\t', '\r', '\n')
    val tokenMap = hashMapOf<Char, (Char) -> Token> (
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

    fun readStringToken(): Token {
        val result = StringBuilder()
        while (true) {
            val c = charReader.readNext() ?: throw MalformedJSONException("Unterminated string")
            if (c == '"') break
            result.append(c)
        }
        return Token.StringValue(result.toString())
    }

    fun readNumberToken(firstChar: Char): Token {
        val buffer = StringBuilder(firstChar.toString())
        while (true) {
            val c = charReader.peekNext()
            if (c == null || c in valueEndChars) break
            buffer.append(charReader.readNext())
        }
        return Token.NumberValue(java.lang.Double.parseDouble(buffer.toString()))
    }

    fun nextToken(): Token? {
        var c: Char?
        do {
            c = charReader.readNext()
        } while (c != null && c.isWhitespace())
        if (c == null) return null

        return tokenMap[c.toChar()]?.invoke(c)
                ?: throw MalformedJSONException("Unexpected token $c")
    }
}
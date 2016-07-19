package ru.yole.jkid.deserialization

import java.io.Reader

interface JsonParseCallback<T> {
    fun createObject(obj: T, propertyName: String): T
    fun createArray(obj: T, propertyName: String): T
    fun visitValue(obj: T, propertyName: String, value: Any?)
}

class Parser<T>(reader: Reader, val root: T, val callback: JsonParseCallback<T>) {
    private val lexer = Lexer(reader)

    fun parse() {
        expect(Token.LBRACE)
        parseObjectBody(root)
        if (lexer.nextToken() != null) {
            throw IllegalArgumentException("Too many tokens")
        }
    }

    private fun parseObjectBody(obj: T) {
        parseCommaSeparated(Token.RBRACE) { token ->
            if (token !is Token.StringValue) {
                throw MalformedJSONException("Unexpected token $token")
            }

            val propName = token.value
            expect(Token.COLON)
            parsePropertyValue(obj, propName, nextToken())
        }
    }

    private fun parseArrayBody(obj: T, propName: String) {
        parseCommaSeparated(Token.RBRACKET) { token ->
            parsePropertyValue(obj, propName, token)
        }
    }

    private fun parseCommaSeparated(stopToken: Token, body: (Token) -> Unit) {
        var expectComma = false
        while (true) {
            var token = nextToken()
            if (token == stopToken) return
            if (expectComma) {
                if (token != Token.COMMA) throw MalformedJSONException("Expected comma")
                token = nextToken()
            }

            body(token)

            expectComma = true
        }
    }

    private fun parsePropertyValue(obj: T, propName: String, token: Token) {
        when (token) {
            is Token.ValueToken ->
                callback.visitValue(obj, propName, token.value)

            Token.LBRACE -> {
                val childObj = callback.createObject(obj, propName)
                parseObjectBody(childObj)
            }

            Token.LBRACKET -> {
                val childObj = callback.createArray(obj, propName)
                parseArrayBody(childObj, propName)
            }

            else ->
                throw MalformedJSONException("Unexpected token $token")
        }
    }

    private fun expect(token: Token) {
        if (lexer.nextToken() != token) {
            throw IllegalArgumentException("$token expected")
        }
    }

    private fun nextToken(): Token = lexer.nextToken() ?: throw IllegalArgumentException("Premature end of data")
}

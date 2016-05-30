package ru.yole.jkid.deserialization

import java.io.Reader

interface JsonParseCallback {
    fun enterObject(propertyName: String)
    fun leaveObject()
    fun enterArray(propertyName: String)
    fun leaveArray()
    fun visitValue(propertyName: String, value: Any?)
}

class Parser(reader: Reader, val callback: JsonParseCallback) {
    private val lexer = Lexer(reader)

    fun parse() {
        expect(Token.LBRACE)
        parseObjectBody()
        if (lexer.nextToken() != null) {
            throw IllegalArgumentException("Too many tokens")
        }
    }

    private fun parseObjectBody() {
        parseCommaSeparated(Token.RBRACE) { token ->
            if (token !is Token.StringValue) {
                throw MalformedJSONException("Unexpected token $token")
            }

            val propName = token.value
            expect(Token.COLON)
            parsePropertyValue(propName, nextToken())
        }
    }

    private fun parseArrayBody(propName: String) {
        parseCommaSeparated(Token.RBRACKET) { token ->
            parsePropertyValue(propName, token)
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

    private fun parsePropertyValue(propName: String, token: Token) {
        when (token) {
            is Token.ValueToken ->
                callback.visitValue(propName, token.value)

            Token.LBRACE -> {
                callback.enterObject(propName)
                parseObjectBody()
                callback.leaveObject()
            }

            Token.LBRACKET -> {
                callback.enterArray(propName)
                parseArrayBody(propName)
                callback.leaveArray()
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

package ru.yole.jkid

import java.io.Reader

interface JsonParseCallback {
    fun enterObject(propertyName: String)
    fun leaveObject()
    fun enterArray(propertyName: String)
    fun visitArrayElement(value: Token.ValueToken)
    fun leaveArray()
    fun visitProperty(propertyName: String, value: Token.ValueToken)
}

class Parser(val reader: Reader, val callback: JsonParseCallback) {
    private val lexer = Lexer(reader)

    fun parse() {
        expect(Token.LBRACE)
        parseObjectBody()
        if (lexer.nextToken() != null) {
            throw IllegalArgumentException("Too many tokens")
        }
    }

    private fun parseObjectBody() {
        var expectComma = false
        while (true) {
            var token = nextToken()
            if (token == Token.RBRACE) return
            if (expectComma) {
                if (token != Token.COMMA) throw MalformedJSONException("Expected comma")
                token = nextToken()
            }
            if (token !is Token.StringValue) {
                throw MalformedJSONException("Unxpected token $token")
            }

            val propName = token.value
            expect(Token.COLON)
            val valueStart = nextToken()
            when (valueStart) {
                is Token.ValueToken ->
                    callback.visitProperty(propName, valueStart)

                Token.LBRACE -> {
                    callback.enterObject(propName)
                    parseObjectBody()
                    callback.leaveObject()
                }

                Token.LBRACKET -> {
                    callback.enterArray(propName)
                    parseArrayBody()
                    callback.leaveArray()
                }

                else ->
                    throw MalformedJSONException("Unexpected token $token")
            }
            expectComma = true
        }
    }

    private fun parseArrayBody() {
        var expectComma = false
        while (true) {
            var token = nextToken()
            if (token == Token.RBRACKET) return
            if (expectComma) {
                if (token != Token.COMMA) throw MalformedJSONException("Expected comma")
                token = nextToken()
            }

            if (token !is Token.ValueToken) {
                throw MalformedJSONException("Unexpected token $token")
            }
            callback.visitArrayElement(token)
            expectComma = true
        }
    }

    private fun expect(token: Token) {
        if (lexer.nextToken() != token) {
            throw IllegalArgumentException("$token expected")
        }
    }

    private fun nextToken(): Token = lexer.nextToken() ?: throw IllegalArgumentException("Premature end of dat")
}

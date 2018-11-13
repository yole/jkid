package ru.yole.jkid.deserialization

import java.io.Reader

class Parser(reader: Reader, val rootObject: JsonObject) {
    private val lexer = Lexer(reader)

    fun parse() {
        expect(Token.LBRACE)
        parseObjectBody(rootObject)
        if (lexer.nextToken() != null) {
            throw IllegalArgumentException("Too many tokens")
        }
    }

    private fun parseObjectBody(jsonObject: JsonObject) {
        parseCommaSeparated(Token.RBRACE) { token ->
            if (token !is Token.StringValue) {
                throw MalformedJSONException("Unexpected token $token")
            }

            val propName = token.value
            expect(Token.COLON)
            parsePropertyValue(jsonObject, propName, nextToken())
        }
    }

    private fun parseArrayBody(currentObject: JsonObject, propName: String) {
        parseCommaSeparated(Token.RBRACKET) { token ->
            parsePropertyValue(currentObject, propName, token)
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

    private fun parsePropertyValue(currentObject: JsonObject, propName: String, token: Token) {
        when (token) {
            is Token.ValueToken ->
                currentObject.setSimpleProperty(propName, token.value)

            Token.LBRACE -> {
                val childObj = currentObject.createObject(propName)
                parseObjectBody(childObj)
            }

            Token.LBRACKET -> {
                val childObj = currentObject.createArray(propName)
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

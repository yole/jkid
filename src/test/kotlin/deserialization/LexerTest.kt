package ru.yole.jkid.deserialization

import org.junit.Test
import java.io.StringReader
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class LexerTest {
    @Test fun testTrivial() {
        verifyTokens(",", Token.COMMA)
    }

    @Test fun testWhitespace() {
        verifyTokens("  , \t", Token.COMMA)
    }

    @Test fun testAllSingleChars() {
        verifyTokens("[]{}:,", Token.LBRACKET, Token.RBRACKET, Token.LBRACE, Token.RBRACE, Token.COLON, Token.COMMA)
    }

    @Test fun testBoolean() {
        verifyTokens("true", Token.TRUE)
        verifyTokens("false", Token.FALSE)
    }

    @Test fun testNull() {
        verifyTokens("null", Token.NullValue)
    }

    @Test fun testEscapeSequences() {
        verifyTokens(""""\\"""", Token.StringValue("\\"))
        verifyTokens(""""\""""", Token.StringValue("\""))
        verifyTokens(""""\/"""", Token.StringValue("/"))
        verifyTokens(""""\n"""", Token.StringValue("\n"))
        verifyTokens(""""\u0041"""", Token.StringValue("A"))
    }


    @Test fun testNullMalformed() {
        verifyMalformed("nulll")
    }

    @Test fun testString() {
        verifyTokens("\"abc\"", Token.StringValue("abc"))
    }

    @Test fun testDouble() {
        verifyTokens("0", Token.NumberValue(0.0))
    }

    @Test fun testNegativeDouble() {
        verifyTokens("-1", Token.NumberValue(-1.0))
    }

    private fun verifyTokens(text: String, vararg tokens: Token) {
        val lexer = Lexer(StringReader(text))
        for (expectedToken in tokens) {
            assertEquals(expectedToken, lexer.nextToken())
        }
        assertNull(lexer.nextToken(), "Too many tokens")
    }

    private fun verifyMalformed(text: String) {
        assertFailsWith<MalformedJSONException> {
            Lexer(StringReader(text)).nextToken()
        }
    }
}

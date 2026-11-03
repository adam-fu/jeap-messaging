package ch.admin.bit.jeap.messaging.registry.verifier;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvroIdlTokenizerTest {

    @Test
    void lineAndBlockCommentsAreIgnored() throws IOException {
        String withComments = """
                // leading comment
                /** Doc comment */
                record TestEvent { /* inline */ string id; } // trailing comment""";
        String withoutComments = "record TestEvent { string id; }";

        assertThat(AvroIdlTokenizer.tokenize(withComments)).isEqualTo(AvroIdlTokenizer.tokenize(withoutComments));
    }

    @Test
    void commentsSeparateTokens() throws IOException {
        String commentBetweenTokens = "record TestEvent { string/* comment */origin; }";

        assertThat(AvroIdlTokenizer.tokenize(commentBetweenTokens))
                .isEqualTo(AvroIdlTokenizer.tokenize("record TestEvent { string origin; }"))
                .isNotEqualTo(AvroIdlTokenizer.tokenize("record TestEvent { stringorigin; }"));
    }

    @Test
    void tokenBoundariesArePreserved() throws IOException {
        assertThat(AvroIdlTokenizer.tokenize("string a;"))
                .isNotEqualTo(AvroIdlTokenizer.tokenize("stringa;"));
    }

    @Test
    void optionalWhitespaceAndLineBreaksDoNotMatter() throws IOException {
        String compact = "protocol P{record TestEvent{string id;}}";
        String spaced = """
                protocol P {
                    record TestEvent {
                        string id;
                    }
                }
                """;

        assertThat(AvroIdlTokenizer.tokenize(compact)).isEqualTo(AvroIdlTokenizer.tokenize(spaced));
    }

    @Test
    void whitespaceInsideStringLiteralsIsSignificant() throws IOException {
        String twoSpaces = "record TestEvent { string text = \"a  b\"; }";
        String oneSpace = "record TestEvent { string text = \"a b\"; }";

        assertThat(AvroIdlTokenizer.tokenize(twoSpaces)).isNotEqualTo(AvroIdlTokenizer.tokenize(oneSpace));
    }

    @Test
    void commentMarkersInsideStringLiteralsAreKept() throws IOException {
        String idl = "record TestEvent { string url = \"https://example.com/*not-a-comment*/\"; }";

        assertThat(AvroIdlTokenizer.tokenize(idl))
                .contains("\"https://example.com/*not-a-comment*/\"")
                .isNotEqualTo(AvroIdlTokenizer.tokenize(
                        "record TestEvent { string url = \"https://example.com\"; }"));
    }

    @Test
    void escapedQuotesInStringLiteralsAreHandled() throws IOException {
        String literal = "\"a \\\" // b\"";

        assertThat(AvroIdlTokenizer.tokenize(
                "record TestEvent { string text = " + literal + "; string other; }"))
                .contains(literal)
                .isEqualTo(AvroIdlTokenizer.tokenize(
                        "record TestEvent {\n  string text = " + literal + ";\n  string other;\n}"));
    }

    @Test
    void lineCommentEndsAtCarriageReturn() throws IOException {
        String carriageReturnOnly = "// comment\rrecord TestEvent { string id; }";

        assertThat(AvroIdlTokenizer.tokenize(carriageReturnOnly))
                .isEqualTo(AvroIdlTokenizer.tokenize("record TestEvent { string id; }"));
    }

    @Test
    void fieldAddedAfterCarriageReturnCommentIsDetected() throws IOException {
        String carriageReturnOnly = "// comment\rrecord TestEvent { string id; string added; }";

        assertThat(AvroIdlTokenizer.tokenize(carriageReturnOnly))
                .isNotEqualTo(AvroIdlTokenizer.tokenize("record TestEvent { string id; }"));
    }

    @Test
    void unterminatedBlockCommentIsRejected() {
        String idl = "record TestEvent { string id; } /* unterminated comment";

        assertThatThrownBy(() -> AvroIdlTokenizer.tokenize(idl))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Unterminated block comment");
    }

    @Test
    void unterminatedStringLiteralIsRejected() {
        String idl = "record TestEvent { string id = \"unterminated; }";

        assertThatThrownBy(() -> AvroIdlTokenizer.tokenize(idl))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Unterminated string literal");
    }

    @Test
    void stripCommentsKeepsSurroundingCode() throws IOException {
        String idl = "record TestEvent { // comment\n string/* comment */origin; /* trailing */\n}";

        assertThat(AvroIdlTokenizer.stripComments(idl))
                .isEqualTo("record TestEvent { \n string origin;  \n}");
    }

    @Test
    void stripCommentsKeepsLineStructureOfBlockComments() throws IOException {
        String idl = "record TestEvent {\n/* first\n   second */\nstring id;\n}";

        assertThat(AvroIdlTokenizer.stripComments(idl))
                .isEqualTo("record TestEvent {\n \n\nstring id;\n}");
    }
}

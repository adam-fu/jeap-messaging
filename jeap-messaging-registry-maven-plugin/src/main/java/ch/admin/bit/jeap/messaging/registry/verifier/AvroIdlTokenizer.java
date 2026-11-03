package ch.admin.bit.jeap.messaging.registry.verifier;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Lexical helper for Avro IDL (.avdl) files.
 * <p>
 * {@link #tokenize(File)} splits the content into tokens for comparison purposes. Comments are skipped and
 * whitespace is not part of any token, so two files compare as equal when they only differ in comments or
 * formatting. Token boundaries and string literals are kept intact, so any change affecting the schema
 * definition (types, names, fields, default values, annotations, imports, ...) is still detected:
 * {@code string origin;} and {@code stringorigin;} produce different tokens, and the content of a string
 * literal - including its whitespace - is part of a single token.
 * <p>
 * {@link #stripComments(String)} removes comments while keeping the remaining code and the line structure,
 * which is used for line oriented analyses of schema files.
 */
public final class AvroIdlTokenizer {

    private AvroIdlTokenizer() {
    }

    static List<String> tokenize(File idlFile) throws IOException {
        return tokenize(readIdlFile(idlFile));
    }

    static List<String> tokenize(String idl) throws IOException {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        while (i < idl.length()) {
            char c = idl.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '"') {
                int end = indexAfterStringLiteral(idl, i);
                tokens.add(idl.substring(i, end));
                i = end;
            } else if (idl.startsWith("//", i)) {
                // A comment separates tokens just like whitespace does.
                i = indexOfLineCommentEnd(idl, i);
            } else if (idl.startsWith("/*", i)) {
                i = indexAfterBlockComment(idl, i);
            } else if (isNameChar(c)) {
                int start = i;
                while (i < idl.length() && isNameChar(idl.charAt(i))) {
                    i++;
                }
                tokens.add(idl.substring(start, i));
            } else {
                tokens.add(String.valueOf(c));
                i++;
            }
        }
        return tokens;
    }

    /**
     * Removes {@code //} and {@code /* *}{@code /} comments but keeps the surrounding code and the line
     * structure, so that line oriented analyses are not affected by comments. String literals are preserved
     * unchanged, i.e. comment markers within a literal are not treated as a comment.
     */
    public static String stripComments(File idlFile) throws IOException {
        return stripComments(readIdlFile(idlFile));
    }

    public static String stripComments(String idl) throws IOException {
        StringBuilder stripped = new StringBuilder(idl.length());
        int i = 0;
        while (i < idl.length()) {
            char c = idl.charAt(i);
            if (c == '"') {
                int end = indexAfterStringLiteral(idl, i);
                stripped.append(idl, i, end);
                i = end;
            } else if (idl.startsWith("//", i)) {
                int end = indexOfLineCommentEnd(idl, i);
                if (end < idl.length()) {
                    stripped.append(idl.charAt(end));
                    end++;
                }
                i = end;
            } else if (idl.startsWith("/*", i)) {
                int end = indexAfterBlockComment(idl, i);
                stripped.append(' ').append(lineBreaks(idl, i, end));
                i = end;
            } else {
                stripped.append(c);
                i++;
            }
        }
        return stripped.toString();
    }

    /**
     * Reads an IDL file decoding leniently: existing schema files may contain bytes that are not valid UTF-8
     * (e.g. a Latin-1 umlaut in a comment), which must not break the comparison.
     */
    private static String readIdlFile(File idlFile) throws IOException {
        return new String(Files.readAllBytes(idlFile.toPath()), StandardCharsets.UTF_8);
    }

    /**
     * Returns the index right after the string literal starting at the given index. String literals may
     * contain comment markers (e.g. a default value with a URL), so their content must not be interpreted.
     */
    private static int indexAfterStringLiteral(String idl, int start) throws IOException {
        int i = start + 1;
        while (i < idl.length()) {
            char c = idl.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == '"') {
                return i + 1;
            } else {
                i++;
            }
        }
        throw new IOException("Unterminated string literal at position " + start);
    }

    /**
     * Returns the index of the line terminator ending the comment, or the end of the input.
     */
    private static int indexOfLineCommentEnd(String idl, int start) {
        int i = start + 2;
        while (i < idl.length()) {
            char c = idl.charAt(i);
            if (c == '\n' || c == '\r') {
                return i;
            }
            i++;
        }
        return idl.length();
    }

    private static int indexAfterBlockComment(String idl, int start) throws IOException {
        int endOfComment = idl.indexOf("*/", start + 2);
        if (endOfComment < 0) {
            // Consuming an unterminated comment would let a broken file compare equal to a valid one
            throw new IOException("Unterminated block comment at position " + start);
        }
        return endOfComment + 2;
    }

    private static String lineBreaks(String idl, int from, int to) {
        StringBuilder lineBreaks = new StringBuilder();
        for (int i = from; i < to; i++) {
            char c = idl.charAt(i);
            if (c == '\n' || c == '\r') {
                lineBreaks.append('\n');
            }
        }
        return lineBreaks.toString();
    }

    private static boolean isNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}

/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openidm.util;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.BitSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Additional HTTP Header utilities, not provided by {@link org.forgerock.http.header.HeaderUtil}.
 */
public class HeaderUtil {

    /**
     * Regular expression for detecting and parsing RFC 5987 header-values.
     * <p>
     * Three capturing groups are defined,
     * <ol>
     * <li>Character-set ({@code UTF-8} or {@code ISO-8859-1})</li>
     * <li>Language-tag (e.g., {@code en}) or {@code null} if not defined</li>
     * <li>Percent encoded text</li>
     * </ol>
     *
     * @see <a href="https://tools.ietf.org/html/rfc5987#section-3.2.1">RFC 5987 section 3.2.1</a>
     */
    private static final Pattern RFC_5987_PATTERN =
            Pattern.compile("^(UTF-8|ISO-8859-1)'([a-zA-Z0-9-]{2,})?'([^']+)$", Pattern.CASE_INSENSITIVE);

    /**
     * Non-safe characters are escaped as UTF-8 octets using "%" HEXDIG HEXDIG production.
     */
    private static final char URL_ESCAPE_CHAR = '%';

    /**
     * Look up table for characters which do not need URL encoding in path elements according to RFC 3986.
     */
    private static final BitSet SAFE_URL_PCHAR_CHARS = new BitSet(128);

    static {
        /*
         * pchar       = unreserved / pct-encoded / sub-delims / ":" / "@"
         *
         * pct-encoded = "%" HEXDIG HEXDIG
         * unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~"
         * sub-delims  = "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
         */
        for (char c : "-._~!$&'()*+,;=:@".toCharArray()) {
            SAFE_URL_PCHAR_CHARS.set(c);
        }
        SAFE_URL_PCHAR_CHARS.set('0', '9' + 1);
        SAFE_URL_PCHAR_CHARS.set('a', 'z' + 1);
        SAFE_URL_PCHAR_CHARS.set('A', 'Z' + 1);
    }

    private HeaderUtil() {
        // hidden constructor
    }

    /**
     * Detects and decodes a header-value, if RFC 5987 encoding is detected, or otherwise passes the value through
     * unchanged. RFC 5987 provides a means to define a character-set ({@code UTF-8} or {@code ISO-8859-1}) and optional
     * language-tag used in a header-value. This makes it possible, for example, to safely use UTF-8 characters within
     * header-values.
     *
     * @param value Header value or {@code null}
     * @return RFC 5987 decoded header-value or the value passed-through unchanged
     * @throws UnsupportedEncodingException when encoded character-set is unsupported
     * @throws IllegalArgumentException when header-value is malformed according to RFC 5987
     * @see <a href="https://tools.ietf.org/html/rfc5987#section-3.2.1">RFC 5987 section 3.2.1</a>
     */
    public static String decodeRfc5987(final String value)
            throws UnsupportedEncodingException, IllegalArgumentException {
        if (value != null) {
            final Matcher m = RFC_5987_PATTERN.matcher(value);
            if (m.matches()) {
                // NOTE: language-tag is ignored here, because it does not impact decoding
                final String charset = m.group(1);
                final String pctEncodedText = m.group(3);
                return decodeUriPathSegment(pctEncodedText, charset);
            }
        }
        return value;
    }

    private static String decodeUriPathSegment(final String s, final String charset)
            throws UnsupportedEncodingException {
        final byte[] inputBytes = s.getBytes(US_ASCII);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(inputBytes.length);
        int i = 0;
        while (i < inputBytes.length) {
            final int b = inputBytes[i++] & 0xFF;
            if (b == URL_ESCAPE_CHAR) {
                // decode percent-encoded pair of alpha-numeric characters
                if (i >= inputBytes.length) {
                    throw new IllegalArgumentException("Unexpected % at end of string");
                }
                final int high = alphaNumericByteToHexByte(inputBytes[i++] & 0xFF);
                if (high == -1) {
                    throw new IllegalArgumentException("Malformed percent encoding at position " + (i - 1));
                }
                if (i >= inputBytes.length) {
                    throw new IllegalArgumentException("Incomplete percent encoding at end of string");
                }
                final int low = alphaNumericByteToHexByte(inputBytes[i++] & 0xFF);
                if (low == -1) {
                    throw new IllegalArgumentException("Malformed percent encoding at position " + (i - 1));
                }
                outputStream.write((high << 4) + low);
            } else if (SAFE_URL_PCHAR_CHARS.get(b)) {
                outputStream.write(b);
            } else {
                throw new IllegalArgumentException("Unexpected character at position " + (i - 1));
            }
        }
        return outputStream.toString(charset);
    }

    private static int alphaNumericByteToHexByte(final int b) {
        if (b >= '0' && b <= '9') {
            return b - '0';
        } else if (b >= 'A' && b <= 'Z') {
            return 10 + b - 'A';
        } else if (b >= 'a' && b <= 'z') {
            return 10 + b - 'a';
        }
        // not an alpha-numeric ASCII character
        return -1;
    }

}

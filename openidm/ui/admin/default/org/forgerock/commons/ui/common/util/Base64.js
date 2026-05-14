"use strict";

/**
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
 * Copyright 2014-2016 ForgeRock AS.
 */

define([], function () {

    var obj = {},
        ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",
        B64CODES = [],
        EQUALS = 64,
        // index of '=' in ALPHABET
    i;

    // Initialise reverse lookup table from base64 char code to 6-bit sequence
    for (i = 0; i < 256; i++) {
        B64CODES.push(-1);
    }
    for (i = 0; i < ALPHABET.length; i++) {
        B64CODES[ALPHABET.charCodeAt(i)] = i;
    }
    B64CODES["=".charCodeAt(0)] = 0;

    /**
     * Encodes a string as base-64 encoded UTF-8 bytes.
     *
     * @param str the string to encode
     * @returns {string} the base-64 encoded UTF-8 bytes of the string.
     */
    obj.encodeUTF8 = function (str) {
        // See: http://ecmanaut.blogspot.co.uk/2006/07/encoding-decoding-utf8-in-javascript.html
        var utf = unescape(encodeURIComponent(str)),
            len = utf.length,
            result = [],
            i,
            c0,
            c1,
            c2,
            b0,
            b1,
            b2,
            b3;

        if (typeof btoa === "function") {
            return btoa(utf);
        } else {
            // Fallback on own implementation of b64 encoding if btoa not available.
            for (i = 0; i < len;) {
                // Encode 3 bytes at a time
                c0 = utf.charCodeAt(i++);
                c1 = utf.charCodeAt(i++);
                c2 = utf.charCodeAt(i++);
                // Split into 6-bit chunks (pad with =/64 if not enough bytes):
                b0 = c0 >>> 2;
                b1 = (c0 & 0x03) << 4 | c1 >>> 4;
                b2 = isNaN(c1) ? EQUALS : (c1 & 0x0F) << 2 | c2 >>> 6;
                b3 = isNaN(c1) || isNaN(c2) ? EQUALS : c2 & 0x3F;

                result.push(ALPHABET.charAt(b0));
                result.push(ALPHABET.charAt(b1));
                result.push(ALPHABET.charAt(b2));
                result.push(ALPHABET.charAt(b3));
            }

            return result.join("");
        }
    };

    /**
     * Decodes a base-64 encoded UTF-8 byte array into a string.
     * @param encoded the base-64 encoded UTF-8 bytes.
     * @returns {string} the decoded string.
     */
    obj.decodeUTF8 = function (encoded) {
        var utf = [],
            len = encoded.length,
            i,
            b0,
            b1,
            b2,
            b3,
            c0,
            c1,
            c2;
        if (typeof atob === "function") {
            utf = atob(encoded);
        } else {

            for (i = 0; i < len;) {
                // Decode 4-char blocks into 3 bytes
                b0 = B64CODES[encoded.charCodeAt(i++)];
                b1 = B64CODES[encoded.charCodeAt(i++)];
                b2 = B64CODES[encoded.charCodeAt(i++)];
                b3 = B64CODES[encoded.charCodeAt(i++)];

                c0 = b0 << 2 | b1 >>> 4;
                c1 = (b1 & 0x0F) << 4 | b2 >>> 2;
                c2 = (b2 & 0x03) << 6 | b3;

                utf.push(String.fromCharCode(c0));
                if (c1 !== 0) {
                    utf.push(String.fromCharCode(c1));
                }
                if (c2 !== 0) {
                    utf.push(String.fromCharCode(c2));
                }
            }

            utf = utf.join("");
        }

        // See: http://ecmanaut.blogspot.co.uk/2006/07/encoding-decoding-utf8-in-javascript.html
        return decodeURIComponent(escape(utf));
    };

    return obj;
});

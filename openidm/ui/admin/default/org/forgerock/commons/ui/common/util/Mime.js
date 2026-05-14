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

define(["org/forgerock/commons/ui/common/util/Base64"], function (base64) {

    var obj = {};

    /**
     * Encodes a header value as MIME base-64 encoded UTF-8 as per RFC 2047. This allows passing
     * Unicode characters beyond ASCII in a header value (which are limited to US-ASCII or ISO-8859-1).
     *
     * @param headerValue the header value to encode.
     * @returns {string} the base-64 encoded, UTF-8 MIME "text" token encoding of the header value.
     */
    obj.encodeHeader = function (headerValue) {
        return "=?UTF-8?B?" + base64.encodeUTF8(headerValue) + "?=";
    };

    return obj;
});

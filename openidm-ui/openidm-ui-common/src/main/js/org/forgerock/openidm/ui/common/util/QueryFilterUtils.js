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
 * Copyright 2026 3A Systems, LLC.
 */

define([], function () {
    var obj = {};

    /**
     * Escapes a value so it can be embedded as a double-quoted string literal inside a CREST
     * query filter, e.g. <code>userName eq "&lt;value&gt;"</code>. The UI counterpart of the
     * server-side <code>auth/queryFilter.escapeStringValue</code>.
     *
     * Inside such a literal only the backslash and the double quote are significant. The backslash
     * is escaped first, so a value ending in a backslash cannot escape the closing quote.
     *
     * The result is filter text, not URL text: a filter sent in a query string still has to go
     * through encodeURIComponent.
     *
     * @param {*} value the raw value (coerced to a string)
     * @returns {string} the value with backslash and double-quote characters backslash-escaped
     */
    obj.escapeStringValue = function (value) {
        return String(value).replace(/\\/g, "\\\\").replace(/"/g, "\\\"");
    };

    return obj;
});

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
 * Copyright 2015 ForgeRock AS.
 */

/*global require, define*/
define([
], function () {

    /* an unfortunate need to duplicate the file names here, but I haven't
     yet found a way to fool requirejs into doing dynamic dependencies */
    var staticFiles = [
        ],
        deps = arguments;

    return function (server) {
        server.respondWith(
            "POST",
            "/openidm/script?_action=eval",
            [
                500,
                { },
                "{\"code\":500,\"reason\":\"Internal Server Error\",\"message\":\"ReferenceError: \\\"test\\\" is not defined. (3543436FACEA27B7BDFC6685AC16EC009F615E83#1) in 3543436FACEA27B7BDFC6685AC16EC009F615E83 at line number 1 at column number 0\"}"
            ]
        );
    };

});

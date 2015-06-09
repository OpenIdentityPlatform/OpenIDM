/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
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

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

(function () {
    exports.setDefaultFields = function (object) {

        if (!object.accountStatus) {
            object.accountStatus = 'active';
        }

        if (!object.authzRoles) {
            object.authzRoles = [
                {
                    "_ref": "repo/internal/role/openidm-authorized"
                }
            ];
        }

        /* uncomment to randomly generate passwords for new users
         if (!object.password) {

         // generate random password that aligns with policy requirements
         object.password = require("crypto").generateRandomString([
         { "rule": "UPPERCASE", "minimum": 1 },
         { "rule": "LOWERCASE", "minimum": 1 },
         { "rule": "INTEGERS", "minimum": 1 },
         { "rule": "SPECIAL", "minimum": 1 }
         ], 16);

         }
         */
    }
}());
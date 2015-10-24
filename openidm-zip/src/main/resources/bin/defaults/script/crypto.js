/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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

/*global exports, openidm */

(function () {

    var canBeEncrypted= function (value) {
        // we are not going to do anything if the value is undefined/null/already encrypted
        return typeof value !== 'undefined' && value !== null && !openidm.isEncrypted(value);
    };

    exports.encrypt = function (value, cipher, alias) {

        cipher = cipher || 'AES/CBC/PKCS5Padding';
        alias = alias || identityServer.getProperty('openidm.config.crypto.alias', 'true', true);

        if (!canBeEncrypted(value)) {
            return value;
        }

        return openidm.encrypt(value, cipher, alias);
    };

    exports.hash = function (value, algorithm) {
        algorithm = algorithm || "SHA-256";
        if (!canBeEncrypted(value) || typeof value !== "string") {
            return value;
        }
        return openidm.hash(value, algorithm);
    };

    /**
     * Produces a random string conforming to a set of minimum complexity requirements and length.
     * Useful for setting random passwords.
     * @param {Object[]} minimum_requirements - a list of requirements for the random password
     * @param {string} minimum_requirements[].rule - the one of UPPERCASE,LOWERCASE,INTEGERS,SPECIAL
     * @param {number} minimum_requirements[].minimum - the number of characters required for this rule
     * @param {number} total_length - the size of the final random string
     * @example
         require('crypto').generateRandomString([
             { "rule": "UPPERCASE", "minimum": 1 },
             { "rule": "LOWERCASE", "minimum": 1 },
             { "rule": "INTEGERS", "minimum": 1 },
             { "rule": "SPECIAL", "minimum": 1 }
         ], 8) // returns a value like "R92kHZ;1"
     */
    exports.generateRandomString = function (minimum_requirements, total_length) {
        // returns a random int between the minimum (inclusive) and maximum (exclusive)
        function getRandomInt(minimum,maximum) {
          return Math.floor(Math.random()*(maximum-minimum))+minimum;
        }

        // returns an index chosen at random from the set of found undefined
        // value indexes in the provided array
        function getRandomUndefinedPosition(array,max) {
            var i = 0,foundUndefined = [];
            for (i=0;i<max;i++) {
                if (array[i] === undefined) {
                    foundUndefined.push(i);
                }
            }
            return foundUndefined[getRandomInt(0,foundUndefined.length)];
        }

        var character_buffer = [],
            i,j,
            character_generators = {
                UPPERCASE: function (buffer,max) {
                    // ASCII codes for A-Z: 65-90
                    buffer[getRandomUndefinedPosition(buffer,max)] = String.fromCharCode(getRandomInt(65,91));
                },
                LOWERCASE: function (buffer,max) {
                    // ASCII codes for a-z: 97-122
                    buffer[getRandomUndefinedPosition(buffer,max)] = String.fromCharCode(getRandomInt(97,123));
                },
                INTEGERS: function (buffer,max) {
                    buffer[getRandomUndefinedPosition(buffer,max)] = (getRandomInt(0,10) + "");
                },
                SPECIAL: function (buffer,max) {
                    // ASCII codes for 'special' characters: 58-64
                    buffer[getRandomUndefinedPosition(buffer,max)] = String.fromCharCode(getRandomInt(58,65));
                }
            }

        for (i=0;i<minimum_requirements.length;i++) {
            for (j=0;j<minimum_requirements[i].minimum;j++) {
                character_generators[minimum_requirements[i].rule](character_buffer, total_length);
            }
        }

        // while there aren't any remaining undefined positions, keep filling them in
        while (getRandomUndefinedPosition(character_buffer, total_length) !== undefined) {
            var generator_names = Object.keys(character_generators),
                random_generator = character_generators[generator_names[getRandomInt(0,generator_names.length)]];

            random_generator(character_buffer, total_length);
        }

        return character_buffer.join("");
    };

}());

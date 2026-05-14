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
 * Copyright 2015 ForgeRock AS.
 */

/**
 * Sets any additional password fields to the value of the main "password" fields if they have not already been set.
 * Additional password fields are declared using the "additionalPasswordFields" global array in the script config.
 */

/*global object */
var additionalPasswordFields,
    object = request.content;

// Set additional password fields using the value of the main password
// Don't set the field if it is already defined
if (additionalPasswordFields !== undefined && object.password !== undefined) {
    for (var key in additionalPasswordFields) {
        var field = additionalPasswordFields[key];
        // Check if the field is not already defined
        if (!object.hasOwnProperty(field)) {
            // Inherit the value
            object[field] = object.password;
        }
    }
}
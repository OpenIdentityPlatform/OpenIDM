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
 * Copyright 2011-2016 ForgeRock AS.
 */

define([
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager"
], function(constants, AbstractDelegate, configuration, eventManager) {

    var obj = new AbstractDelegate(constants.host + "/openidm/repo/internal/user");

    obj.patchSelectedUserAttributes = function(id, rev, patchDefinitionObject, successCallback, errorCallback, noChangesCallback) {
        //PATCH for repo is unsupported

        return obj.readEntity(id).then(function(user) {
            var i, v;

            for(i = 0; i < patchDefinitionObject.length; i++) {
                v = patchDefinitionObject[i];

                // replace any leading slashes to translate basic JSON Pointer
                // back into regular JS object property references
                v.field = v.field.replace(/^\//, '');

                user[v.field] = v.value;
            }

            return obj.updateEntity(id, user, successCallback, errorCallback);
        });
    };

    return obj;
});

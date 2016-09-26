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
    exports.preserveLastSync = function (object, oldObject, request) {
        if (request.getResourcePath !== "managed/user"
            && request.method === "update"
            && object.lastSync === undefined
            && oldObject.lastSync) {
            object.lastSync = oldObject.lastSync;
        }
    };

    /**
     * Persists or removes all relationships associated with current
     * identity providers for a managed user object.
     *
     * @param object managed user
     */
    exports.updateIdpRelationships = function (object) {

        // query all existing identity provider relationships for managed/user/{id}
        var existingIDPRelationships = openidm.query("managed/user/" + object._id + "/idpDataRelationships", { "_queryFilter": "true"}, ["*"]),
            relationshipsToRemove = [];

        // if there is any idp data associated with the current user,
        // compare it with the existing idp relationships
        if (object.idpData) {

            Object.keys(object.idpData).filter(function (provider) {
                var subject = object.idpData[provider].subject,
                // looks for any existing relationship which matches the current subject and provider
                // within the current managed user object
                    doesNotExist = existingIDPRelationships.result.filter(function (existingRelationship) {
                            return existingRelationship.subject === subject && existingRelationship.provider === provider;
                        }).length === 0;

                // current provider has to be enabled AND not exist already
                return object.idpData[provider].enabled !== false && doesNotExist;
            }).forEach(function (provider) {
                // creates a relationship between managed/user/{id} and managed/idpData
                openidm.create("managed/" + provider, object.idpData[provider].subject, object.idpData[provider].rawProfile);
                return {
                    _ref: "managed/" + provider + "/" + object.idpData[provider].subject
                };
            });

            // determines if a relationship needs to be removed by filtering on the following :
            // 1) if the identity provider is no longer present in the managedUser
            // 2) the identity provider is disabled in the managed user properties
            relationshipsToRemove = existingIDPRelationships.result.filter(function (existingRelationship) {
                return object.idpData[existingRelationship.provider] === undefined ||
                    object.idpData[existingRelationship.provider].enabled === false;
            });

        } else {
            // there are no relationships to identity providers in the current managed user
            // remove all existing relationships
            relationshipsToRemove = existingIDPRelationships.result;
        }

        // remove identity provider relationships that were
        // determined unnecessary between managed/user and manged/idpData
        relationshipsToRemove.forEach(function (relationship) {
            openidm['delete'](relationship._ref, relationship._rev);
        });

    };
}());
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

/*global exports, openidm, require */

/*
    This takes a resourceName and looks for all of the records that are linked to it,
    returning them as a list included within the data for the provided object.

    The returned object will have a new property named "linkedTo". This will be a list
    of all resources which link to the main object. Each item in the list will look like so:

    {
        "resourceName": "system/abc/id",
        "linkType": "systemAbc_managedUser",
        "mappings": [
            {
                "name": "systemAbc_managedUser",
                "type": "source"
            },
            {
                "name": "managedUser_systemAbc",
                "type": "target"
            }
        ],
        "content": {...}
    }

    There should only ever either be one or two mapping entries.
 */

exports.fetch = function (resourceName) {

    var _ = require('lib/lodash'),
        getException = function (e) {
            if (_.has(e, "javaException") && _.has(e.javaException, "cause") && e.javaException.cause !== null) {
                return e.javaException.cause.localizedMessage || e.javaException.cause.message;
            } else if (_.has(e, "messageDetail") && _.has(e.messageDetail, "message")) {
                return e.messageDetail.message;
            } else if (_.has(e, "message")) {
                return e.message;
            } else {
                return e;
            }
        },
        syncConfig = openidm.read("config/sync") || {},
        currentResource = {},
        resourceParts = resourceName.match(/(.*)\/(.*?)$/), // ["component/id", "component", "id"]

        component = resourceParts[1],
        id = resourceParts[2],

        // uses the sync config to build a map of linkTypes to resource containers
        resourceMap = _(syncConfig.mappings)
                        .filter(function (m) {
                            // we only care about those mappings which aren't using another mapping's links entry
                            // we also only care about those which involve the given component in some way
                            return  (m.links === undefined || m.links === m.name) &&
                                    (m.target === component || m.source === component);
                        })
                        .map(function (m) {
                            return [m.name, {
                                "firstContainer": m.source,
                                "secondContainer": m.target
                            }];
                        })
                        .object()
                        .value(),

        // all links found referring to this id
        allLinks = openidm.query("repo/link", {
            "_queryFilter": 'firstId eq "'+id+'" or secondId eq "'+id+'"'
        });

        try {
            // TODO-crest3: restore the below commented-out line when forgerock-script supports passing along crest3 contexts
            // https://bugster.forgerock.org/jira/browse/OPENIDM-3857 filed to track this regression
            //currentResource = openidm.read(resourceName, null, context.current);
            currentResource = openidm.read(resourceName);
        } catch (e) {
            currentResource["error"] = getException(e);
        }

        return _.extend(currentResource, {
            "linkedTo": _(allLinks.result)

                // Need to verify that these links we are processing are one of those we know relates to the given resourceName
                // it's possible that the link queries above found results for id values which happen to match the one provided, but
                // are in fact unrelated to the given resource. This filter guards against that possibility.
                .filter(function (l) {
                    return _.has(resourceMap, l.linkType);
                })

                // For each of the found links, determine the full linked resourceName and return some useful
                // information about it.
                .map(function (l) {
                    var linkedResourceName,
                        linkedResource = {};

                    if (resourceMap[l.linkType].firstContainer === component) {
                        linkedResourceName = resourceMap[l.linkType].secondContainer + '/' + l.secondId;
                    } else {
                        linkedResourceName = resourceMap[l.linkType].firstContainer + '/' + l.firstId;
                    }

                    try {
                        linkedResource = openidm.read(linkedResourceName, null, context.current);
                    } catch (e) {
                        linkedResource["error"] = getException(e);
                    }

                    return {
                        "resourceName": linkedResourceName,
                        "content": linkedResource,
                        "linkQualifier" : l.linkQualifier,
                        "linkType": l.linkType,
                        "mappings": _(syncConfig.mappings)
                                    .filter(function (m) {
                                        return m.name === l.linkType || m.links === l.linkType;
                                    })
                                    .map(function (m) {
                                        return {
                                            "name": m.name,
                                            // the type is how the linkedResourceName relates to the main
                                            // resourceName in the context of a particular mapping.
                                            "type": (component === m.source) ? "target" : "source"
                                        }
                                    })
                                    .value()
                    };
                })
                .value()

            });
};

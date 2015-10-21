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
 * Copyright 2014-2015 ForgeRock AS.
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
        linkedResources = {};

    // read the resource; if the read throws an exception, let the backend handle it
    var currentResource = openidm.read(resourceName);
    if (currentResource === null) {
        return currentResource;
    }

    try {
        // read the linked resources
        linkedResources = openidm.action("sync", "getLinkedResources", {}, { "resourceName" : resourceName });
    } catch (e) {
        currentResource["error"] = getException(e);
    }

    // augment the resource with the resources that link to the main object
    return _.extend(currentResource, {
        "linkedTo": linkedResources
    });
};

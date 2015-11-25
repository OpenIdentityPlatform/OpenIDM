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
 * When a managed/user is linked to a target resource, this adds a relationship to the managed/user's historicalAccounts
 * field which also includes the linked date property and will set the historical account as being active.
 * 
 * The newly added relationship is of the form:
 * 
 * {
 *     "_ref" : "system/ldap/accounts/uid=jdoe,ou=People,dc=example,dc=com"
 *     "_refProperties" : {
 *         "_id": "b6580bf0-7ece-4856-b716-64f16f8cb6a7",
 *         "_rev": "2",
 *         "linkedDate" : "Sun Oct 04 2015 19:18:31 GMT-0700 (PDT)",
 *         "active" : true
 *     }
 * }
 */

var targetRef = mappingConfig.target + "/" + target._id,
    sourcePath = mappingConfig.source + "/" + source._id,
    state = target.disabled ? "disabled" : "enabled",
    historicalAccount = {
		"_ref" : targetRef,
        "_refProperties" : {
        	"active" : true,
        	"linkDate" : (new Date()).toString(),
            "state" : state,
            "stateLastChanged" : (new Date()).toString()
        }
    },
    result;

logger.debug("Adding historical account " + targetRef + " to managed user " + source._id);

result = openidm.create(sourcePath + "/historicalAccounts", null, historicalAccount);

logger.debug("Created historical account: " + result);

/** 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

var mappings,
    syncConfig,
    sourceQuery,
    sourceQueryId,
    result, 
    params = {
        "waitForCompletion": "true"
    },
    value = {
    	"sourceQuery" : {
            "queryId": "get-users-of-direct-role",
            "role": resourceName	
        }
    };

// Check if default sourceQuery is overridden
if (sourceQuery) {
	value.sourceQuery = sourceQuery;
}

// Check if the queryId is overridden
if (sourceQueryId) {
	value.sourceQuery.queryId = sourceQueryId;
}

// Get sync mappings
syncConfig = openidm.read("config/sync");

// Iterate over mappings calling recon on each one
if (syncConfig && syncConfig.mappings) {
    mappings = syncConfig.mappings;
    for (i = 0; i < mappings.length; i++) {
        var mapping = mappings[i];
        if (mapping.source == "managed/user") {
            var name = mapping.name;
            params.mapping = name;
            openidm.action("recon", "recon", params, value);
        }
    }
}
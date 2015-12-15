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
 * Copyright 2015 ForgeRock AS.
 */

/**
 * Performs necessary setup on a reconciliation event.
 *
 * Pre-loads all roles.
 * Pre-loads all assignments associated with the current mapping.
 *
 * The following variables are supplied:
 *   context: the current request context
 *   mappingConfig: the mapping configuration
 */

import org.forgerock.openidm.sync.ReconContext;
import org.forgerock.services.context.Context;

def reconContext = context.asContext(ReconContext.class)
def source = mappingConfig.source.getObject() as String
def target = mappingConfig.target.getObject() as String

if ((target.equals("managed/user") || source.equals("managed/user")) && reconContext != null) {
    def assignments = openidm.query("managed/assignment", [ "_queryFilter" : '/mapping eq ' + mappingConfig.name ]).result
    def roles = openidm.query("managed/role", [ "_queryFilter" : 'true' ], [ "*", "assignments" ]).result

    reconContext.put("assignments", assignments)
    reconContext.put("roles", roles)
}

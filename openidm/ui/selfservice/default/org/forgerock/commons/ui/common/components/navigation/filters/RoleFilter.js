"use strict";

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
 * Copyright 2015-2016 ForgeRock AS.
 */

/**
 * Filters links on the logged-in users role, returning the first matching link.
 *
 * If a link has no role, it will be considered matching.
 *
 * @module org/forgerock/commons/ui/common/components/navigation/filters/RoleFilter
 */
define(["lodash", "org/forgerock/commons/ui/common/main/Configuration"], function (_, Configuration) {
    return {
        filter: function filter(links) {
            var link, linkName, linkHasNoRole, userHasNecessaryRole;

            for (linkName in links) {
                link = links[linkName];

                linkHasNoRole = !link.role;
                userHasNecessaryRole = link.role && Configuration.loggedUser && _.contains(Configuration.loggedUser.uiroles, link.role);

                if (linkHasNoRole || userHasNecessaryRole) {
                    return links[linkName];
                }
            }
        }
    };
});

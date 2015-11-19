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

/** 
 * Calculates the effective roles
 */  

/*global object */

logger.debug("Invoked effectiveRoles script on {} value: ", propertyName, object);
// Allow for configuration in virtual attribute config, but default
if (rolesPropName === undefined) {
    var rolesPropName = "roles";
}
logger.trace("Configured rolesPropName: {}", rolesPropName);

var directRoles = object[rolesPropName];
var effectiveRoles = directRoles == null ? [] : directRoles;

// This is the location to expand to dynamic roles, 
// project role script return values can then be added via
// effectiveRoles = effectiveRoles.concat(dynamicRolesArray);
if (object.department === 'Human Resources') {
    effectiveRoles = effectiveRoles.concat([{"_ref":"managed/role/human-resources"}]);
}
if (object.department === 'Production Planning') {
    effectiveRoles = effectiveRoles.concat([{"_ref":"managed/role/production-planning"}]);
}
if (object.department === 'Sales & Distribution') {
    effectiveRoles = effectiveRoles.concat([{"_ref":"managed/role/sales-distribution"}]);
}
if (object.department === 'Treasury & Payments') {
    effectiveRoles = effectiveRoles.concat([{"_ref":"managed/role/treasury-payments"}]);
}

effectiveRoles;

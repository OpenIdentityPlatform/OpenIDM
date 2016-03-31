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
 * Copyright 2011-2015 ForgeRock AS.
 */

/*global define*/

define("org/forgerock/openidm/ui/common/util/Constants", [
    "org/forgerock/commons/ui/common/util/Constants"
], function (commonConstants) {
    commonConstants.context = "openidm";

    commonConstants.HEADER_PARAM_PASSWORD = "X-OpenIDM-Password";
    commonConstants.HEADER_PARAM_USERNAME = "X-OpenIDM-Username";
    commonConstants.HEADER_PARAM_NO_SESSION = "X-OpenIDM-NoSession";
    commonConstants.HEADER_PARAM_REAUTH = "X-OpenIDM-Reauth-Password";

    commonConstants.DOC_URL = "https://forgerock.org/openidm/doc/bootstrap/";

    commonConstants.EVENT_POLICY_FAILURE = "EVENT_POLICY_FAILURE";

    //Events
    commonConstants.EVENT_QUALIFIER_CHANGED = "mapping.properties.EVENT_QUALIFIER_CHANGED";

    commonConstants.EVENT_UPDATE_NAVIGATION = "common.navigation.EVENT_UPDATE_NAVIGATION";
    commonConstants.EVENT_SELF_SERVICE_CONTEXT = "common.navigation.EVENT_SELF_SERVICE_CONTEXT";

    return commonConstants;
});

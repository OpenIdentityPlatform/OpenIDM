/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All rights reserved.
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

/*global define*/

define("org/forgerock/openidm/ui/common/util/Constants", [
    "org/forgerock/commons/ui/common/util/Constants"
], function (commonConstants) {
    commonConstants.context = "openidm";

    commonConstants.HEADER_PARAM_PASSWORD = "X-OpenIDM-Password";
    commonConstants.HEADER_PARAM_USERNAME = "X-OpenIDM-Username";
    commonConstants.HEADER_PARAM_NO_SESSION = "X-OpenIDM-NoSession";
    commonConstants.HEADER_PARAM_REAUTH = "X-OpenIDM-Reauth-Password";

    commonConstants.DOC_URL = "http://openidm.forgerock.org/doc/";

    commonConstants.EVENT_USER_UPDATE_POLICY_FAILURE = "EVENT_USER_UPDATE_POLICY_FAILURE";

    //Events
    commonConstants.EVENT_QUALIFIER_CHANGED = "mapping.properties.EVENT_QUALIFIER_CHANGED";
    
    return commonConstants;
});

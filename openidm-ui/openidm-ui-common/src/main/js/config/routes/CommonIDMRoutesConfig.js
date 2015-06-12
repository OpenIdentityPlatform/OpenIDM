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

/*global define*/
define("config/routes/CommonIDMRoutesConfig", [
], function() {

    var obj = {
        "mandatoryPasswordChangeDialog" : {
            base: "landingPage",
            dialog: "org/forgerock/openidm/ui/common/MandatoryPasswordChangeDialog",
            url: "change_password/",
            role: "ui-admin"
        },
        "authenticationUnavailable" : {
            view: "org/forgerock/openidm/ui/common/login/AuthenticationUnavailable",
            url: "authenticationUnavailable/"
        },
        /*
         * this is an override of the UserRoutesConfig definition because the openidm 
         * implementation of change security dialog is not yet using bootstrap-dialog
         */
        "changeSecurityData": {
            base: "profile",
            dialog: "ChangeSecurityDataDialog",
            role: "ui-user,ui-admin",
            url: "profile/change_security_data/"
        }
    };

    return obj;
});

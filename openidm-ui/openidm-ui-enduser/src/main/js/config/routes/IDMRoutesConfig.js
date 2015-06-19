/** 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All rights reserved.
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

/**
 * @author huck.elliott
 */
define("config/routes/IDMRoutesConfig", [
    "org/forgerock/commons/ui/common/util/Constants"
], function(constants) {
    
    var obj = {
        "siteIdentification": {
            base: "profile",
            dialog: "org/forgerock/openidm/ui/profile/ChangeSiteIdentificationDialog",
            url: "profile/site_identification/",
            role: "ui-user,ui-admin",
            excludedRole: "ui-admin"
        },
        "termsOfUse": {
            base: "selfRegistration",
            dialog: "org/forgerock/openidm/ui/registration/TermsOfUseDialog",
            url: "register/terms_of_use/"
        },
        "forgottenPassword" : {
            base: "login",
            dialog: "org/forgerock/openidm/ui/passwordReset/ForgottenPasswordDialog",
            url: "profile/forgotten_password/"
        },
        "enterOldPassword": {
            base: "profile",
            dialog: "org/forgerock/openidm/ui/profile/EnterOldPasswordDialog",
            role: "ui-user,ui-admin",
            url: "profile/old_password/"
        }
    };
    
    return obj;
});
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

/*global define */

define("org/forgerock/openidm/ui/user/anonymousProcess/SelfRegistrationView", [
    "jquery",
    "lodash",
    "form2js",
    "handlebars",
    "org/forgerock/commons/ui/user/anonymousProcess/AnonymousProcessView",
    "org/forgerock/commons/ui/user/anonymousProcess/SelfRegistrationView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager"
], function($, _, form2js, Handlebars,
    AnonymousProcessView,
    CommonSelfRegistrationView,
    ValidatorsManager) {

    var SelfRegistrationView = AnonymousProcessView.extend({
        baseEntity: "selfservice/registration"
    });

     SelfRegistrationView.prototype = _.extend(Object.create(CommonSelfRegistrationView), SelfRegistrationView.prototype);

    return new SelfRegistrationView();
});

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
 * Copyright 2014-2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate"
], function($, _, constants, AbstractDelegate) {

    var obj = new AbstractDelegate(constants.host + "/openidm/identityProviders");

    obj.availableProviders = function() {
        return obj.serviceCall({
            url: "",
            type: "get"
        }).then((results) => {
            return results;
        });
    };

    obj.providerList = function() {
        return obj.serviceCall({
            url: "?_action=availableProviders",
            type: "post"
        }).then((results) => {
            return results;
        });
    };

    return obj;
});

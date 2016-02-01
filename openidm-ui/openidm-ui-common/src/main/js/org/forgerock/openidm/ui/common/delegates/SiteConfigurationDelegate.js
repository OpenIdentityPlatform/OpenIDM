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
 * Copyright 2011-2016 ForgeRock AS.
 */

/*global define, require */

define("org/forgerock/openidm/ui/common/delegates/SiteConfigurationDelegate", [
    "org/forgerock/openidm/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager"
], function(Constants,
            AbstractDelegate,
            configuration) {

    var obj = new AbstractDelegate(Constants.host + "/" + Constants.context + "/config/ui/configuration");

    obj.getConfiguration = function(successCallback, errorCallback) {
        var headers = {};
        headers[Constants.HEADER_PARAM_USERNAME] = "anonymous";
        headers[Constants.HEADER_PARAM_PASSWORD] = "anonymous";
        headers[Constants.HEADER_PARAM_NO_SESSION] = "true";

        return obj.serviceCall({
            url: "",
            headers: headers
        }).then(function(data) {

            if (data.configuration.kbaEnabled === true) {
                require.config({"map": { "*": {
                    "UserProfileView" : "org/forgerock/commons/ui/user/profile/UserProfileKBAView"
                } } } );
            } else {
                require.config({"map": { "*": {
                    "UserProfileView": "org/forgerock/commons/ui/user/profile/UserProfileView"
                } } } );
            }

            if(successCallback) {
                successCallback(data.configuration);
            }
            return data.configuration;
        }, errorCallback);
    };
    return obj;
});

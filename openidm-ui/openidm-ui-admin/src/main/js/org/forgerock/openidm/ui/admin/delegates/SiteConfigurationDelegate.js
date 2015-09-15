/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All rights reserved.
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

/*global define */

define("org/forgerock/openidm/ui/admin/delegates/SiteConfigurationDelegate", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/delegates/SiteConfigurationDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/components/Navigation"
], function($, _, conf, commonSiteConfigurationDelegate, eventManager, constants, Navigation) {

    var obj = commonSiteConfigurationDelegate;

    obj.checkForDifferences = function(){
        var promise = $.Deferred();

        if (conf.loggedUser && _.contains(conf.loggedUser.get("roles"),"ui-admin") &&
            Navigation.configuration.links.admin.urls.managed.urls.length === 0) {

            eventManager.sendEvent(constants.EVENT_UPDATE_NAVIGATION,
                {
                    callback: function () {
                        promise.resolve();
                    }
                }
            );

        } else {
            promise.resolve();
        }

        return promise;
    };

    return obj;
});

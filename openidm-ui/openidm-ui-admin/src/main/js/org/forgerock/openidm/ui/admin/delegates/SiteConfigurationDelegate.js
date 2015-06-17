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

/*global $, define, _ */

/**
 * @author huck.elliott
 */
define("org/forgerock/openidm/ui/admin/delegates/SiteConfigurationDelegate", [
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/delegates/SiteConfigurationDelegate",
    "org/forgerock/commons/ui/common/components/Navigation",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function(conf, commonSiteConfigurationDelegate, nav, configDelegate) {

    var obj = commonSiteConfigurationDelegate;

    obj.checkForDifferences = function(){
        if(_.contains(conf.loggedUser && conf.loggedUser.roles,"ui-admin")){
            return obj.setDynamicNavItems();
        } else {
            return $.Deferred().resolve();
        }
    };

    obj.setDynamicNavItems = function() {
        return configDelegate.readEntity("managed").then(function(managedConfig){
            nav.configuration.links.admin.urls.managed.urls = [];

            _.each(managedConfig.objects, function(managed) {
                if(!managed.schema.icon) {
                    managed.schema.icon = "fa-cube";
                }

                nav.configuration.links.admin.urls.managed.urls.push({
                    "url" : "#resource/managed/" +managed.name +"/list/",
                    "name" : managed.name,
                    "icon" : "fa " +managed.schema.icon
                });
            });

            nav.configuration.links.admin.urls.managed.urls.push({
                "url" : "#managed/add/",
                "name" : "New Managed Object",
                "icon" : "fa fa-plus"
            });

            nav.reload();
        });
    };

    return obj;
});
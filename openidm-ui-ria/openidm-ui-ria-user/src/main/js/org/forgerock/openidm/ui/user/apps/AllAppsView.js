/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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

/*global define, $, _ */

/**
 * @author jdabrowski
 */
define("org/forgerock/openidm/ui/user/apps/AllAppsView", [
    "org/forgerock/openidm/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/user/delegates/ApplicationDelegate",
    "org/forgerock/openidm/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/user/apps/UsersApplicationsView",
    "org/forgerock/openidm/ui/user/apps/DefaultApplicationsView",
    "org/forgerock/openidm/ui/user/delegates/UserApplicationLnkDelegate",
    "org/forgerock/openidm/ui/user/apps/AllApplicationsView",
    "org/forgerock/openidm/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/main/UniversalCachedDelegate"
], function(AbstractView, 
                applicationDelegate, 
                conf, 
                UsersApplicationsView, 
                DefaultApplicationsView, 
                userApplicationLnkDelegate, 
                AllApplicationsView, 
                eventManager, 
                constants,
                universalCachedDelegate) {
    var appsViewInstance, AllAppsView = AbstractView.extend({
        template: "templates/user/apps/AllAppsTemplate.html",
        
        defaultApplicationsView: new DefaultApplicationsView(),
        
        usersApplicationsView: new UsersApplicationsView(),
        
        allApplicationsView: new AllApplicationsView(),
        
        render: function(args, callback) {
            var self = this;
            this.parentRender(_.bind(function() {
                
                //user application view
                userApplicationLnkDelegate.getUserApplicationLnksForUserName(conf.loggedUser.userName, function(userApplicationLnks) {
                    applicationDelegate.prepareApplications(userApplicationLnks, function() {
                        self.usersApplicationsView.render({el: $("#userApps"), items: userApplicationLnks});
                        self.defaultApplicationsView.render({el: $("#defaultAppsTable"), items: userApplicationLnks});
                    });
                });
                
            }, this));            
        }
    });
    
    appsViewInstance = new AllAppsView();
    return appsViewInstance;
});



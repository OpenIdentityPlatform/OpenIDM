/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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
 * @author mbilski
 */
define("org/forgerock/openidm/ui/apps/dashboard/DashboardView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/apps/dashboard/BaseUserInfoView",
    "org/forgerock/openidm/ui/apps/dashboard/NotificationsView",
    "org/forgerock/openidm/ui/apps/FrequentlyUsedApplicationsView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/admin/notifications/NotificationDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/apps/delegates/ApplicationDelegate",
    "org/forgerock/openidm/ui/apps/delegates/UserApplicationLnkDelegate",
    "org/forgerock/commons/ui/common/main/UniversalCachedDelegate"
], function(AbstractView, 
            baseUserInfoView, 
            NotificationsView, 
            FrequentlyUsedApplicationsView, 
            conf, 
            notificationDelegate, 
            eventManager, 
            constants, 
            applicationDelegate, 
            userApplicationLnkDelegate,
            universalCachedDelegate) {
    
    var DashboardView = AbstractView.extend({
        template: "templates/apps/dashboard/DashboardTemplate.html",
            
        render: function(args, callback) {
                this.parentRender(_.bind(function() {
                    var notificationsView, frequentlyUsedAppsTableView;
                    
                    //profile
                    if(conf.loggedUser.roles.indexOf("openidm-admin") === -1) {
                        baseUserInfoView.render();
                    
                        //notifications
                        notificationDelegate.getNotificationsForUser(function(notifications) {
                            
                            notifications.sort(function(a, b) {
                                if (a.requestDate < b.requestDate) {
                                    return 1;
                                }
                                if (a.requestDate > b.requestDate){
                                    return -1;
                                }
                                return 0;
                            });
                            
                            notificationsView = new NotificationsView();
                            notificationsView.render({el: $("#notifications"), items: notifications});
                        }, function() {
                            eventManager.sendEvent(constants.EVENT_GET_NOTIFICATION_FOR_USER_ERROR);
                        });
                        
                        //applications
                        userApplicationLnkDelegate.getUserApplicationLnksForUser(conf.loggedUser._id, function(userApplicationLnks) {
                            applicationDelegate.prepareApplications(userApplicationLnks, function() {
                                frequentlyUsedAppsTableView = new FrequentlyUsedApplicationsView();
                                frequentlyUsedAppsTableView.render({el: $("#applicationTable"), items: userApplicationLnks});
                            });
                        });
                    }
                    
                    if(callback) {
                        callback();
                    }
                }, this));            
            }
    });
    
    return new DashboardView();
});



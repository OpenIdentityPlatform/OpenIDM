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
define("org/forgerock/openidm/ui/apps/AddMoreAppsView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/apps/delegates/ApplicationDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/apps/UsersApplicationsView",
    "org/forgerock/openidm/ui/apps/DefaultApplicationsView",
    "org/forgerock/openidm/ui/apps/delegates/UserApplicationLnkDelegate",
    "org/forgerock/openidm/ui/apps/AllApplicationsView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/UniversalCachedDelegate"
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
    var appsViewInstance, AddMoreAppsView = AbstractView.extend({
        template: "templates/apps/AppsTemplate.html",
        
        defaultApplicationsView: new DefaultApplicationsView(),
        
        usersApplicationsView: new UsersApplicationsView(),
        
        allApplicationsView: new AllApplicationsView(),
        
        render: function(args, callback) {
            var self = this;
            this.parentRender(_.bind(function() {
                
              //all apps
              applicationDelegate.getAllApplications(function(apps) {
                  self.allApplicationsView.render({el: $("#appsTable"), items: apps});
                  $("#appsTable li").draggable({
                      appendTo: "body",
                      helper: "clone"
                  });
              });
                
                //user application view
                userApplicationLnkDelegate.getUserApplicationLnksForUser(conf.loggedUser._id, function(userApplicationLnks) {
                    
                    applicationDelegate.prepareApplications(userApplicationLnks, function() {
                        
                        self.usersApplicationsView.render({el: $("#userApps"), items: userApplicationLnks});
                        $("#userAppsView").droppable({
                            activeClass: "ui-state-default",
                            hoverClass: "ui-state-hover",
                            accept: ":not(.ui-sortable-helper)",
                            drop: function( event, ui ) {
                                var idAppToAdd = ui.draggable.find("input").val(), appName = ui.draggable.find("span:last").text();
                                self.usersApplicationsView.addApplication(idAppToAdd, appName);
                            }
                        });
                        
                        self.defaultApplicationsView.render({el: $("#defaultAppsTable"), items: userApplicationLnks});
                        
                    });
                });
                
            }, this));            
        },
        
        initOnce: function() {
            var self = this;
            eventManager.registerListener(constants.EVENT_USER_APPLICATION_DEFAULT_LNK_CHANGED, function(newUserApplicationLnk) {
                self.defaultApplicationsView.rebuildView();
            });
        }
        
    });
    
    appsViewInstance = new AddMoreAppsView();
    appsViewInstance.initOnce();
    return appsViewInstance;
});



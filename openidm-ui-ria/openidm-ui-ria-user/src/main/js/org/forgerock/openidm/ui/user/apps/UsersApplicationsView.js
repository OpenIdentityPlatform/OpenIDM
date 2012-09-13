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

/*global define, _, $ */

/**
 * @author jdabrowski
 */
define("org/forgerock/openidm/ui/user/apps/UsersApplicationsView", [
    "org/forgerock/openidm/ui/user/BaseApplicationsView",
    "org/forgerock/openidm/ui/user/delegates/ApplicationDelegate",
    "org/forgerock/openidm/ui/user/delegates/UserDelegate",
    "org/forgerock/openidm/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/util/Constants",
    "org/forgerock/openidm/ui/user/delegates/NotificationDelegate",
    "org/forgerock/openidm/ui/common/util/DateUtil",
    "org/forgerock/openidm/ui/user/delegates/UserApplicationLnkDelegate",
    "org/forgerock/openidm/ui/common/main/UniversalCachedDelegate"
], function(BaseApplicationsView, 
            applicationDelegate, 
            userDelegate, 
            conf, 
            eventManager, 
            constants, 
            notificationDelegate, 
            dateUtil, 
            userApplicationLnkDelegate,
            universalCachedDelegate) {
    
    var UsersApplicationsView = BaseApplicationsView.extend({
        
        events: {
            "click .ui-state-close" : "onClick",
            "click li" : "preventDefault",
            "sortupdate" : "orderChanged"
        },
        
        runAfterRebuildSteps: function(event) {
            this.$el.find("ul li").each(function() {
                var el, stateId;
                stateId = $(this).find("input[name='state']").val();
                el = $(this);
                universalCachedDelegate.read("user_application_state", stateId, function(state) {
                    el.addClass('ui-state-app-' + state.name);
                });
            });
        },
        
        orderChanged: function() {
            var patchDefinitionObject = [];
            patchDefinitionObject.push({replace: "userApplicationsOrder", value: this.getItems()});
            userDelegate.patchSelectedUserAttributes(conf.loggedUser.userName, patchDefinitionObject, _.bind(function(r) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "userApplicationsUpdate"); 
                userDelegate.getForUserName(conf.loggedUser.userName, function(user) {
                    conf.loggedUser = user;
                });
            }, this), _.bind(function(r) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unknown");
            }, this));
        },
        
        onClick: function(event) {
            var self = this, applicationId, lnkId;
            event.preventDefault();
            
            applicationId = $(event.target).next().find("input:first").val(); 
            lnkId = $(event.target).next().find("input:last").val(); 
            
            userApplicationLnkDelegate.deleteEntity(lnkId, function() {
                var appDetails, newNotification;
                
                self.removeItemAndRebuild(lnkId);
                self.orderChanged();
                
                appDetails = applicationDelegate.getApplicationDetails(applicationId);
            });
        },
        
        addApplication: function(appId, appName) {
            var self = this, newUserApplicationLnk;
            
            if (!this.appExists(appId)) {
                universalCachedDelegate.readByName("user_application_state", "pending", function(state) {
                    newUserApplicationLnk = {
                            "state": state._id, 
                            "applicationId": appId, 
                            "userName": conf.loggedUser.userName,
                            "lastTimeUsed": new Date()
                    };
                    
                    userApplicationLnkDelegate.createEntity(newUserApplicationLnk, function(userAppLnk) {
                        var appDetails, newNotification;
                        newUserApplicationLnk._id = userAppLnk._id;
                        self.addItemAndRebuild(newUserApplicationLnk);
                        self.orderChanged();
                        
                        appDetails = applicationDelegate.getApplicationDetails(newUserApplicationLnk.applicationId);
                        if (appDetails.isDefault === true) {
                            eventManager.sendEvent(constants.EVENT_USER_APPLICATION_DEFAULT_LNK_CHANGED, appDetails);
                        }
                        
                    });
                });
            }
        },
        
        closable: function() {
            return true;
        },
        
        preventDefault: function(event) {
            event.preventDefault();
        },
        
        noItemsMessage: function(item) {
            return "You have no applications";
        },
        
        shouldApplicationBeVisible: function(item) {
            if (item.isDefault) {
                return false;
            } else {
                return true;
            }
        },
        
        sort: function(item) {
            if (conf.loggedUser.userApplicationsOrder) {
                var order = conf.loggedUser.userApplicationsOrder.slice(0), reverse, i;
                order.reverse();
                for (i = 0; i < order.length; i++) {
                   this.moveToTopOfTheAppListIfExists(order[i]);
                }
            }
        },
        
        moveToTopOfTheAppListIfExists: function(elementId) {
            var i, element;
            for (i = 0; i < this.items.length; i++) {
                if (this.items[i].applicationId === elementId) {
                    element = this.items[i];
                    this.items.splice(i,1);
                    this.items.unshift(element);
                    return;
                }
            }
        }
    
    });
    
    return UsersApplicationsView;
});
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

/*global define, $, form2js, _, js2form, document */

/**
 * @author jdabrowski
 */
define("org/forgerock/openidm/ui/common/workflow/processes/customview/SendNotificationProcess", [
    "org/forgerock/openidm/ui/common/workflow/processes/AbstractProcessForm",
    "UserDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/common/notifications/NotificationViewHelper"
], function(AbstractProcessForm, userDelegate, conf, validatorsManager, notificationViewHelper) {
    var SendNotificationProcess = AbstractProcessForm.extend({
        
        template: "templates/workflow/processes/customview/SendNotificationTemplate.html",
        
        prepareData: function(callback) {
             var nTypes, notificationType;
             _.extend(this.data, this.processDefinition);
             this.data.loggedUser = conf.loggedUser;
             
             nTypes = {};
             for (notificationType in notificationViewHelper.notificationTypes) {
                 nTypes[notificationType] = $.t(notificationViewHelper.notificationTypes[notificationType].name);
             }
             this.data.notificationTypes = nTypes;
             this.data.defaultNotificationType = notificationViewHelper.defaultType;
             
            _.bind(function() {
                
                userDelegate.getAllUsers(_.bind(function(users) {
                    
                    var resultMap = {},userPointer,user;
                    for (userPointer in users) {
                        user = users[userPointer];
                        resultMap[user._id] = user.givenName + " " + user.familyName;
                    }
                    this.data.users = resultMap;
                    callback();
                    
                }, this));
                
            }, this)();
        },
        
        postRender: function() {
            validatorsManager.bindValidators(this.$el);
            validatorsManager.validateAllFields(this.$el);
        }
        
    }); 
    
    return new SendNotificationProcess();
});



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

define([
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/common/workflow/processes/AbstractProcessForm",
    "UserDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/ValidatorsManager"
], function($, _,
            AbstractProcessForm,
            userDelegate,
            conf,
            validatorsManager) {
    var SendNotificationProcess = AbstractProcessForm.extend({

        template: "templates/workflow/processes/customview/SendNotificationTemplate.html",

        prepareData: function(callback) {
            var nTypes, notificationType;
            _.extend(this.data, this.processDefinition);
            this.data.loggedUser = conf.loggedUser.toJSON();

            nTypes = {};
            for (notificationType in conf.globalData.notificationTypes) {
                nTypes[notificationType] = $.t(conf.globalData.notificationTypes[notificationType].name);
            }
            this.data.notificationTypes = nTypes;
            this.data.defaultNotificationType = conf.globalData.notificationTypes.defaultType;

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

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

/*global define */

define("org/forgerock/openidm/ui/common/notifications/NotificationsView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/common/notifications/NotificationDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/DateUtil"
], function($, _,
            AbstractView,
            notificationDelegate,
            eventManager,
            constants,
            conf,
            DateUtil) {
    var NotificationsView = AbstractView.extend({
        events: {
            "click .list-item-close" : "deleteLink"
        },
        element: "#notifications",
        template: "templates/notifications/NotificationMessageTemplate.html",
        noBaseTemplate: true,
        data: {

        },

        render: function(args, callback) {
            this.element = args.el;
            this.data.notifications = args.items;

            _.each(this.data.notifications, function(notification){
                notification.createDate = DateUtil.formatDate(notification.createDate, "MMMM dd, yyyy HH:mm");
            });

            this.parentRender(_.bind(function() {
                if(callback) {
                    callback();
                }
            }));
        },

        deleteLink: function(event) {
            var notificationId,
                self = this;

            event.preventDefault();

            notificationId = $(event.target).parents(".list-group-item").find("input[name=id]").val();

            notificationDelegate.deleteEntity(notificationId, _.bind(function() {
                    $(event.target).parents(".list-group-item").remove();

                    this.data.notifications = _.filter(this.data.notifications, function(notification){
                        return notificationId !== notification.id;
                    }, this);

                    self.render({
                        "el" : self.$el,
                        "notifications" : this.data.notifications
                    });
                }, this),
                function() {
                    eventManager.sendEvent(constants.EVENT_NOTIFICATION_DELETE_FAILED);

                    notificationDelegate.getNotificationsForUser(function(notificationList) {
                        self.render({
                            "el" : self.$el,
                            "notifications" : notificationList.notifications
                        });
                    }, function() {
                        eventManager.sendEvent(constants.EVENT_GET_NOTIFICATION_FOR_USER_ERROR);
                    });
                });

        }
    });

    return new NotificationsView();
});
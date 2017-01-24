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
 * Copyright 2011-2017 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "handlebars",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/notifications/Notifications"
], function($, _,
            Handlebars,
            AbstractView,
            constants,
            EventManager,
            Notifications
        ) {
    var NotificationsView = AbstractView.extend({
        template: "templates/notifications/NotificationMessageTemplate.html",
        partials: [
            "partials/notifications/_notification.html",
            "partials/notifications/_noNotifications.html"
        ],
        noBaseTemplate: true,
        notificationItems: [],
        loaded: false,

        initialize: function(args, options) {
            this.notifications = new Notifications.Collection();
            EventManager.registerListener("NOTIFICATION_DELETE", (id) => {
                if (this.notifications.get(id)) {
                    this.notifications.remove(id);
                }
                this.renderNotifications();
                this.trigger("change");
            });
            AbstractView.prototype.initialize.call(this, args, options);
        },

        render: function(args, callback) {
            // allows render on different elements
            if (args && args.el) {
                this.element = args.el;
            }
            if (!this.loaded) {
                this.parentRender(() => {
                    this.fetchNotifications((collection, response, options) => {
                        this.loaded = true;
                        this.renderNotifications();
                    });

                });
            } else {
                this.renderNotifications();
            }
            return this;
        },

        fetchNotifications: function(callback) {
            const error = () => {
                EventManager.sendEvent(constants.EVENT_GET_NOTIFICATION_FOR_USER_ERROR);
            };
            const success = (collection, response, options) => {
                if (callback) {
                    callback(collection, response, options);
                }
            };

            this.notifications.fetch({ success, error });
        },

        renderNotifications: function() {
            this.$el.empty();
            if (this.notifications.length === 0) {
                this.$el.html(Handlebars.compile("{{> notifications/_noNotifications}}")());
            } else {
                this.notifications.forEach((notification) => {
                    var notificationItem = new Notifications.ItemView({
                        model: notification,
                        template: Handlebars.compile("{{> notifications/_notification}}")
                    });
                    this.$el.append(notificationItem.el);
                    this.notificationItems.push(notificationItem);
                });
            }
        }
    });

    return NotificationsView;
});

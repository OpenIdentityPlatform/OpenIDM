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
 * Copyright 2017 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "handlebars",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/notifications/NotificationsView",
    "org/forgerock/commons/ui/common/main/Configuration"
], function($, _,
            Handlebars,
            AbstractView,
            constants,
            EventManager,
            NotificationsView,
            configuration
        ) {
    var NavigationNotificationsView = AbstractView.extend({
        events: {
            "click [data-toggle='popover']" : "showPopover"
        },
        noBaseTemplate: true,

        render: function(args, callback) {
            if (_.has(args, "el")) {
                this.setElement(args.el);
            }
            this.View = new NotificationsView();
            this.notifications = this.View.notifications;

            if (configuration.loggedUser) {
                this.View.fetchNotifications((notifications) => {
                    this.updateBadge(notifications.length);
                });
            }

            this.initializePopover();
            this.nonPopoverClickHandler();

            // respond to changes withing the collection (e.g. an item was deleted elsewhere)
            this.View.on("change", (event) => {
                this.updateBadge(this.notifications.length);
            });

            this.registerListener();
        },

        registerListener: function() {
            // respond to changes to the user data
            EventManager.registerListener(constants.EVENT_DISPLAY_MESSAGE_REQUEST, (event) => {
                if (event === "profileUpdateSuccessful" ) {
                    this.View.fetchNotifications((notifications) => {
                        this.updateBadge(notifications.length);
                    });
                }
            });
        },

        updateBadge: function(count) {
            this.$el.find(".fr-badge-notification").empty();
            if (count > 0) {
                this.$el.find(".fr-badge-notification").text(count);
            } else {
                this.$el.find("[data-toggle='popover']").popover("hide");
            }
        },

        initializePopover: function() {
            this.$el.find("[data-toggle='popover']").popover({
                html : true,
                title: "Notifications",
                placement: "bottom",
                trigger: "click"
            });
        },

        showPopover: function(e) {
            e.preventDefault();

            if (this.notifications.length) {
                this.View.render({ el: $('.popover-content') });
                this.$el.find(".popover").addClass("fr-popover-notifications");
            } else {
                this.$el.find("[data-toggle='popover']").popover("hide");
            }
        },

        nonPopoverClickHandler: function(e) {
            $('body').off('click');

            // click handler for closing popover
            $('body').on('click', (e) => {
                if ($(".popover").is(":visible")
                    && $(e.target).data('toggle') !== 'popover'
                    && !$(e.target).hasClass("fa-bell")
                    && $(e.target).parents('.popover.in').length === 0
                ) {
                    $('[data-toggle="popover"]').popover('hide');
                    this.$el.find(".fa-bell").click();
                }
            });
        }
    });

    return new NavigationNotificationsView();
});

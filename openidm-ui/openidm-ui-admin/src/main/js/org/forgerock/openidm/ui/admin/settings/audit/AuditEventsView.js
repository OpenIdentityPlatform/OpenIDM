/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

/*global define*/

define("org/forgerock/openidm/ui/admin/settings/audit/AuditEventsView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditAdminAbstractView",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditEventsDialog"

], function($, _, AuditAdminAbstractView,
            AuditEventsDialog) {

    var AuditEventsView = AuditAdminAbstractView.extend({
        template: "templates/admin/settings/audit/AuditEventsTemplate.html",
        element: "#AuditEventsView",
        noBaseTemplate: true,
        events: {
            "click .editEvent": "editEvent",
            "click .deleteEvent": "deleteEvent",
            "click .addEvent": "addEvent"
        },

        constants: {
            DEFAULT_EVENTS: ["authentication", "access", "activity", "recon", "sync"],
            CRUDPAQ_ACTIONS: ["action", "create", "delete", "patch", "query", "read", "update"],
            CUSTOM_CRUDPAQ_ACTIONS: ["create", "delete", "update", "link", "unlink", "exception", "ignore"]
        },

        render: function (args, callback) {
            this.data = {
                events: []
            };

            this.model = {
                auditData: {},
                events: {},
                EVENT_ACTIONS: {
                    "authentication": [],
                    "access": [],
                    "activity": this.constants.CRUDPAQ_ACTIONS,
                    "custom": this.constants.CRUDPAQ_ACTIONS,
                    "recon": this.constants.CUSTOM_CRUDPAQ_ACTIONS,
                    "sync": this.constants.CUSTOM_CRUDPAQ_ACTIONS
                }

            };

            if (!_.has(args, "model")) {
                this.model.auditData = this.getAuditData();

                if (_.has(this.model.auditData, "extendedEventTypes")) {
                    _.extend(this.model.events, this.model.auditData.extendedEventTypes);
                }
                if (_.has(this.model.auditData, "customEventTypes")) {
                    _.extend(this.model.events, this.model.auditData.customEventTypes);
                }

            } else {
                this.model = _.clone(args.model, true);
            }

            _.each(_.clone(this.model.events, true), function (event, name) {
                event.defaultEvents = _.contains(this.constants.DEFAULT_EVENTS, name);
                event.name = name;
                this.data.events.push(event);
            }, this);

            this.parentRender(_.bind(function() {
                if (callback) {
                    callback();
                }
            }, this));
        },

        reRender: function() {
            this.render({model: this.model});
            this.setEvents(this.model.events, this.$el.find(".alert"));
        },

        addEvent:function(e) {
            e.preventDefault();
            AuditEventsDialog.render({
                    "event": {},
                    "eventName": "",
                    "definedEvents": _.keys(this.model.events),
                    "eventDeclarativeActions": this.model.EVENT_ACTIONS.custom,
                    "newEvent": true
                },
                _.bind(function(results) {
                    this.model.events[results.eventName] = results.data;
                    this.reRender();
                }, this));
        },

        editEvent:function(e) {
            e.preventDefault();

            var eventName = $(e.currentTarget).attr("data-name"),
                dialogConfig = {
                    "eventName": eventName,
                    "event": this.model.events[eventName],
                    "definedEvents": _.keys(this.model.events),
                    "newEvent": false
                };

            if (_.contains(this.constants.DEFAULT_EVENTS, eventName) && eventName !== "custom") {
                dialogConfig.eventDeclarativeActions = this.model.EVENT_ACTIONS[eventName];
                dialogConfig.limitedEdits = true;

            } else {
                dialogConfig.eventDeclarativeActions = this.model.EVENT_ACTIONS.custom;
            }

            // Triggers only apply to recon and activity events, we can enhance this later if it becomes necessary.
            if (_.contains(["activity", "recon"], eventName)) {
                dialogConfig.triggers = {"recon": this.model.EVENT_ACTIONS.recon};
            }

            AuditEventsDialog.render(dialogConfig,
                _.bind(function(results) {
                    this.model.events[results.eventName] = results.data;
                    this.reRender();
                }, this));
        },

        deleteEvent:function(e) {
            e.preventDefault();
            delete this.model.events[$(e.currentTarget).attr("data-name")];
            this.reRender();
        }
    });

    return new AuditEventsView();
});

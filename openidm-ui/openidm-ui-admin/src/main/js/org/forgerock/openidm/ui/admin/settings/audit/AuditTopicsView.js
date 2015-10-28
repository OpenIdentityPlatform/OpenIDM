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
 * Copyright 2015 ForgeRock AS.
 */

/*global define*/

define("org/forgerock/openidm/ui/admin/settings/audit/AuditTopicsView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditAdminAbstractView",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditTopicsDialog",
    "org/forgerock/commons/ui/common/components/ChangesPending"

], function($, _,
            AuditAdminAbstractView,
            AuditTopicsDialog,
            ChangesPending) {

    var AuditTopicsView = AuditAdminAbstractView.extend({
        template: "templates/admin/settings/audit/AuditTopicsTemplate.html",
        element: "#AuditTopicsView",
        noBaseTemplate: true,
        events: {
            "click .editEvent": "editEvent",
            "click .deleteEvent": "deleteEvent",
            "click .addEvent": "addEvent"
        },

        constants: {
            DEFAULT_TOPICS_LIST: ["authentication", "access", "activity", "recon", "sync", "config"],
            DEFAULT_TOPICS: {
                "authentication": {},
                "access": {},
                "activity": {},
                "recon": {},
                "sync": {},
                "config": {}
            },
            CRUDPAQ_ACTIONS: ["action", "create", "delete", "patch", "query", "read", "update"],
            CUSTOM_CRUDPAQ_ACTIONS: ["create", "delete", "update", "link", "unlink", "exception", "ignore"]
        },

        render: function (args, callback) {
            this.data = {
                topics: []
            };

            this.model = {
                auditData: {},
                events: {},
                EVENT_ACTIONS: {
                    "authentication": [],
                    "access": [],
                    "activity": this.constants.CRUDPAQ_ACTIONS,
                    "custom": this.constants.CRUDPAQ_ACTIONS,
                    "config": this.constants.CRUDPAQ_ACTIONS,
                    "recon": this.constants.CUSTOM_CRUDPAQ_ACTIONS,
                    "sync": this.constants.CUSTOM_CRUDPAQ_ACTIONS
                }
            };

            if (!_.has(args, "model")) {
                this.model.auditData = this.getAuditData();

                if (_.has(this.model.auditData, "eventTopics")) {
                    this.model.topics = _.extend(_.clone(this.constants.DEFAULT_TOPICS, true), _.clone(this.model.auditData.eventTopics, true));
                }
            } else {
                this.model = args.model;
            }

            _.each(_.clone(this.model.topics, true), function (event, name) {
                event.defaultEvents = _.contains(this.constants.DEFAULT_TOPICS_LIST, name);
                event.name = name;
                this.data.topics.push(event);
            }, this);

            this.parentRender(_.bind(function() {

                if (!_.has(this.model, "changesModule")) {
                    this.model.changesModule = ChangesPending.watchChanges({
                        element: this.$el.find(".audit-events-alert"),
                        undo: true,
                        watchedObj: $.extend({}, this.model.auditData),
                        watchedProperties: ["eventTopics"],
                        undoCallback: _.bind(function (original) {
                            this.model.auditData.eventTopics = original.eventTopics;
                            this.model.topics = {};

                            if (_.has(this.model.auditData, "eventTopics")) {
                                this.model.topics = _.extend(_.clone(this.constants.DEFAULT_TOPICS, true), _.clone(this.model.auditData.eventTopics, true));
                            }

                            this.reRender();
                        }, this)
                    });
                } else {
                    this.model.changesModule.reRender(this.$el.find(".audit-events-alert"));
                    if (args && args.saved) {
                        this.model.changesModule.saveChanges();
                    }
                }

                if (callback) {
                    callback();
                }
            }, this));
        },

        reRender: function() {
            this.model.auditData.eventTopics = this.model.topics;

            this.setProperties(["eventTopics"], this.model.auditData);
            this.model.changesModule.makeChanges(this.model.auditData);
            this.render({model: this.model});
        },

        addEvent:function(e) {
            e.preventDefault();
            AuditTopicsDialog.render(
                {
                    "event": {},
                    "eventName": "",
                    "isDefault": false,
                    "definedEvents": _.keys(this.model.topics),
                    "eventDeclarativeActions": this.model.EVENT_ACTIONS.custom,
                    "newEvent": true
                },
                _.bind(function(results) {
                    this.model.topics[results.eventName] = results.data;
                    this.reRender();
                }, this)
            );
        },

        editEvent:function(e) {
            e.preventDefault();

            var eventName = $(e.currentTarget).attr("data-name"),
                dialogConfig = {
                    "event": this.model.topics[eventName],
                    "eventName": eventName,
                    "isDefault": _.contains(this.constants.DEFAULT_TOPICS_LIST, eventName),
                    "definedEvents": _.keys(this.model.topics),
                    "newEvent": false
                };

            if (_.contains(this.constants.DEFAULT_TOPICS_LIST, eventName) && eventName !== "custom") {
                dialogConfig.eventDeclarativeActions = this.model.EVENT_ACTIONS[eventName];
                dialogConfig.limitedEdits = true;

            } else {
                dialogConfig.eventDeclarativeActions = this.model.EVENT_ACTIONS.custom;
            }

            // Triggers only apply to recon and activity events, we can enhance this later if it becomes necessary.
            if (_.contains(["activity", "recon"], eventName)) {
                dialogConfig.triggers = {"recon": this.model.EVENT_ACTIONS.recon};
            }

            AuditTopicsDialog.render(dialogConfig,
                _.bind(function(results) {
                    this.model.topics[results.eventName] = results.data;
                    this.reRender();
                }, this));
        },

        deleteEvent:function(e) {
            e.preventDefault();
            delete this.model.topics[$(e.currentTarget).attr("data-name")];
            this.reRender();
        }
    });

    return new AuditTopicsView();
});

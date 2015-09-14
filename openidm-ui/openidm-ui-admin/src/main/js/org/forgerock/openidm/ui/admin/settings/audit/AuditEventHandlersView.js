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
/*jslint es5: true */
define("org/forgerock/openidm/ui/admin/settings/audit/AuditEventHandlersView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditAdminAbstractView",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditEventHandlersDialog",
    "org/forgerock/commons/ui/common/components/ChangesPending"

], function ($, _, AuditAdminAbstractView,
             AuditEventHandlersDialog,
             ChangesPending) {

    var AuditEventHandlersView = AuditAdminAbstractView.extend({
        template: "templates/admin/settings/audit/AuditEventHandlersTemplate.html",
        element: "#AuditEventHandlersView",
        noBaseTemplate: true,
        events: {
            "change .useForQueries": "changeUseForQueries",
            "click .deleteEventHandler": "deleteEventHandler",
            "click .editEventHandler": "editEventHandler",
            "click .addEventHandler": "addEventHandler"
        },
        model: {},
        data: {},

        render: function (args, callback) {
            this.data.definedEventHandlers = [];
            this.data.eventHandlers = [];
            this.model.events = {};
            var ONE_HANDLER_MAX_PROP_NAME = "RepositoryAuditEventHandler",
                allowRepo = true;

            if (args && args.reRender) {
                this.model.auditData = _.clone(args.auditData, true);
            } else {
                this.model.auditData = this.getAuditData();
            }

            if (_.has(this.model.auditData, "extendedEventTypes")) {
                _.extend(this.model.events, this.model.auditData.extendedEventTypes);
            }
            if (_.has(this.model.auditData, "customEventTypes")) {
                _.extend(this.model.events, this.model.auditData.customEventTypes);
            }

            if (_.has(this.model.auditData, "auditServiceConfig") && _.has(this.model.auditData.auditServiceConfig, "handlerForQueries")) {
                this.model.useForQueries = _.clone(this.model.auditData.auditServiceConfig.handlerForQueries, true);
            } else {
                this.model.useForQueries = "";
            }

            if (_.has(this.model.auditData, "eventHandlers")) {
                _.each(_.clone(this.model.auditData.eventHandlers, true), function(handler) {
                    if (handler.name === this.model.useForQueries) {
                        handler.useForQueries = true;
                    }

                    if (_.has(handler, "class")) {
                        handler.class = _.last(handler.class.split("."));

                        if (handler.class === ONE_HANDLER_MAX_PROP_NAME) {
                            allowRepo = false;
                        }
                    }

                    if (_.has(handler, "events")) {
                        handler.events = handler.events.join(", ");
                    }
                    this.data.definedEventHandlers.push(handler);
                }, this);
            }

            if (_.has(this.model.auditData, "auditServiceConfig")) {
                if (_.has(this.model.auditData.auditServiceConfig, "availableAuditEventHandlers")) {
                    _.each(_.clone(this.model.auditData.auditServiceConfig.availableAuditEventHandlers, true), function (handler) {

                        if (_.last(handler.split(".")) === ONE_HANDLER_MAX_PROP_NAME && allowRepo) {
                            this.data.eventHandlers.push({
                                display: _.last(handler.split(".")),
                                value: handler
                            });
                        } else if (_.last(handler.split(".")) !== ONE_HANDLER_MAX_PROP_NAME) {
                            this.data.eventHandlers.push({
                                display: _.last(handler.split(".")),
                                value: handler
                            });
                        }
                    }, this);
                }
            }

            this.parentRender(_.bind(function () {
                if (!_.has(this.model, "changesModule")) {
                    this.model.changesModule = ChangesPending.watchChanges({
                        element: this.$el.find(".audit-event-handlers-alert"),
                        undo: true,
                        watchedObj: _.clone(this.model.auditData, true),
                        watchedProperties: ["auditServiceConfig", "eventHandlers"],
                        undoCallback: _.bind(function (original) {
                            _.each(this.model.changesModule.data.watchedProperties, function (prop) {
                                if (_.has(original, prop)) {
                                    this.model.auditData[prop] = original[prop];
                                } else if (_.has(this.model.auditData, prop)) {
                                    delete this.model.auditData[prop];
                                }
                            }, this);

                            this.setProperties(["auditServiceConfig", "eventHandlers"], this.model.auditData);

                            this.reRender();
                        }, this)
                    });
                } else {
                    this.model.changesModule.reRender(this.$el.find(".audit-event-handlers-alert"));
                    if (args && args.saved) {
                        this.model.changesModule.saveChanges();
                    }
                }

                if (callback) {
                    callback();
                }

            }, this));
        },

        reRender: function () {
            this.render({
                reRender: true,
                auditData: this.model.auditData
            });
            this.setProperties(["auditServiceConfig", "eventHandlers"], this.model.auditData);
            this.model.changesModule.makeChanges(_.clone(this.model.auditData));
        },

        changeUseForQueries: function (e) {
            e.preventDefault();
            var eventHandlerName = $(e.currentTarget).attr("data-name");

            if (!_.has(this.model.auditData, "auditServiceConfig")) {
                this.model.auditData.auditServiceConfig = {};
            }

            this.model.auditData.auditServiceConfig.handlerForQueries = eventHandlerName;

            this.reRender();
        },

        deleteEventHandler: function (e) {
            e.preventDefault();
            var eventHandlerName = $(e.currentTarget).attr("data-name");
            this.model.auditData.eventHandlers.splice(_.findIndex(this.model.auditData.eventHandlers, {"name": eventHandlerName}), 1);
            if (_.has(this.model.auditData, "auditServiceConfig") &&
                _.has(this.model.auditData.auditServiceConfig, "handlerForQueries") &&
                this.model.auditData.auditServiceConfig.handlerForQueries === eventHandlerName &&
                this.model.auditData.eventHandlers.length > 0) {
                this.model.auditData.auditServiceConfig.handlerForQueries = this.model.auditData.eventHandlers[0].name;
            }

            this.reRender();
        },

        editEventHandler: function (e) {
            e.preventDefault();
            var eventHandlerName = $(e.currentTarget).attr("data-name"),
                event = _.findWhere(this.model.auditData.eventHandlers, { "name": eventHandlerName }),
                useForQueries = true;

            if (_.has(this.model.auditData, "auditServiceConfig") &&
                _.has(this.model.auditData.auditServiceConfig, "handlerForQueries") &&
                this.model.auditData.auditServiceConfig.handlerForQueries !== eventHandlerName) {
                useForQueries = false;
            }

            AuditEventHandlersDialog.render(
                {
                    "eventHandlerType": event.class,
                    "eventHandler": event,
                    "newEventHandler": false,
                    "availableEvents": _.keys(this.model.events),
                    "useForQueries": useForQueries,
                    "usedEventHandlerNames": _.map(this.model.auditData.eventHandlers, function (t) {return t.name;})
                },
                _.bind(function (results) {
                    if (results.useForQueries) {
                        this.model.auditData.auditServiceConfig.handlerForQueries = results.eventHandler.name;
                    }

                    var index = _.indexOf(this.model.auditData.eventHandlers, _.findWhere(this.model.auditData.eventHandlers, { "name": results.eventHandler.name }));
                    this.model.auditData.eventHandlers[index] = results.eventHandler;

                    this.reRender();
                }, this)
            );
        },

        addEventHandler: function (e) {
            e.preventDefault();
            var newHandler = this.$el.find("#addAuditModuleSelect").val();
            if (newHandler !== null) {
                AuditEventHandlersDialog.render(
                    {
                        "eventHandlerType": newHandler,
                        "eventHandler": {},
                        "newEventHandler": true,
                        "availableEvents": _.keys(this.model.events),
                        "usedEventHandlerNames": _.map(this.model.auditData.eventHandlers, function (t) {return t.name;})
                    },
                    _.bind(function (results) {
                        if (results.useForQueries) {
                            this.model.auditData.auditServiceConfig.handlerForQueries = results.eventHandler.name;
                        }

                        this.model.auditData.eventHandlers.push(results.eventHandler);

                        this.reRender();
                    }, this)
                );
            }
        }
    });

    return new AuditEventHandlersView();
});

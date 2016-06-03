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
/*jslint es5: true */
define("org/forgerock/openidm/ui/admin/settings/audit/AuditEventHandlersView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditAdminAbstractView",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditEventHandlersDialog",
    "org/forgerock/openidm/ui/admin/delegates/AuditDelegate",
    "org/forgerock/commons/ui/common/components/ChangesPending"

], function ($, _,
             AuditAdminAbstractView,
             AuditEventHandlersDialog,
             AuditDelegate,
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
            this.model.events = [];

            var ONE_HANDLER_MAX_PROP_NAME = "RepositoryAuditEventHandler",
                allowRepo = true,
                eventHandler = {};

            if (args && args.reRender) {
                this.model.auditData = _.clone(args.auditData, true);
            } else {
                this.model.auditData = this.getAuditData();
            }

            AuditDelegate.availableHandlers().then(_.bind(function (data) {
                this.model.availableHandlers = data;

                if (_.has(this.model.auditData, "auditServiceConfig") && _.has(this.model.auditData.auditServiceConfig, "handlerForQueries")) {
                    this.model.useForQueries = _.clone(this.model.auditData.auditServiceConfig.handlerForQueries, true);
                } else {
                    this.model.useForQueries = "";
                }

                if (_.has(this.model.auditData, "eventHandlers")) {
                    this.model.events = _.clone(this.model.auditData.eventHandlers, true);

                    _.each(_.clone(this.model.auditData.eventHandlers, true), function(handler) {

                        eventHandler = _.find(data, {"class": handler.class});

                        handler.isUsableForQueries = eventHandler.isUsableForQueries;

                        var allUsedClasses = _.map(this.model.events, function(event) {
                                return event.class;
                            }),
                            usableForQueriesClasses =  _.map(_.filter(this.model.availableHandlers, function(event) {
                                    return event.isUsableForQueries;
                                }),
                                function(event) {
                                    return event.class;
                                }),
                            usableForQueriesCount = 0;

                        _.each(allUsedClasses, function(classTest){
                            if(usableForQueriesClasses.indexOf(classTest) !== -1){
                                usableForQueriesCount++;
                            }
                        });

                        if (usableForQueriesCount > 1 || !handler.isUsableForQueries) {
                            handler.deletable = true;
                        }


                        if (handler.config.name === this.model.useForQueries) {
                            handler.useForQueries = true;
                        }

                        if (_.has(handler, "class")) {
                            handler.class = _.last(handler.class.split("."));

                            if (handler.class === ONE_HANDLER_MAX_PROP_NAME) {
                                allowRepo = false;
                            }
                        }

                        if (_.has(handler.config, "topics") && _.isArray(handler.config.topics)) {
                            handler.config.topics = handler.config.topics.join(", ");
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

                                this.setProperties(["eventHandlers"], this.model.auditData);
                                this.setUseForQueries(this.model.auditData.auditServiceConfig.handlerForQueries);
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
            }, this));
        },

        reRender: function () {
            this.render({
                reRender: true,
                auditData: this.model.auditData
            });
            this.setProperties(["eventHandlers"], this.model.auditData);
            this.setUseForQueries(this.model.auditData.auditServiceConfig.handlerForQueries);

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

            if ($(e.target).closest("button").hasClass("disabled")) {
                return false;
            }

            var eventHandlerName = $(e.currentTarget).attr("data-name"),
                found = false;

            this.model.auditData.eventHandlers.splice(_.findIndex(this.model.auditData.eventHandlers, {"config": {"name": eventHandlerName}}), 1);

            if (_.has(this.model.auditData, "auditServiceConfig") &&
                _.has(this.model.auditData.auditServiceConfig, "handlerForQueries") &&
                this.model.auditData.auditServiceConfig.handlerForQueries === eventHandlerName &&
                this.model.auditData.eventHandlers.length > 0) {

                _.each(this.model.auditData.eventHandlers, function(eventHandler) {
                    if (_.find(this.model.availableHandlers, {"class": eventHandler.class}).isUsableForQueries) {
                        this.model.auditData.auditServiceConfig.handlerForQueries = eventHandler.config.name;
                        found = true;
                        return false;
                    }
                }, this);

                if (!found && this.model.auditData.eventHandlers.length > 0) {
                    this.model.auditData.auditServiceConfig.handlerForQueries = this.model.auditData.eventHandlers[0].config.name;
                } else if (!found) {
                    this.model.auditData.auditServiceConfig.handlerForQueries = "";
                }
            }

            this.reRender();
        },

        editEventHandler: function (e) {
            e.preventDefault();
            var eventHandlerName = {"config": {"name": $(e.currentTarget).attr("data-name")}},
                event = _.findWhere(this.model.auditData.eventHandlers, eventHandlerName),
                canDisable = !_.findWhere(this.data.definedEventHandlers, eventHandlerName).useForQueries;

            AuditEventHandlersDialog.render(
                {
                    "canDisable": canDisable,
                    "eventHandlerType": event.class,
                    "eventHandler": _.clone(event, true),
                    "newEventHandler": false,
                    "usedEventHandlerNames": _.map(this.model.auditData.eventHandlers, function (t) {return t.config.name;})
                },
                _.bind(function (results) {
                    var index = _.indexOf(this.model.auditData.eventHandlers, _.findWhere(this.model.auditData.eventHandlers, {"config": {"name": results.eventHandler.config.name }}));
                    this.model.auditData.eventHandlers[index] = results.eventHandler;

                    this.reRender();
                }, this)
            );
        },

        addEventHandler: function (e) {
            e.preventDefault();
            var newHandler = this.$el.find("#addAuditModuleSelect").val(),
                found = false,
                // If this is the first event handler then it must be enabled
                canDisable = this.data.eventHandlers.length >= 1;
            if (newHandler !== null) {
                AuditEventHandlersDialog.render(
                    {
                        "canDisable": canDisable,
                        "eventHandlerType": newHandler,
                        "eventHandler": {},
                        "newEventHandler": true,
                        "usedEventHandlerNames": _.map(this.model.auditData.eventHandlers, function (t) {return t.config.name;})
                    },
                    _.bind(function (results) {
                        var currentHandlerName = this.model.auditData.auditServiceConfig.handlerForQueries,
                            currentHandler = _.find(this.model.auditData.eventHandlers, {"config": {"name": currentHandlerName}}),
                            currentHandlerType = {};

                        _.each(this.model.auditData.eventHandlers, function(eventHandler) {
                            if (_.find(this.model.availableHandlers, {"class": eventHandler.class}).isUsableForQueries) {
                                found = true;
                                return false;
                            }
                        }, this);

                        if (_.has(currentHandler, "class")) {
                            currentHandlerType = _.find(this.model.availableHandlers, {"class": currentHandler.class});
                        }

                        // If the current useForQueries property is not set to a valid event handler OR there are no valid event handlers
                        // AND the newly added event handler isUsableForQueries, then set the newly added handler as the handlerForQueries
                        if ((!currentHandlerType.isUsableForQueries || !found) &&
                            _.find(this.model.availableHandlers, {"class": results.eventHandler.class}).isUsableForQueries) {
                            this.model.auditData.auditServiceConfig.handlerForQueries = results.eventHandler.config.name;
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

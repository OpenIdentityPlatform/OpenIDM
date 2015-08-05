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

/*global define */

define("org/forgerock/openidm/ui/admin/settings/AuditView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/openidm/ui/admin/settings/AuditEventsDialog"

], function($, _,
            AdminAbstractView,
            eventManager,
            constants,
            ConfigDelegate,
            InlineScriptEditor,
            AuditEventsDialog) {

    var AuditView = AdminAbstractView.extend({
        template: "templates/admin/settings/AuditTemplate.html",
        element: "#auditContainer",
        noBaseTemplate: true,
        events: {
            "click .edit-event": "editEvent",
            "click .add-event": "addEvent",
            "click .delete-event": "deleteEvent",
            "click .undoEvents": "undoEvents",
            "click .deleteLogType": "deleteLogType",
            "click .addLog": "addLogType",
            "change #logTo input": "updateLogTo",
            "click .undoLogs": "undoLogs",
            "click .undoExceptions": "undoExceptions",
            "click #submitAudit": "save"
        },
        model: {
            logTo: {
                "csv": {
                    "logType": "csv",
                    "location": "",
                    "useForQueries": false,
                    "ignoreLoggingFailures": false,
                    "recordDelimiter": "",
                    "bufferSize" : "",
                    "maxFlushDelay" : "",
                    "csv": true
                },
                "repository": {
                    "logType": "repository",
                    "useForQueries": false,
                    "repository": true,
                    "ignoreLoggingFailures": false,
                    "bufferSize" : "",
                    "maxFlushDelay" : ""
                },
                "router": {
                    "logType": "router",
                    "useForQueries": false,
                    "location": "",
                    "ignoreLoggingFailures": false,
                    "bufferSize" : "",
                    "maxFlushDelay" : ""
                }
            },
            eventDeclarativeActions: {
                "access": [],
                "activity": ["action", "create", "delete", "patch", "query", "read", "update"],
                "recon": ["create", "delete", "update", "link", "unlink", "exception", "report", "noreport", "async", "ignore"],
                "sync": ["create", "delete", "update", "link", "unlink", "exception", "report", "noreport", "async", "ignore"]
            }
        },

        /**
         * If this is a first render define the data object, then fire the postRender
         *
         * @param {object} [args]
         * @param {function} [callback]
         */
        render: function (args, callback) {
            if (!args) {
                ConfigDelegate.readEntity("audit").then(_.bind(function(results) {
                    var temp;

                    this.model.audit = _.clone(results, true);
                    this.model.unaddedEvents = [];

                    this.data = {
                        docHelpUrl: constants.DOC_URL,
                        audit: _.clone(results, true),
                        exceptionFormatter: {},
                        repoAdded: false,
                        unaddedEvents: [
                            "access",
                            "activity",
                            "recon",
                            "sync"
                        ]
                    };

                    if (_.has(results, "exceptionFormatter")) {
                        this.data.exceptionFormatter = results.exceptionFormatter;
                    }

                    this.data.audit.logTo = _.map(results.logTo, function (type) {
                        temp = _.clone(this.model.logTo[type.logType], true);

                        if (type.logType === "repository") {
                            this.data.repoAdded = true;
                        }

                        _.extend(temp, type);
                        return temp;
                    }, this);


                    _.each(_.keys(this.data.audit.eventTypes), function(key) {
                        this.data.unaddedEvents.splice(_.indexOf(this.data.unaddedEvents, key), 1);
                    }, this);

                    this.data.unaddedEvents = _.toArray(_.omit(this.data.unaddedEvents, function (e) {
                        return _.has(this.data.audit.eventTypes, e);
                    }, this));

                    this.model.unaddedEvents = _.clone(this.data.unaddedEvents, true);

                    this.postRender();

                    if (callback) {
                        callback();
                    }
                }, this));
            } else {
                this.data = args;
                this.postRender();
            }
        },

        /**
         * Creates the exception formatter script and fires the callback.
         */
        postRender: function() {
            this.parentRender(_.bind(function() {
                this.model.exceptionFormatterScript = InlineScriptEditor.generateScriptEditor({
                    "element": this.$el.find("#exceptionFormatterScript"),
                    "eventName": "exceptionFormatterScript",
                    "disableValidation": true,
                    "onChange": _.bind(function(e) {
                        var script = this.model.exceptionFormatterScript.generateScript();

                        if (_.isNull(script)) {
                            if (_.has(this.data.audit, "exceptionFormatter")) {
                                delete this.data.audit.exceptionFormatter;
                            }
                        } else {
                            this.data.audit.exceptionFormatter = script;
                        }

                        if (_.isEqual(this.model.audit.exceptionFormatter, this.data.audit.exceptionFormatter)) {
                            this.$el.find("#exceptionFormatter .alert-warning").hide();
                        } else {
                            this.$el.find("#exceptionFormatter .alert-warning").show();
                        }
                    }, this),
                    "scriptData": this.data.exceptionFormatter
                });
            }, this));
        },

        /**
         * Opens the Audit Events Dialog for editing with the provided event.
         * @param e
         */
        editEvent: function(e) {
            e.preventDefault();
            var eventName = $(e.currentTarget).closest(".list-group-item").attr("eventName");

            AuditEventsDialog.render({
                    "event": this.data.audit.eventTypes[eventName],
                    "eventName": eventName,
                    "eventDeclarativeActions": this.model.eventDeclarativeActions[eventName],
                    "titlePrefix": "Edit Event"
                },
                _.bind(function(results) {
                    this.data.audit.eventTypes[results.eventName] = results.data;
                    this.checkEventChanges();
                }, this));
        },

        /**
         * Opens a new Audit Events Dialog with the provided event.  If the data is saved
         * the data object is updated and the UI is rerendered.
         * @param e
         */
        addEvent: function(e) {
            e.preventDefault();
            var event = this.$el.find(".eventList").val();

            AuditEventsDialog.render({
                    "event": {},
                    "eventName": event,
                    "eventDeclarativeActions": this.model.eventDeclarativeActions[event],
                    "titlePrefix": "Add Event"
                },
                _.bind(function(results) {
                    var i = _.indexOf(this.data.unaddedEvents, results.eventName);
                    this.data.unaddedEvents.splice(i, 1);
                    this.data.audit.eventTypes[results.eventName] = results.data;
                    this.render(this.data, this.model.callback);
                    this.checkEventChanges();
                }, this));
        },

        /**
         * The provided event is removed from the added events array and added to the unadded events array. The UI is rerendered.
         * @param e
         */
        deleteEvent: function(e) {
            e.preventDefault();
            var event = $(e.currentTarget).closest(".list-group-item").attr("eventName");

            this.data.unaddedEvents.push(event);
            delete this.data.audit.eventTypes[event];
            this.render(this.data, this.model.callback);
            this.checkEventChanges();
        },

        checkEventChanges: function() {
            if (_.isEqual(this.model.audit.eventTypes, this.data.audit.eventTypes)) {
                this.$el.find("#eventTypes .alert-warning").hide();
            } else {
                this.$el.find("#eventTypes .alert-warning").show();
            }
        },

        undoEvents: function(e) {
            e.preventDefault();
            this.data.audit.eventTypes = _.clone(this.model.audit.eventTypes, true);
            this.data.unaddedEvents = _.clone(this.model.unaddedEvents, true);

            this.render(this.data, this.model.callback);
        },

        /**
         * A new log is copied from the model data and added to the log to array.
         * If no other logs exist the new log is given the useForQueries property as if any logTos are defined one MUST have the property
         * If the new log is a repository a flag is turned on so no others can be added.
         *
         * The UI is rerendered
         *
         * @param e
         */
        addLogType: function(e) {
            e.preventDefault();

            var newLog = this.$el.find(".logTypes").val();

            this.data.audit.logTo.push(_.clone(this.model.logTo[newLog]));

            if (this.data.audit.logTo.length ===1) {
                this.data.audit.logTo[0].useForQueries = true;
            }

            if (newLog === "repository") {
                this.data.repoAdded = true;
            }

            this.render(this.data);
            this.checkLogChanges();
        },

        /**
         * The selected log is removed from the logTo array.
         * If the log has the useForQueries property, the first log in the array is given that property.
         * If the log deleted was a repository than the flag is switched off so users can re-add it.
         *
         * The UI is re-rendered
         *
         * @param e
         */
        deleteLogType: function(e) {
            e.preventDefault();
            var index = $(e.currentTarget).closest(".log").attr("data-index");

            if (index > -1) {
                if (this.data.audit.logTo[index].useForQueries && this.data.audit.logTo.length > 0) {
                    this.data.audit.logTo[0].useForQueries = true;
                }

                if (this.data.audit.logTo[index].repository) {
                    this.data.repoAdded = false;
                }

                this.data.audit.logTo.splice(index, 1);

                this.render(this.data);
                this.checkLogChanges();
            }
        },

        /**
         * When a useForQueries radio button is clicked remove the property from other logs, update the logTo property useForQueries
         * of the selected log type or designate that property to the first log if the selected one is unavailable.
         * @param e
         */
        updateLogTo: function(e) {
            var field = $(e.currentTarget).attr("data-field"),
                fieldType = $(e.currentTarget).attr("data-type"),
                index = parseInt($(e.currentTarget).closest(".log").attr("data-index"), 10);

            if (fieldType === "boolean") {
                this.data.audit.logTo[index][field] = $(e.currentTarget).prop("checked");
            } else {
                this.data.audit.logTo[index][field] = $(e.currentTarget).val();
            }

            if (field === "useForQueries") {
                _.each(this.data.audit.logTo, function(log, i) {
                    if (i !== index) {
                        this.data.audit.logTo[i].useForQueries = false;
                    }
                }, this);
            }

            this.checkLogChanges();
        },

        clean: function(logs) {
            var cleanedLogs = [],
                keys;
            _.each(_.clone(logs, true), function(log){
                keys = _.keys(_.omit(log, "csv", "repository"));

                _.each(keys, function(key) {
                    if ((_.isString(log[key]) && log[key].length === 0) || _.isNull(log[key]) || _.isUndefined(log[key])) {
                        delete log[key];
                    }
                });
                cleanedLogs.push(log);
            }, this);
            return cleanedLogs;
        },

        checkLogChanges: function() {
            if (_.isEqual(this.clean(this.model.audit.logTo), this.clean(this.data.audit.logTo))) {
                this.$el.find("#logTo .alert-warning").hide();
            } else {
                this.$el.find("#logTo .alert-warning").show();
            }
        },

        undoLogs: function(e) {
            e.preventDefault();
            var temp;

            this.data.audit.logTo = _.map(_.clone(this.model.audit.logTo, true), function (type) {
                temp = _.clone(this.model.logTo[type.logType], true);

                if (type.logType === "repository") {
                    this.data.repoAdded = true;
                }

                _.extend(temp, type);
                return temp;
            }, this);

            this.render(this.data);
        },

        undoExceptions: function(e) {
            e.preventDefault();
            this.data.audit.exceptionFormatter = _.clone(this.model.audit.exceptionFormatter, true);
            this.render(this.data);
        },

        /**
         * Formats the eventTypes, exceptionFormatter and logTo values and saves them to the audit entity
         */
        save: function() {
            _.each(this.data.audit.logTo, function(log, i) {
                this.data.audit.logTo = this.clean(this.data.audit.logTo);
            }, this);
            ConfigDelegate.updateEntity("audit", this.data.audit).then(_.bind(function() {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "auditSaveSuccess");
                this.render();
            }, this));
        }
    });

    return new AuditView();
});

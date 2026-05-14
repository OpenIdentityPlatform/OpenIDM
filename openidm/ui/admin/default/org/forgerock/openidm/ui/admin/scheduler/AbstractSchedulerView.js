"use strict";

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

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
 * Copyright 2016 ForgeRock AS.
 */

define(["jquery", "lodash", "form2js", "handlebars", "moment", "moment-timezone", "cron", "bootstrap-datetimepicker", "org/forgerock/openidm/ui/admin/util/AdminAbstractView", "org/forgerock/commons/ui/common/main/EventManager", "org/forgerock/commons/ui/common/main/ValidatorsManager", "org/forgerock/commons/ui/common/util/Constants", "org/forgerock/openidm/ui/admin/delegates/SchedulerDelegate", "org/forgerock/openidm/ui/admin/util/InlineScriptEditor", "org/forgerock/openidm/ui/admin/util/AdminUtils", "org/forgerock/commons/ui/common/components/ChangesPending", "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate", "org/forgerock/openidm/ui/common/delegates/ConfigDelegate", "org/forgerock/openidm/ui/admin/util/Scheduler", "org/forgerock/commons/ui/common/main/Router"], function ($, _, form2js, Handlebars, moment, momentTimezone, cron, Datetimepicker, AdminAbstractView, eventManager, ValidatorsManager, constants, SchedulerDelegate, InlineScriptEditor, AdminUtils, ChangesPending, ConnectorDelegate, ConfigDelegate, scheduler, router) {
    /**
     * @exports AbstractSchedulerView
     */
    var AbstractShedulerView = AdminAbstractView.extend({
        template: "templates/admin/scheduler/EditSchedulerViewTemplate.html",
        events: {
            "click .btn-save": "saveSchedule",
            "click .undo": "resetSchedule",
            "click .btn-cancel": "resetSchedule",
            "click #deleteSchedule": "deleteSchedule",
            "change .schedule-form-element :input": "formChangeHandler",
            "change .cron-editor-container": "cronChangeHandler",
            "change .script-editor-container": "iseChangeHandler",
            "click .cron-controls": "toggleCronElement",
            "click .advanced-options-toggle": "toggleAdvancedLinkText",
            "dp.change .date-time-picker": "dateTimeChangeHandler"
        },
        data: {
            misfirePolicyOptions: ["fireAndProceed", "doNothing"],
            invokeServiceOptions: [{ name: $.t("templates.scheduler.provisionerType"), value: "provisioner" }, { name: $.t("templates.scheduler.scriptType"), value: "script" }, { name: $.t("templates.scheduler.syncType"), value: "sync"
                // "org.forgerock.openidm.taskScanner"
            }],
            invokeLogLevelOptions: ["trace", "debug", "info", "warn", "error", "fatal"],
            timzoneOptions: moment.tz.names()
        },
        partials: ["partials/scheduler/_scheduleForm.html", "partials/scheduler/_invokeContextSyncProv.html"],

        /**
         * Kicks off the theme bootstrap process and loads all the html and data for the schedule form
         * @callback renderFormCallback
         * @return {undefined}
         */
        renderForm: function renderForm(callback) {
            var _this = this;

            _.bindAll(this);
            this.addTimeZoneFormData(this.data.schedule);
            this.parentRender(function () {
                var watchedObj = _.cloneDeep(_this.schedule);
                _this.undelegateEvents();
                _this.changeWatcher = ChangesPending.watchChanges({
                    element: ".changes-pending-container",
                    undo: true,
                    watchedObj: watchedObj,
                    watchedProperties: _.keys(watchedObj)
                });

                _this.addDateTimePickers();

                _this.addInvokeContextInlineScriptEditor(function () {
                    _this.addCronEditor();
                    _this.$el.find(".schedule-form").selectize();
                    _this.delegateEvents();

                    if (callback) {
                        callback();
                    }
                });
            });
        },


        /**
         * Load the initial timeZone value and timeZone options
         * into the form data in preparation for handlebars compile
         * @param {object} schedule
         */
        addTimeZoneFormData: function addTimeZoneFormData(schedule) {
            var currentTimezone = schedule.timeZone || moment.tz.guess();
            this.data.timeZone = currentTimezone;
            this.data.timeZoneOptions = moment.tz.names();
        },


        /**
         * Initializes the inline script editor and adds it as `this.editor` on
         * the instance of `AbstractSchedulerView`.
         * @callback invokeContextCallback
         */
        addInvokeContextInlineScriptEditor: function addInvokeContextInlineScriptEditor(callback) {
            var _this2 = this;

            var element = this.$el.find(".script-editor-container"),
                options = {
                element: element,
                noValidation: true,
                onChange: function onChange() {
                    element.trigger("change");
                },
                onAddPassedVariable: function onAddPassedVariable() {
                    element.trigger("change");
                },
                onLoadComplete: function onLoadComplete() {

                    _this2.$el.find(".script-body select").selectize();
                    var serviceType = _this2.serviceType(_this2.data.schedule.invokeService);
                    _this2.updateInvokeContextVisibleElement(serviceType);
                    if (callback) {
                        callback();
                    }
                },
                scriptData: this.data.schedule.invokeContext.script
            };
            this.editor = InlineScriptEditor.generateScriptEditor(options);
        },


        /**
         * Initializes bootstrap-datetimepicker and sets the default/ min date
         * constraints on the pickers
         */
        addDateTimePickers: function addDateTimePickers() {
            var schedule = this.data.schedule,
                pickerOptions = {
                sideBySide: true,
                useCurrent: false,
                icons: {
                    time: 'fa fa-clock-o',
                    date: 'fa fa-calendar',
                    up: 'fa fa-chevron-up',
                    down: 'fa fa-chevron-down',
                    previous: 'fa fa-chevron-left',
                    next: 'fa fa-chevron-right',
                    today: 'fa fa-crosshairs',
                    clear: 'fa fa-trash',
                    close: 'fa fa-remove'
                }
            },
                $endTimeInput = this.$el.find("#scheduleEndTime");

            this.$el.find(".date-time-picker").each(function (index) {
                var val = $(this).val();
                if (val) {
                    pickerOptions.defaultDate = moment(val);
                } else {
                    pickerOptions.minDate = moment().format();
                }
                $(this).val("");
                $(this).datetimepicker(pickerOptions);
            });

            // if end time not specified, set min end time to start time
            if (schedule.startTime && !schedule.endTime) {
                $endTimeInput.data("DateTimePicker").minDate(moment(schedule.startTime));
            }
        },


        /**
         * Shows/Hides the current context input based on the current service type
         * @param {string} serviceType -- The properly formated invokeService type (e.g. 'sync', 'provisioner',etc.)
         * @return {undefined}
         */
        updateInvokeContextVisibleElement: function updateInvokeContextVisibleElement(serviceType) {
            this.hide(".invoke-context-part");
            var selector = void 0;
            switch (serviceType) {
                case "provisioner":
                    this.showLiveSyncOptions();
                    break;
                case "sync":
                    this.showMappingOptions();
                    break;
                case "script":
                    this.showInlineScriptEditor();
                    break;
                default:
                    _.noop();
                    break;
                // case "taskScanner":
            }
        },


        /**
         * Shows the inline script editor
         * @return {Object} DomElement -- The dom element that was just shown
         */
        showInlineScriptEditor: function showInlineScriptEditor() {
            return this.show(".invoke-context-ise-container");
        },


        /**
         * Shows the mapping select loaded with the correct options
         * @return {Object} DomElement -- The dom element that was just shown
         */
        showMappingOptions: function showMappingOptions() {
            var _this3 = this;

            return this.fetchMappings().then(function (response) {
                return _this3.updateSourceMappingSelect(response);
            });
        },


        /**
         * Shows the mapping select loaded with the correct options
         * @return {object} DomElement -- The dom element that was just shown
         */
        showLiveSyncOptions: function showLiveSyncOptions() {
            var _this4 = this;

            return this.fetchSources().then(function (response) {
                return _this4.updateSourceMappingSelect(response);
            });
        },


        /**
         * @typedef {object} resourceMapping
         * @property {string} type -- string specifying either source or mapping
         * @property {array} options -- The options
         */

        /**
         * Shows the mapping/sources select loaded with the correct options
         * @param {object} resourceMapping
         * @return {object} data needed by the invokeContextSyncProv partial
         */
        createSourceMappingData: function createSourceMappingData(resourceMapping) {
            var type = resourceMapping.type,
                action = { mapping: "reconcile", source: "liveSync" },
                data = {
                action: action[type],
                type: type,
                resourceMapping: this.schedule.invokeContext[type],
                resourceMappingOptions: resourceMapping.options
            };

            return data;
        },


        /**
         * Creats an updated data object for the source/mapping select element
         * reloads the html for the element with updated data,
         * shows the element and selectizes
         * @param {object} resourceMapping
         * @return {object} domElement
         */
        updateSourceMappingSelect: function updateSourceMappingSelect(resourceMapping) {
            var partial = "scheduler/_invokeContextSyncProv",
                sourceMappingSelect = ".invoke-context-well-select-container",
                data = this.createSourceMappingData(resourceMapping),
                html = this.loadPartial(partial, data);

            this.$el.find(sourceMappingSelect).html(html);
            this.show(sourceMappingSelect);
            // call selectize if not selectized
            if (!this.$el.find(sourceMappingSelect).find(".selectize-control").length) {
                this.$el.find(sourceMappingSelect).find("select").selectize();
            }

            // if there is no resourceMapping set and data.type is "mapping" we know this is a new schedule
            // we need to set the default mapping to be equal first value in the options array
            if (!data.resourceMapping && data.type === "mapping" && resourceMapping.options.length > 0) {
                this.data.schedule.invokeContext.mapping = resourceMapping.options[0];
            }

            return this.$el.find(sourceMappingSelect);
        },


        /**
         * makes a call to the connector delegate and
         * returns a promise that resolves to a resource mapping options object
         * @return {promise} resourceMapping
         */
        fetchSources: function fetchSources() {
            return ConnectorDelegate.currentConnectors().then(this.formatSourceOptions);
        },


        /**
         * properly formats the connector delegate response for current connectors
         * into a list of options that can be used in the scheduler
         * @param {array} currentConnectors -- connector objects returned by
         * connector delegate call
         * @param {array} currentConnectors.objectTypes -- array containing the list
         * of available object types (e.g. "objectTypes": [ "__ALL__", "account", "group" ])
         * @return {object} resourceMapping
         */
        formatSourceOptions: function formatSourceOptions(currentConnectors) {
            var options = currentConnectors.map(function (connector) {
                return connector.objectTypes.map(function (type) {
                    return ["system", connector.name, type].join("/");
                });
            }).reduce(function (a, b) {
                return a.concat(b);
            });
            return { type: "source", options: options };
        },


        /**
         * makes a call to the config delegate and
         * returns a promise that resolves to a resource mapping options object
         * @return {promise} resourceMapping
         */
        fetchMappings: function fetchMappings() {
            return ConfigDelegate.readEntity("sync").then(this.formatMappingOptions);
        },


        /**
         * properly formats the config delegate response to request for 'sync'
         * into a list of options that can be used in the scheduler
         * @param {object} config -- Object returned by config delegate
         * @param {array} config.mappings -- array listing the mappings
         * @return {object} resourceMapping
         */
        formatMappingOptions: function formatMappingOptions(config) {
            var options = config.mappings.map(function (mapping) {
                return mapping.name;
            });
            return { type: "mapping", options: options };
        },


        /**
         * Checks if the inline script editor is loaded
         * @return {boolean}
         */
        inlineEditorIsLoaded: function inlineEditorIsLoaded() {
            return !_.isUndefined(this.editor) && !_.isUndefined(this.editor.getInlineEditor());
        },


        /**
         * Set the inline script content
         * @param {string} value
         */
        setCmValue: function setCmValue(value) {
            if (this.inlineEditorIsLoaded()) {
                this.editor.getInlineEditor().setValue(value);
            }
        },


        /**
         * Get the inline script content
         * @return {object} script
         */
        getIseValue: function getIseValue() {
            if (this.inlineEditorIsLoaded()) {
                return this.editor.generateScript();
            }
        },


        /**
         * Invoked on change event on a generic form element (not cron, or inline script editor)
         * extracts the property name and value which should be updated from the
         * triggering dom element and calls
         * `updateSchedule` with those as arguments
         * @param {object} event -- triggering event
         */
        formChangeHandler: function formChangeHandler(event) {
            var prop = $(event.target).attr("name"),
                value = $(event.target).val();
            if ($(event.target).attr("type") === "checkbox") {
                value = $(event.target).prop("checked");
            }
            // kick off invokeContext changes on invokeService change
            if (prop === "mapping" || prop === "source") {
                this.setInvokeContextObject(prop, value);
            } else {
                this.updateSchedule(prop, value);
            }
        },


        /**
         * Invoked on change event from the cron element, this method
         * reads and syncs the current cron expression and calls
         * `updateSchedule`
         * @param {object} event -- triggering event
         */
        cronChangeHandler: function cronChangeHandler(event) {
            var prop = "schedule",
                value = this.getAndSyncCronValue(event);

            this.updateSchedule(prop, value);
        },


        /**
         * Invoked on change event from the inline script editor, this method
         * generates a script object and calls `setInvokeContextObject`
         * @param {object} event -- triggering event
         */
        iseChangeHandler: function iseChangeHandler(event) {
            var prop = "script",
                value = this.getIseValue();

            this.setInvokeContextObject(prop, value);
        },


        /**
         * Invoked on dp.change event (bootstrap-datetimepicker element), this method
         * grabs the dateTime string, formats it into ISO8601 and calls `updateSchedule`
         * @param {object} event -- triggering event
         */
        dateTimeChangeHandler: function dateTimeChangeHandler(event) {
            event.preventDefault();
            // if start date was picked, end date should be min date updated
            var $target = $(event.target),
                localFormat = "YYYY-MM-DD[T]HH:mm:ss",
                value = moment($target.val()).format(localFormat),
                name = $target.attr("name");
            if (name === "startTime") {
                $("#scheduleEndTime").data("DateTimePicker").minDate(event.date);
            }
            this.updateSchedule(name, value);
        },

        /**
         * Properly generates an `invokeContext` object based on the prop param
         * and pases to `updateSchedule`
         * @param {string} prop -- the property name to be updated
         * (i.e. source, mapping, script, taskScanner)
         * @param {(string|object)} value -- recon and liveSync services expect
         * a string (mapping/source) and the script and taskScanner expect objects
         */
        setInvokeContextObject: function setInvokeContextObject(prop, value) {
            var invokeContext = _defineProperty({}, prop, value);
            if (prop === "source") {
                _.set(invokeContext, "action", "liveSync");
            } else if (prop === "mapping") {
                _.set(invokeContext, "action", "reconcile");
            } else if (prop === "script") {
                // do nothing
            } else {
                throw new TypeError("unknown invoke context type");
            }

            this.updateSchedule("invokeContext", invokeContext);
        },


        /**
         * Generates a `invokeContext` object based on the prop param
         * and pases to `setInvokeContextObject`.
         * @param {string} invokeService -- specifies the type of default object
         */
        createDefaultInvokeContext: function createDefaultInvokeContext(invokeService) {
            var prop = void 0,
                value = void 0;

            switch (invokeService) {
                case "sync":
                    prop = "mapping";
                    value = this.$el.find("#select-mapping").val();
                    break;
                case "provisioner":
                    prop = "source";
                    value = this.$el.find("#select-source").val();
                    break;
                case "script":
                    prop = "script";
                    value = {};
                    break;
            }

            this.setInvokeContextObject(prop, value);
        },


        /**
         * Replaces the data.schedule member of the view instance with an new copy
         * that has prop/value pair.
         * Kicks off invokeService/invokeContext Dom changes (if necessary)
         * Checks changes and shows/hide, disable/enables appropriate dom elemnts
         * @param {string} prop -- the property of the Scheduler/Job that should be updated
         * @param {string|object} value -- updated value for prop
         */
        updateSchedule: function updateSchedule(prop, value) {
            if (prop === "invokeService") {
                var serviceType = value;
                this.updateInvokeContextVisibleElement(serviceType);
                if (serviceType !== this.schedule.invokeService) {
                    this.createDefaultInvokeContext(serviceType);
                } else {
                    this.data.schedule.invokeContext = this.schedule.invokeContext;
                }
            }

            var mutatedSchedule = _.set(this.data.schedule, prop, value);
            this.changeWatcher.makeChanges(mutatedSchedule);

            if (this.changeWatcher.isChanged() || this.isNew) {
                this.enable(".save-cancel-btn");
                this.data.schedule = _.cloneDeep(mutatedSchedule);
            } else {
                this.disable(".save-cancel-btn");
            }
        },


        /**
         * Invoked on user click on cancel button or undo in changes pending widget.
         * Replaces the data.schedule member of the view instance with an new copy
         * cloned from the original version pulled down from the server.
         * Re-renders form
         * @param {object} event -- the triggering event
         */
        resetSchedule: function resetSchedule(event) {
            var _this5 = this;

            event.preventDefault();
            this.data.schedule = _.cloneDeep(this.schedule);
            this.renderForm(function () {
                _this5.disable(".save-cancel-btn");
                _this5.$el.find("#select-invokeService").val(_this5.schedule.invokeService);
            });
        },


        /**
         * Invoked on user click on save button.
         * sends the `data.schedule` object via SchedulerDelegate
         * calls create/save depending on the value of `isNew`
         * On success, saves changes on changesPending widget or
         * redirects to `Edit` page for schedule (depending on context)
         * on delegate call failure triggers error message.
         * @param {object} event -- the triggering event
         */
        saveSchedule: function saveSchedule(event) {
            if (event) {
                event.preventDefault();
            }
            if (this.isNew) {
                SchedulerDelegate.addSchedule(this.data.schedule).then(this.createScheduleSuccess, this.saveScheduleError);
            } else {
                SchedulerDelegate.saveSchedule(this.schedule._id, this.data.schedule).then(this.saveScheduleSuccess, this.saveScheduleError);
            }
        },


        /**
         * Invoked on user click on delete button.
         * sends `DELETE` request to server via SchedulerDelegate
         * redirects to schedules page on success
         * triggers error on delegate call failure.
         * @param {object} event -- the triggering event
         */
        deleteSchedule: function deleteSchedule(event) {
            var _this6 = this;

            event.preventDefault();
            var dialogText = $("<p>", { "text": $.t("templates.scheduler.schedule") }),
                scheduleId = this.schedule._id;

            AdminUtils.confirmDeleteDialog(dialogText, function () {
                SchedulerDelegate.deleteSchedule(scheduleId).then(_this6.deleteScheduleSuccess, _this6.deleteScheduleError);
            });
        },


        /**
         * triggers redirect to edit schedule page for schedule with id returned by response
         * @param {object} response -- response object from delegate call
         */
        createScheduleSuccess: function createScheduleSuccess(response) {
            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "scheduleSaved");
            eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {
                route: router.configuration.routes.editSchedulerView,
                args: [response._id, ""]
            });
        },


        /**
         * saves changes on ChangesPending widget. Disables cancel-save buttons
         * @param {object} response -- response object from delegate call
         */
        saveScheduleSuccess: function saveScheduleSuccess(response) {
            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "scheduleSaved");
            this.changeWatcher.saveChanges();
            this.disable(".save-cancel-btn");
        },

        /**
         * Sends error message via `EventManager`
         * @param {object} error -- error returned from delegate call
         */
        saveScheduleError: function saveScheduleError(error) {
            if (error) {
                console.log(error);
            }
            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "scheduleSaveFailed");
        },


        /**
         * Sends confirmation message of successful deletion and redirects to
         * Schedules list page
         * @param {object} response -- response object from delegate call
         */
        deleteScheduleSuccess: function deleteScheduleSuccess(response) {
            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "scheduleDeleted");
            eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, { route: router.configuration.routes.scheduler });
        },


        /**
         * Sends error message via `EventManager`
         * @param {object} error -- error returned from delegate call
         */
        deleteScheduleError: function deleteScheduleError(error) {
            if (error) {
                console.log(error);
            }
            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "scheduleDeletedFailed");
        },


        /**
         * Initializes cron builder functionality via jQuery plugin and sets initial value
         * enables/disables cron builder element based on expression complexity
         */
        addCronEditor: function addCronEditor() {
            var defaultCronValue = void 0,
                cronExpr = this.data.schedule.schedule;

            // generate the cron html and add it to the dom
            // this.$el.find(cronEditorContainer).html(loadedPartial);
            // initialize jquery cron here
            this.cronBuilder = this.$el.find(".cronField").cron();
            // set the cron editor value
            defaultCronValue = this.cronBuilder.cron("value", this.cronBuilder.cron("convertCronVal", cronExpr));

            //jquery-cron returns false if the cron value being set is too
            // complex for it to handle or if it is not a valid cron value
            if (defaultCronValue) {
                this.enableCronBuilder();
            } else {
                this.disableCronBuilder();
            }

            this.$el.find(".cronField select").selectize();
        },


        /**
         * Grabs cron value from builder or expression input based on triggering
         * element.
         * Validates cron expression via SchedulerDelegate call.
         * Syncs cron expression value for builder/raw expression text input
         * @param {object} event -- triggering event
         */
        getAndSyncCronValue: function getAndSyncCronValue(event) {
            var _this7 = this;

            var $target = $(event.target);
            if ($target.hasClass("complexExpression")) {
                var cronExpr = $target.val();

                SchedulerDelegate.validate(cronExpr).then(function (response) {
                    if (!response.valid) {
                        // validations manager invalidate the biz
                        _this7.disable(".save-cancel-btn");
                        // validations manager invalidate the biz
                        ValidatorsManager.validationFailed($target, ['cron expression invalid']);
                    } else {
                        ValidatorsManager.validationSucceeded($target);
                    }
                });
                this.updateCronBuilder(cronExpr);
                return cronExpr;
            } else {
                try {
                    var _cronExpr = this.cronBuilder.cron("convertCronVal", this.cronBuilder.cron("value"));
                    SchedulerDelegate.validate(_cronExpr).then(function (response) {
                        if (!response.valid) {
                            _this7.disable(".save-cancel-btn");
                            // validations manager invalidate the biz
                        }
                    });
                    this.$el.find(".complexExpression").val(_cronExpr);
                    return _cronExpr;
                } catch (err) {
                    console.log(err);
                    return this.schedule.schedule;
                }
            }
        },


        /**
         * Attempt to set expression value on cron builder (jQuery cron plugin).
         * If expression is too complex for the builder, disable the builder
         * @param {string} cronExpression -- Quartz format cron expression
         */
        updateCronBuilder: function updateCronBuilder(cronExpression) {
            if (this.cronBuilder) {
                var cronVal = this.cronBuilder.cron("value", cronExpression);
                if (!cronVal) {
                    this.disableCronBuilder();
                } else {
                    this.enableCronBuilder();
                }
            }
        },


        /**
         * Toggle betweeen cron builder and raw text input.
         * @param {object} event -- triggering event
         */
        toggleCronElement: function toggleCronElement(event) {
            event.preventDefault();
            this.$el.find(".cron-builder").toggleClass("hidden");
        },


        /**
         * Allow user to access the cron builder.
         */
        enableCronBuilder: function enableCronBuilder() {
            this.enable(".btn-cron-basic");
            this.hide(".advanced-cron-msg");
        },


        /**
         * disable user access to the cron builder.
         * Show "too advanced" message.
         */
        disableCronBuilder: function disableCronBuilder() {
            this.disable(".btn-cron-basic");
            this.hide(".cron-builder-basic");
            this.show(".cron-builder-advanced");
            this.show(".advanced-cron-msg");
        },


        /**
         * Compile a handlebars partial and return data
         * @param {string} partial -- handlebars registered partial name
         * @param {object} data -- data needed by the partial during compilation
         * @return {string} html
         */
        loadPartial: function loadPartial(partial, data) {
            var template = Handlebars.compile("{{> " + partial + "}}");
            return template(data);
        },


        /**
         * Provide a declarative method for hiding scoped dom elements
         * @param {(string|jQuery)} selector -- element to hide
         * @return {object} domElement -- element that was hidden
         */
        hide: function hide(selector) {
            return this.$el.find(selector).addClass("hidden");
        },


        /**
         * Provide a declarative method for showing scoped dom elements
         * @param {(string|jQuery)} selector -- element to show
         * @return {object} domElement -- element that was shown
         */
        show: function show(selector) {
            return this.$el.find(selector).removeClass("hidden");
        },


        /**
         * Provide a declarative method for disabling scoped dom elements
         * @param {(string|jQuery)} selector -- element to disable
         * @return {object} domElement -- element that was disabled
         */
        disable: function disable(selector) {
            return this.$el.find(selector).prop("disabled", true);
        },


        /**
         * Provide a declarative method for enabling scoped dom elements
         * @param {(string|jQuery)} selector -- element to disable
         * @return {object} domElement -- element that was disabled
         */
        enable: function enable(selector) {
            return this.$el.find(selector).prop("disabled", false);
        },


        /**
         * Set text to either 'show' or 'hide' based on `aria-expanded` attribute
         * of the triggering event
         * @param {object} event -- triggering event
         */
        toggleAdvancedLinkText: function toggleAdvancedLinkText(event) {
            var action = "text",
                selector = event.target,
                expanded = $(event.target).attr("aria-expanded").toString(),
                swapText = {
                "false": "hide",
                "true": "show"
            };

            this.$el.find(event.target).text($.t("common.form." + swapText[expanded] + "AdvancedOptions"));
        },


        /**
         * Strip off the prefixed namespace from invokeService type.
         * If the service type can't match the prefix it will return
         * the passed string.
         * @param {string} invokeService
         * @return {string} invokeServiceType
         */
        serviceType: function serviceType(invokeService) {
            if (invokeService.match(/org\.forgerock\.openidm/)) {
                return _.last(invokeService.split("."));
            }
            return invokeService;
        }
    });

    return AbstractShedulerView;
});

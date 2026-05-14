"use strict";

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
 * Copyright 2015-2016 ForgeRock AS.
 */

define(["jquery", "underscore", "handlebars", "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView", "org/forgerock/openidm/ui/common/delegates/ConfigDelegate", "org/forgerock/openidm/ui/admin/util/ScriptDialog", "org/forgerock/commons/ui/common/util/Constants", "org/forgerock/commons/ui/common/main/EventManager", "org/forgerock/commons/ui/common/components/ChangesPending"], function ($, _, handlebars, MappingAdminAbstractView, ConfigDelegate, ScriptDialog, constants, eventManager, ChangesPending) {
    var IndividualRecordValidationView = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/association/IndividualRecordValidationViewTemplate.html",
        element: "#objectFiltersView",
        noBaseTemplate: true,
        events: {
            "change .validation-select": "changeMappingEventDisplay",
            "click #individualValidationSave": "individualValidationSave",
            "click .edit-btn": "editScript",
            "change .preference-check": "changePreference"
        },
        partials: ["partials/mapping/association/_individualPreferences.html", "partials/mapping/association/_singleScript.html"],
        model: {},

        /**
         * Renders the individual record validation this includes getting the current mapping and list of preferences
         */
        render: function render() {
            var _this = this;

            this.model.sync = this.getSyncConfig();
            this.model.mapping = this.getCurrentMapping();

            this.data.sourceDefinition = this.getDisplayDetails(this.model.mapping.validSource);
            this.data.targetDefinition = this.getDisplayDetails(this.model.mapping.validTarget);

            ConfigDelegate.readEntity("managed").then(function (managed) {
                _.each(managed.objects, function (managedObject) {
                    if (managedObject.name === "user") {
                        this.data.preferences = managedObject.schema.properties.preferences.properties;
                        this.model.allPreferences = _.keys(this.data.preferences);
                    }
                }, _this);

                if (!_.isUndefined(_this.model.mapping.validSource) && _this.model.mapping.validSource.file && _this.model.mapping.validSource.file === "ui/preferenceCheck.js") {

                    _this.model.currentPreferences = _this.model.mapping.validSource.globals.preferences;
                } else {
                    _this.model.currentPreferences = _.clone(_this.model.allPreferences);
                }

                _this.parentRender(function () {
                    _this.model.changesModule = ChangesPending.watchChanges({
                        element: _this.$el.find("#indvidualRecordMessages"),
                        undo: false,
                        watchedObj: _.clone(_this.model.mapping, true),
                        watchedProperties: ["validSource", "validTarget"]
                    });

                    if (_this.data.sourceDefinition.hasPreferences) {
                        _this.setPreferences(_this.model.currentPreferences, _this.$el.find(".preference-check"));
                    }
                });
            });
        },

        /**
         *
         * @param validationDetails - Current valid source or target object from mapping object
         * @returns {{hasScript: boolean, hasPreferences: boolean, script: null, value: string}} - Returns an object reflecting the current state of validSource
         *
         * Generates an object that will be used to correctly set the current html display for validSource or validTarget
         */
        getDisplayDetails: function getDisplayDetails(validationDetails) {
            var definition = {
                "hasScript": true,
                "hasPreferences": false,
                "script": null,
                "value": "allRecords"
            };

            if (_.isUndefined(validationDetails)) {
                definition.hasScript = false;
            } else if (!_.isUndefined(validationDetails.file) && validationDetails.file === "ui/preferenceCheck.js") {
                definition.hasPreferences = true;
                definition.script = validationDetails;
                definition.value = "userPreferences";
            } else {
                definition.script = validationDetails;
                definition.value = "customScript";
            }

            return definition;
        },

        /**
         *
         * @param event - Change event for the drop down selection for either validSource or validTarget
         *
         * This is the event fired when a drop down is changed this function handles the primary html updates while changeMappingEvent handles the config update
         */
        changeMappingEventDisplay: function changeMappingEventDisplay(event) {
            event.preventDefault();

            var select = event.target,
                type = $(select).attr("data-type"),
                value = $(select).val(),
                displayHolder,
                passedData = {};

            if (type === "source") {
                displayHolder = this.$el.find("#validSourceHolder .definition-holder");
            } else {
                displayHolder = this.$el.find("#validTargetHolder .definition-holder");
            }

            displayHolder.empty();

            if (value === "userPreferences") {
                passedData.preferences = this.data.preferences;

                displayHolder.html($(handlebars.compile("{{> mapping/association/_individualPreferences}}")(passedData)));

                this.setPreferences(this.model.allPreferences, this.$el.find(".preference-check"));
            } else if (value === "customScript") {
                passedData.script = {
                    "type": "text/javascript",
                    "globals": {},
                    "source": ""
                };

                passedData.type = type;

                displayHolder.html($(handlebars.compile("{{> mapping/association/_singleScript}}")(passedData)));
            }

            this.model.mapping = this.changeMappingEventConfig(this.model.mapping, value, type);
        },

        /**
         *
         * @param mapping - The current sync mapping object
         * @param value - The current value based on a drop down box select of types available for individual record validation
         * @param type - The current individual record validation type (source or target).
         * @returns {*} - Returns an updated mapping object with the type of individual record updated
         *
         * Updates the mapping object with the new config details based on the drop down selections made for individual record validation type
         */
        changeMappingEventConfig: function changeMappingEventConfig(mapping, value, type) {
            var scriptName,
                updatedMapping = _.clone(mapping);

            if (type === "source") {
                scriptName = "validSource";
            } else {
                scriptName = "validTarget";
            }

            if (value === "userPreferences") {
                updatedMapping[scriptName] = {
                    "type": "text/javascript",
                    "globals": {
                        "preferences": this.model.allPreferences
                    },
                    "file": "ui/preferenceCheck.js"
                };
            } else if (value === "customScript") {
                updatedMapping[scriptName] = {
                    "type": "text/javascript",
                    "globals": {},
                    "source": ""
                };
            } else {
                delete updatedMapping[scriptName];
            }

            if (this.model.changesModule) {
                this.model.changesModule.makeChanges(_.clone(updatedMapping));
            }

            return updatedMapping;
        },

        /**
         *
         * @param preferences - Takes in an array of strings for preferences
         * @param checkboxes - Takes in an array of html checkboxes for preferences
         */
        setPreferences: function setPreferences(preferences, checkboxes) {
            _.each(checkboxes, function (element) {
                _.find(preferences, function (value) {
                    if ($(element).val() === value) {
                        $(element).prop("checked", true);
                    }

                    return $(element).val() === value;
                }, this);
            }, this);
        },

        /**
         *
         * @param event - Change event that is used to find what checkbox for a preference was used
         *
         * Adds or removes the preference based on the checkbox value and checked state
         */
        changePreference: function changePreference(event) {
            event.preventDefault();

            var checkbox = $(event.target),
                checked = checkbox.is(":checked"),
                value = checkbox.val();

            if (checked) {
                this.model.currentPreferences.push(value);
            } else {
                this.model.currentPreferences = _.filter(this.model.currentPreferences, function (val) {
                    return val !== value;
                });
            }

            this.model.mapping.validSource.globals.preferences = this.model.currentPreferences;

            if (this.model.changesModule) {
                this.model.changesModule.makeChanges(_.clone(this.model.mapping));
            }
        },

        /**
         * @param event - The event returned from the save button
         *
         * Saves the current state of the record validation view
         */
        individualValidationSave: function individualValidationSave(event) {
            event.preventDefault();

            this.AbstractMappingSave(this.model.mapping, _.bind(function () {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "individualRecordValidationSaveSuccess");
                this.model.changesModule.saveChanges();
            }, this));
        },

        /**
         *
         * @param event - Click event used to find the current script being edited
         *
         * Opens the script dialog to allow a user to edit the current script for either validSource or validTarget
         */
        editScript: function editScript(event) {
            var _this2 = this;

            event.preventDefault();

            var type = $(event.target).parents(".event-hook-body").attr("data-type"),
                scriptName,
                args = {
                "saveCallback": function saveCallback(script) {
                    _this2.model.mapping = _this2.setScript(script, type, _this2.model.mapping);
                },
                "disableValidation": true
            };

            if (type === "source") {
                scriptName = "validSource";
                args.scriptDialogTitle = $.t("templates.mapping.recordValidation.editValidScript", { "script": "validSource" });
            } else {
                scriptName = "validTarget";
                args.scriptDialogTitle = $.t("templates.mapping.recordValidation.editValidScript", { "script": "validTarget" });
            }

            args.scriptData = this.model.mapping[scriptName];

            ScriptDialog.render(args);
        },

        /**
         *
         * @param script - The currently produced script object from the script editor
         * @param type - The type associated with the individual record validation event (source or target)
         * @param mapping - The current mapping object
         *
         * @returns {*} - Returns an updated mapping object with the newly set script object
         *
         * Sets the newly saved script to the correct validation event
         */
        setScript: function setScript(script, type, mapping) {
            if (type === "source") {
                mapping.validSource = script;
            } else {
                mapping.validTarget = script;
            }

            if (this.model.changesModule) {
                this.model.changesModule.makeChanges(_.clone(mapping));
            }

            return mapping;
        }
    });

    return new IndividualRecordValidationView();
});

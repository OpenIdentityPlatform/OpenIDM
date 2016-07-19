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

define([
    "jquery",
    "lodash",
    "jsonEditor",
    "form2js",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants"
], function($, _,
            JSONEditor,
            Form2js,
            AdminAbstractView,
            InlineScriptEditor,
            ConfigDelegate,
            EventManager,
            Constants) {

    var authenticationDataChanges = {},
        authenticationData = {},
        AuthenticationAbstractView = AdminAbstractView.extend({
            noBaseTemplate: true,
            element: "#AuthenticationModuleDialogContainer",
            model: {},
            knownProperties: [
                "enabled",
                "queryOnResource",
                "queryId",
                "defaultUserRoles",
                "propertyMapping",
                "augmentSecurityContext",
                "groupRoleMapping"
            ],
            partials: [
                "partials/form/_titaToggle.html",
                "partials/form/_basicSelect.html",
                "partials/form/_basicInput.html",
                "partials/form/_tagSelectize.html",
                "partials/settings/authentication/_customProperties.html",
                "partials/settings/authentication/_augmentSecurityContext.html",
                "partials/settings/authentication/_propertyMapping.html"
            ],
            events: {
                "click .advanced-toggle": "toggleAdvanced",
                "change #select-userOrGroup": "userOrGroupChange"
            },
            userOrGroupOptions: [
                {
                    "value": "userRoles",
                    "display": "templates.auth.userRoles"
                },
                {
                    "value": "groupMembership",
                    "display": "templates.auth.groupMembership"
                }
            ],
            JSONEditorDefaults: {
                disable_edit_json: true,
                disable_array_reorder: false,
                disable_collapse: true,
                disable_properties: true,
                show_errors: 'never',
                template: 'handlebars',
                iconlib: 'fontawesome4',
                theme: 'bootstrap3',
                no_additional_properties: true,
                additionalItems: false,
                required_by_default: true
            },

            getConfig: function() {
                var basic = Form2js("basicFields", ".", true),
                    advanced = Form2js("advancedForm", ".", true),
                    custom = this.getCustomProperties(this.customPropertiesEditor.getValue()),
                    augmentSecurityContext = this.getAugmentSecurityContext(),
                    userOrGroup = this.getUserOrGroupProperties();

                basic.name = this.data.config.name;

                if (_.has(advanced, "properties")) {
                    basic.properties = _.extend(basic.properties || {}, advanced.properties);
                }

                basic.properties = _.extend(basic.properties || {}, custom);
                basic.properties = _.extend(basic.properties || {}, augmentSecurityContext);
                basic.properties = _.extend(basic.properties || {}, userOrGroup);


                if (this.getCustomPropertyConfigs) {
                    basic.properties = _.extend(basic.properties || {}, this.getCustomPropertyConfigs());
                }

                return basic;
            },

            /**
             *
             * @param args
             * @param args.name {string}
             * @param args.customProperties {array}
             * @param args.augmentSecurityContext {object}
             */
            postRenderComponents: function(args) {
                this.$el.find(".array-selection").selectize({
                    delimiter: ",",
                    persist: false,
                    create: function (input) {
                        return {
                            value: input,
                            text: input
                        };
                    }
                });

                this.$el.find(".array-selection-no-custom").selectize({
                    delimiter: ",",
                    persist: false
                });

                if (!_.isUndefined(args.augmentSecurityContext)) {
                    this.model.scriptEditor = InlineScriptEditor.generateScriptEditor({
                        "element": this.$el.find("#augmentSecurityContext"),
                        "eventName": "module" + args.name + "ScriptEditor",
                        "scriptData": args.augmentSecurityContext
                    });
                }

                if (!_.isUndefined(args.customProperties)) {
                    this.initCustomPropertiesJSONEditor(args.customProperties);
                }

                if (!_.isUndefined(args.userOrGroup)) {
                    this.initGroupMembershipJSONEditor((args.userOrGroup));
                }
            },

            getAugmentSecurityContext: function() {
                let returnVal = {};
                if (this.model.scriptEditor.generateScript() !== null) {
                    returnVal.augmentSecurityContext = this.model.scriptEditor.generateScript();
                }
                return returnVal;
            },

            /**
             * Takes a list of known properties and the defined properties then creates a list of the
             * unknown properties formatted in such a way that it can be consumed by JSONEditor.
             *
             * @param knownProperties {array<string>} A list of known properties
             * @param configProperties {object} The properties object of an authentication module
             * @returns {Array<object>} A list of JSONEditor formatted object to render custom properties
             */
            getCustomPropertiesList: function(knownProperties, configProperties) {
                let customProperties = [];

                // Create Custom Properties object from properties that are not known
                let customPropertyKeys = (_.filter(_.keys(configProperties), (key) => {
                    if (knownProperties.indexOf(key) === -1) {
                        return true;
                    }
                }));

                _.each(customPropertyKeys, (property) => {
                    customProperties.push({
                        "propertyName": property,
                        "propertyValue": configProperties[property]
                    });
                });

                return customProperties;
            },

            /**
             * Get the JSONEditor value for the custom properties and formats them back into a
             * standard key:value pair object
             *
             * @returns {object}
             */
            getCustomProperties: function(config) {
                var formattedProperties = {};

                _.each(config, (property) => {
                    if (property.propertyName.length > 0) {
                        formattedProperties[property.propertyName] = property.propertyValue;
                    }
                });

                return formattedProperties;
            },


            /**
             * Given a default value this will initialize the custom properties JSONEditor with that value.
             * @param defaultValue
             */
            initCustomPropertiesJSONEditor: function(defaultValue) {
                const schema = {
                    "type": "array",
                    "title": " ",
                    "items": {
                        "type": "object",
                        "title": $.t("templates.auth.property"),
                        "headerTemplate": "{{self.propertyName}}",
                        "properties": {
                            "propertyName": {
                                "title": $.t("templates.auth.propertyName"),
                                "type": "string"
                            },
                            "propertyValue": {
                                "title": $.t("templates.auth.propertyType"),
                                "oneOf": [
                                    {
                                        "type": "string",
                                        "title": $.t("templates.auth.string")
                                    }, {
                                        "title": $.t("templates.auth.simpleArray"),
                                        "type": "array",
                                        "format": "table",
                                        "items": {
                                            "type": "string",
                                            "title": $.t("templates.auth.value")
                                        }
                                    }
                                ]
                            }
                        }
                    }
                };

                this.customPropertiesEditor = new JSONEditor(this.$el.find("#customProperties")[0], _.extend({
                    schema: schema,
                    startval: defaultValue
                }, this.JSONEditorDefaults));
            },

            /**
             * Given a authentication module configuration this will determine if the UI should us
             * the user roles or group membership
             *
             * @param config {object}
             * @returns {{type: string, formattedGroupRoleMapping: Array}}
             */
            getUserOrGroupDefault: function(config) {
                let userOrGroup = {
                    type: "userRoles",
                    formattedGroupRoleMapping: []
                };

                if (_.has(config, "properties") && _.has(config.properties, "propertyMapping") && _.has(config.properties.propertyMapping, "groupMembership")) {
                    userOrGroup.type = "groupMembership";

                    if (_.has(config.properties, "groupRoleMapping")) {
                        _.each(config.properties.groupRoleMapping, (mappings, roleName) => {
                            userOrGroup.formattedGroupRoleMapping.push({
                                "groupMapping": mappings,
                                "roleName": roleName
                            });
                        });
                    }
                }

                return userOrGroup;
            },

            getUserOrGroupProperties: function() {
                if (this.$el.find("#basicPropertyMappingFields")[0] && this.$el.find("#select-userOrGroup")[0]) {
                    var userOrGroupProperties = Form2js("basicPropertyMappingFields", ".", true),
                        userOrGroup = this.$el.find("#select-userOrGroup").val();

                    if (!_.has(userOrGroupProperties, "properties")) {
                        userOrGroupProperties.properties = {"propertyMapping": {}};
                    }

                    userOrGroupProperties.properties.propertyMapping[userOrGroup] = this.$el.find("#input-properties\\.propertyMapping\\." + userOrGroup).val();

                    if (userOrGroup === "groupMembership") {
                        userOrGroupProperties.properties.groupRoleMapping = this.formatGroupMembershipProperties(this.groupMembershipEditor.getValue());
                    }

                    return userOrGroupProperties.properties;
                }
            },

            /**
             * Takes a JSONEditor formatted value of group membership properties and converts them to an IDM format
             * @param editorValue
             * @returns {{}}
             */
            formatGroupMembershipProperties: function(editorValue) {
                var mappings = {};
                _.each(editorValue, (group) => {
                    mappings[group.roleName] = group.groupMapping;
                });
                return mappings;
            },

            userOrGroupChange: function(e) {
                if ($(e.currentTarget).val() === "userRoles") {
                    this.$el.find("#groupMembershipOptions").hide();
                    this.$el.find("#userRolesOptions").show();

                    this.$el.find("#input-properties.propertyMapping.userRoles").val("");

                } else {
                    this.$el.find("#groupMembershipOptions").show();
                    this.$el.find("#userRolesOptions").hide();

                    this.customPropertiesEditor.setValue({});
                }
            },

            /**
             * Given a default value this will initialize the groupMembership JSONEditor with that value.
             * @param defaultValue
             */
            initGroupMembershipJSONEditor: function(config) {
                if (config.type === "groupMembership") {
                    this.$el.find("#select-userOrGroup").val("groupMembership").change();
                }

                const schema = {
                    "title": $.t("templates.auth.groupRoleMapping"),
                    "type": "array",
                    "default": [
                        {"roleName":"openidm-admin", "groupMapping": []}
                    ],
                    "items": {
                        "type": "object",
                        "title": $.t("templates.auth.role"),
                        "headerTemplate": "{{self.roleName}}",
                        "properties": {
                            "roleName": {
                                "type": "string",
                                "title": $.t("templates.auth.roleName")
                            },
                            "groupMapping": {
                                "title": $.t("templates.auth.groupMappings"),
                                "type": "array",
                                "format": "table",
                                "items": {
                                    "type": "string",
                                    "title": $.t("templates.auth.group")
                                }
                            }
                        }
                    }
                };

                this.groupMembershipEditor = new JSONEditor(this.$el.find("#groupMembershipOptionsEditor")[0], _.extend({
                    schema: schema,
                    startval: config.formattedGroupRoleMapping
                }, this.JSONEditorDefaults));
            },

            retrieveAuthenticationData: function (callback) {
                ConfigDelegate.readEntity("authentication").then(_.bind(function (data) {
                    authenticationDataChanges = _.clone(data, true);
                    authenticationData = _.clone(data, true);

                    if (callback) {
                        callback();
                    }
                }, this));
            },

            toggleAdvanced: function() {
                this.$el.find(".advancedShowHide").toggleClass("fa fa-caret-down");
                this.$el.find(".advancedShowHide").toggleClass("fa fa-caret-right");
                this.$el.find("#advancedForm").toggle();

                if (this.$el.find("#advancedForm").is(":visible")) {
                    this.model.scriptEditor.refresh();
                }
            },

            getAuthenticationData: function () {
                return _.clone(authenticationData.serverAuthContext, true);
            },

            /**
             * Keeps a clean, ready to save copy of the authentication changes.
             * This should be called by implementing views right before calling checkChanges to ensure data is always uptodate.
             *
             * @param properties {array} - an array of strings representing the properties in the passed in object.
             * @param object {object} - the object containing changes
             */
            setProperties: function(properties, object) {
                _.each(properties, function(prop) {
                    if (_.isEmpty(object[prop]) &&
                        !_.isNumber(object[prop]) &&
                        !_.isBoolean(object[prop])) {
                        delete authenticationDataChanges[prop];
                    } else {
                        authenticationDataChanges.serverAuthContext[prop] = object[prop];
                    }
                }, this);
            },

            saveAuthentication: function() {
                return ConfigDelegate.updateEntity("authentication", authenticationDataChanges).then(function() {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "authSaveSuccess");
                    authenticationData = _.clone(authenticationDataChanges, true);
                });
            }

        });

    return AuthenticationAbstractView;
});

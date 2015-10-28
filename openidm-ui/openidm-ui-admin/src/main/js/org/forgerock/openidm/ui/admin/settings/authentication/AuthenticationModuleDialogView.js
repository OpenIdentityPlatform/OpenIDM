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

/*global define */

define("org/forgerock/openidm/ui/admin/settings/authentication/AuthenticationModuleDialogView", [
    "jquery",
    "underscore",
    "jsonEditor",
    "handlebars",
    "bootstrap-dialog",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/admin/settings/authentication/AuthenticationAbstractView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/openidm/ui/common/delegates/SiteConfigurationDelegate",
    "org/forgerock/openidm/ui/common/delegates/OpenAMProxyDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "selectize"

], function($, _,
            JSONEditor,
            Handlebars,
            BootstrapDialog,
            Conf,
            AuthenticationAbstractView,
            ConfigDelegate,
            InlineScriptEditor,
            ConnectorDelegate,
            SiteConfigurationDelegate,
            OpenamProxyDelegate,
            UIUtils) {

    var AuthenticationModuleDialogView = AuthenticationAbstractView.extend({
        template: "templates/admin/settings/authentication/AuthenticationModuleDialogTemplate.html",
        element: "#dialogs",
        noBaseTemplate: true,
        events: {
            "click .advancedForm > div > h3": "toggleAdvanced",
            "change .changes-watched": "changed"
        },
        model: {},
        data: {},

        /**
         * @param configs {object}
         * @param configs.addedOpenAM {function}
         * @param configs.config {object} - the existing config for the module
         * @param callback
         */
        render: function (configs, callback) {
            this.model = _.extend(
                {
                    defaultUserRoles: [
                        {"name": "openidm-admin"},
                        {"name": "openidm-authorized"},
                        {"name": "openidm-cert"},
                        {"name": "openidm-reg"},
                        {"name": "openidm-task-manager"}
                    ],
                    amUIProperties: [
                        "openamLoginUrl",
                        "openamLoginLinkText",
                        "openamUseExclusively"
                    ],
                    amTruststoreType : "&{openidm.truststore.type}",
                    amTruststoreFile : "&{openidm.truststore.location}",
                    amTruststorePassword : "&{openidm.truststore.password}"
                },
                configs
            );

            this.model.readableName = $.t("templates.auth.modules." + this.model.config.name + ".name");

            // Get resources and get the JSON schema
            $.when(this.getResources(), this.getJSONSchema(this.model.config.name)).then(_.bind(function(resources, jsonTemplate) {
                this.model.moduleTemplate = Handlebars.compile(jsonTemplate[0])();
                this.model.moduleTemplate = $.parseJSON(this.model.moduleTemplate);

                // The following code updates the enum values for the queryOnResource property
                // Internal/Anonymous User Modules will only need access to the internal repo and can be excluded as that value is set in the template.
                if (this.model.moduleTemplate.templateName !== "INTERNAL_USER" &&
                    this.model.moduleTemplate.templateName !== "STATIC_USER" &&
                    _.has(this.model.moduleTemplate.mainSchema, "properties") &&
                    _.has(this.model.moduleTemplate.mainSchema.properties, "queryOnResource") &&
                    _.has(this.model.moduleTemplate.mainSchema.properties.queryOnResource, "enum")) {

                    this.model.moduleTemplate.mainSchema.properties.queryOnResource["enum"] = resources;

                    // Client Cert Modules have access to an additional resource: "security/truststore"
                    if (this.model.moduleTemplate.templateName === "CLIENT_CERT") {
                        this.model.moduleTemplate.mainSchema.properties.queryOnResource["enum"].push("security/truststore");
                    }
                }

                this.parentRender(_.bind(function() {
                    var self = this,
                        prefix =  $.t("templates.auth.edit");

                    if (this.model.newModule) {
                        prefix =  $.t("templates.auth.new");
                    }

                    this.model.currentDialog = $('<div id="AuthenticationModuleDialog"></div>');
                    this.setElement(this.model.currentDialog);
                    $('#dialogs').append(this.model.currentDialog);

                    BootstrapDialog.show({
                        title: prefix + " " + this.model.readableName + " " + $.t("templates.auth.authFieldsetName"),
                        size: BootstrapDialog.SIZE_WIDE,
                        type: BootstrapDialog.TYPE_DEFAULT,
                        message: this.model.currentDialog,
                        onshown: function() {
                            UIUtils.renderTemplate(
                                self.template,
                                self.$el,
                                _.extend({}, Conf.globalData, self.data),
                                function() {
                                    self.loadDefaults();
                                    self.makeCustomEditorChanges();
                                    self.toggleModule();

                                    if (callback) {
                                        callback();
                                    }
                                },
                                "replace"
                            );
                        },
                        buttons: [
                            {
                                label: $.t("common.form.cancel"),
                                action: function(dialogRef) {
                                    dialogRef.close();
                                }
                            }, {
                                label: $.t("common.form.submit"),
                                id: "submitAuditEventHandlers",
                                cssClass: "btn-primary",
                                action: function(dialogRef) {
                                    dialogRef.close();
                                    self.getConfig();
                                }
                            }
                        ]
                    });
                }, this));
            }, this));
        },

        /**
         * Gets the JSON Schema corresponding to the moduleName provided
         *
         * @param moduleName
         * @returns {promise}
         */
        getJSONSchema: function(moduleName) {
            return $.ajax({
                url: "templates/admin/settings/authentication/" + moduleName + ".hbs",
                type: "GET"
            });
        },

        /**
         * Retrieves a list of connectors and managed objects and creates an array of resources
         *
         * @returns {promise} promise - resolves with an array of strings
         */
        getResources: function() {
            var connectorPromise = ConnectorDelegate.currentConnectors(),
                managedPromise = ConfigDelegate.readEntity("managed"),
                resources = [];

            return $.when(connectorPromise, managedPromise).then(_.bind(function(connectors, managedObjects) {
                _.each(managedObjects.objects, _.bind(function(managed){
                    resources.push("managed/" + managed.name);
                }, this));

                _.each(connectors, _.bind(function(connector) {
                    _.each(connector.objectTypes, _.bind(function(ot) {
                        resources.push("system/" + connector.name + "/" + ot);
                    }, this));
                }, this));

                return resources;
            }, this));
        },

        /**
         * Toggles the advanced section
         */
        toggleAdvanced: function() {
            this.$el.find(".advancedShowHide").toggleClass("fa fa-caret-down");
            this.$el.find(".advancedShowHide").toggleClass("fa fa-caret-right");
            this.$el.find(".advancedForm").find(".well").first().toggle();
        },

        loadDefaults: function() {
            var jsonEditorBasicFormat,
                jsonEditorAdvancedFormat,
                tempVar,
                JSONEditorDefaults = {
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
                };

            function addProperty(jsonEditor, value, property) {
                if (property === "propertyMapping") {
                    _.each(value, function(propertyMapValue, propertyMapKey) {

                        // Generic propertyMapping property: is truthy only if the property exists on the main object
                        if (_.has(jsonEditor.propertyMapping, propertyMapKey)) {
                            jsonEditor.propertyMapping[propertyMapKey] = propertyMapValue;

                        // The following two cases would normally be found on the main object,
                        // but because of formatting from json editor they must be called out directly.
                        } else if (propertyMapKey === "userRoles") {
                            jsonEditor.propertyMapping.userorgroup = propertyMapValue;

                        } else if (propertyMapKey ==="groupMembership") {
                            tempVar = [];
                            _.map(this.model.config.properties.groupRoleMapping, function(map, key){
                                tempVar.push({
                                    roleName: key,
                                    groupMapping: map
                                });
                            });

                            jsonEditor.propertyMapping.userorgroup = {
                                grpMembership: propertyMapValue,
                                groupRoleMapping: tempVar
                            };
                        }
                    }, this);

                } else if (property === "defaultUserRoles") {
                    this.$el.find(".container-defaultUserRoles input")[0].selectize.setValue(value);

                } else {
                    jsonEditor[property] = value;
                }
            }

            this.model.basicEditor = new JSONEditor(this.$el.find(".basicForm")[0], _.extend({
                schema: this.model.moduleTemplate.mainSchema
            }, JSONEditorDefaults));

            this.model.advancedEditor = new JSONEditor(this.$el.find(".advancedForm")[0], _.extend({
                schema: this.model.moduleTemplate.advancedSchema
            }, JSONEditorDefaults));

            this.model.scriptEditor = InlineScriptEditor.generateScriptEditor({
                "element": this.$el.find(".container-augmentSecurityContext .well"),
                "eventName": "module" + this.model.config.name + "ScriptEditor",
                "scriptData": this.model.config.properties.augmentSecurityContext
            });

            this.$el.find(".container-defaultUserRoles input").selectize({
                options: this.model.defaultUserRoles,
                valueField: 'name',
                labelField: 'name',
                searchField: ['name'],
                create: true
            });

            this.model.basicEditor.on("change", _.bind(function () {
                this.makeCustomEditorChanges();
            }, this));

            this.model.advancedEditor.on("change", _.bind(function () {
                this.makeCustomEditorChanges();
            }, this));

            this.$el.find(".advancedForm > div > .ui-widget-content").hide();
            this.$el.find(".advancedForm>div>h3").prepend("<i class='advancedShowHide fa fa-caret-right'></i>");

            jsonEditorBasicFormat = this.model.basicEditor.getValue();
            jsonEditorAdvancedFormat = this.model.advancedEditor.getValue();
            jsonEditorBasicFormat.enabled = this.model.config.enabled;
            jsonEditorAdvancedFormat.customProperties = [];

            if (this.model.config.name === "OPENAM_SESSION"){
                //add amUIProperties
                this.model.config.properties = _.extend(
                    this.model.config.properties,
                    _.pick(Conf.globalData, this.model.amUIProperties)
                );
            }

            _.each(this.model.config.properties, function(value, key) {
                if (_.has(jsonEditorBasicFormat, key)) {
                    _.bind(addProperty, this)(jsonEditorBasicFormat, value, key);

                } else if (_.has(jsonEditorAdvancedFormat, key)) {
                    _.bind(addProperty, this)(jsonEditorAdvancedFormat, value, key);

                } else if (key !== "groupRoleMapping") {
                    jsonEditorAdvancedFormat.customProperties.push({
                        propertyName: key,
                        propertyType: value
                    });
                }
            }, this);

            this.model.basicEditor.setValue(jsonEditorBasicFormat);
            this.model.advancedEditor.setValue(jsonEditorAdvancedFormat);
        },

        makeCustomEditorChanges: function() {
            var openamDeploymentUrl,
                openamLoginUrl,
                basicEditor = this.model.basicEditor.getValue(),
                advancedEditor = this.model.advancedEditor.getValue();

            if (this.model.config.name === "STATIC_USER" && _.has(basicEditor, "username")) {
                this.$el.find(".authModuleResource").html(basicEditor.username);

            } else if(_.has(basicEditor, "queryOnResource")) {
                this.$el.find(".authModuleResource").html(basicEditor.queryOnResource);

            } else if(_.has(advancedEditor, "queryOnResource")) {
                this.$el.find(".authModuleResource").html(advancedEditor.queryOnResource);
            }

            this.$el.find("input[type='hidden']").parent().hide();

            if (this.model.config.name === "OPENAM_SESSION"){
                openamDeploymentUrl = this.$el.find("input[name*='openamDeploymentUrl']");
                openamLoginUrl = this.$el.find("input[name*='openamLoginUrl']");
                openamDeploymentUrl.focus(function() {
                    openamDeploymentUrl.attr("beforeValue", openamDeploymentUrl.val());
                });

                // Unbinding here so that this function does not get bound multiple times
                openamDeploymentUrl.unbind("blur").blur(_.bind(function() {
                    advancedEditor.getEditor('root.openamLoginUrl').setValue(
                        openamLoginUrl.val().replace(openamDeploymentUrl.attr("beforeValue"), openamDeploymentUrl.val())
                    );
                },this));

            }
        },

        // When the module is opened or closed toggle the ui appropriately and update the display setting of the userorgroup fields
        toggleModule: function() {
            var userorgroup = false,
                userGroupEditorNode;

            // The conditional display of roles or groupmembership doesn't load as expected from JsonEditor, custom code to show and hide is necessary
            userGroupEditorNode = this.model.basicEditor.getEditor("root.propertyMapping.userorgroup") || this.model.advancedEditor.getEditor("root.propertyMapping.userorgroup");
            if (!_.isUndefined(userGroupEditorNode) && !_.isNull(userGroupEditorNode)) {
                userorgroup = userGroupEditorNode.value;

                if (_.isString(userorgroup) && userorgroup.length > 0) {
                    userGroupEditorNode.switchEditor(1);
                    this.$el.find(".container-userorgroup select").val($.t("templates.auth.userRoles"));

                } else if (_.isObject(userorgroup)) {
                    userGroupEditorNode.switchEditor(2);
                    this.$el.find(".container-userorgroup select").val($.t("templates.auth.groupMembership"));

                } else {
                    userGroupEditorNode.switchEditor(0);
                    this.$el.find(".container-userorgroup select").val($.t("templates.auth.selectOption"));
                }

            }
        },

        // Creates an Authentication JSON based off of the object the page was loaded with and any new or changed values
        getConfig: function(e) {
            var editorValues = _.extend(this.model.basicEditor.getValue(), this.model.advancedEditor.getValue()),
                newConfig = {},
                amUISettingsProm;

            newConfig.name = this.model.config.name;
            newConfig.enabled = editorValues.enabled;
            newConfig.properties = {};
            newConfig.properties.propertyMapping = {};

            if (newConfig.name === "OPENAM_SESSION") {
                amUISettingsProm = this.handleOpenAMUISettings(editorValues);

                //remove amUIProperties
                editorValues = _.omit(editorValues, this.model.amUIProperties);
                editorValues.truststoreType = this.model.amTruststoreType;
                editorValues.truststoreFile = this.model.amTruststoreFile;
                editorValues.truststorePassword = this.model.amTruststorePassword;
            } else {
                amUISettingsProm = $.Deferred().resolve();
            }

            // Set root level properties that are not objects
            _.each(_.omit(editorValues, ["authModule", "customProperties", "propertyMapping", "enabled", "augmentSecurityContext", "defaultUserRoles"]), function (property, key) {
                if ((_.isString(property) && property.length > 0) || (!_.isString(property))) {
                    newConfig.properties[key] = property;
                }
            });

            _.each(editorValues.customProperties, function(customProperty) {
                newConfig.properties[customProperty.propertyName] = customProperty.propertyType;
            });


            _.each(_.omit(editorValues.propertyMapping, ["userorgroup"]), function (property, key) {
                newConfig.properties.propertyMapping[key] = property;
            });

            newConfig.properties.defaultUserRoles = $(".container-defaultUserRoles input").val().split(",");

            if (this.model.scriptEditor.generateScript() !== null) {
                newConfig.properties.augmentSecurityContext = this.model.scriptEditor.generateScript();
            }

            // these are undefined/null for the STATIC_USER module which has no propertyMapping
            if (!_.isUndefined(editorValues.propertyMapping) &&
                !_.isNull(editorValues.propertyMapping) &&
                !_.isUndefined(editorValues.propertyMapping.userorgroup) &&
                !_.isNull(editorValues.propertyMapping.userorgroup)) {

                if (editorValues.propertyMapping.userorgroup.length > 0) {
                    newConfig.properties.propertyMapping.userRoles = editorValues.propertyMapping.userorgroup;

                } else if (!_.isUndefined(editorValues.propertyMapping.userorgroup.grpMembership)) {
                    newConfig.properties.propertyMapping.groupMembership = editorValues.propertyMapping.userorgroup.grpMembership;
                    newConfig.properties.groupRoleMapping = {};

                    _.each(editorValues.propertyMapping.userorgroup.groupRoleMapping, function (mapping) {
                        newConfig.properties.groupRoleMapping[mapping.roleName] = mapping.groupMapping;
                    });
                }
            }

            if (_.isEmpty(newConfig.properties.propertyMapping)) {
                delete newConfig.properties.propertyMapping;
            }

            amUISettingsProm.then(_.bind(function() {
                this.model.saveCallback(newConfig);
            }, this));
        },

        handleOpenAMUISettings: function(editorValues){
            var prom = $.Deferred(),
                amSettings = _.pick(editorValues, this.model.amUIProperties, "enabled"),
                confirmed = function(){
                    SiteConfigurationDelegate.getConfiguration().then(function(uiConfig){
                        ConfigDelegate.updateEntity("ui/configuration", { configuration: _.extend(uiConfig, amSettings) }).then(function() {
                            prom.resolve();
                        });
                    });
                };

            amSettings.openamAuthEnabled = amSettings.enabled;
            delete amSettings.enabled;

            if (amSettings.openamAuthEnabled) {
                // Validate openamDeploymentUrl
                OpenamProxyDelegate.serverinfo(editorValues.openamDeploymentUrl).then(_.bind(function(info){
                        if (info.cookieName) {
                            // Set openamSSOTokenCookieName for this module
                            editorValues.openamSSOTokenCookieName = info.cookieName;
                            confirmed();
                        } else {
                            UIUtils.jqConfirm($.t("templates.auth.openamDeploymentUrlConfirmation"), confirmed);
                        }
                    },this),
                    _.bind(function(){
                        UIUtils.jqConfirm($.t("templates.auth.openamDeploymentUrlConfirmation"), confirmed);
                    },this));
            } else {
                confirmed();
            }

            return prom;
        }
    });

    Handlebars.registerHelper("jsonEditor", function (jsonString) {
        return "{{" + jsonString + "}}";
    });

    return new AuthenticationModuleDialogView();
});

/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

define("org/forgerock/openidm/ui/admin/settings/AuthenticationView", [
    "jquery",
    "underscore",
    "form2js",
    "jsonEditor",
    "handlebars",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/openidm/ui/common/delegates/SiteConfigurationDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/delegates/OpenAMProxyDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "jqueryui"
], function($, _, form2js, JSONEditor, Handlebars, AdminAbstractView, eventManager, constants, router, ConfigDelegate, InlineScriptEditor, ConnectorDelegate, SiteConfigurationDelegate, conf, openamProxyDelegate, UIUtils) {

    var AuthenticationView = AdminAbstractView.extend({
        template: "templates/admin/settings/AuthenticationTemplate.html",
        element: "#authenticationContainer",
        noBaseTemplate: true,
        events: {
            "click .edit": "toggleModule",
            "click .trash": "trashModule",
            "click .down": "downModule",
            "click .up": "upModule",
            "click #addNew": "addNewModule",
            "click #submitAuth": "submitAuthModules",
            "click .advancedForm > div > h3": "toggleAdvanced",
            "change #moduleType": "newModuleTypeSet"
        },

        model: {
            moduleIndex: 0,
            modules: {},
            defaultAuth: {},
            module_types: {
                "STATIC_USER" : null,
                "CLIENT_CERT": null,
                "IWA": null,
                "MANAGED_USER": null,
                "OPENAM_SESSION": null,
                "INTERNAL_USER": null,
                "OPENID_CONNECT": null,
                "PASSTHROUGH": null
            },
            defaultUserRoles: [
                "openidm-admin",
                "openidm-authorized",
                "openidm-cert",
                "openidm-reg",
                "openidm-task-manager"
            ],
            resources: [],
            amUIProperties: [
                "openamLoginUrl",
                "openamLoginLinkText",
                "openamUseExclusively"
            ],
            amTokenTime: "5",
            defaultTokenTime: "7200",
            defaultTokenIdleTime: "1800",
            amTruststoreType : "&{openidm.truststore.type}",
            amTruststoreFile : "&{openidm.truststore.location}",
            amTruststorePassword : "&{openidm.truststore.password}"
        },

        render: function (args, callback) {
            // Set JSONEditor defaults
            _(JSONEditor.defaults.options).extend({
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
            });

            this.data.dochelpurl = constants.DOC_URL;

            var connectorPromise = ConnectorDelegate.currentConnectors(),
                managedPromise = ConfigDelegate.readEntity("managed");

            $.when(connectorPromise, managedPromise).then(_.bind(function(connectors, managedObjects) {
                this.model.resources = [];

                _.each(managedObjects.objects, _.bind(function(managed){
                    this.model.resources.push("managed/" + managed.name);
                }, this));

                _.each(connectors, _.bind(function(connector) {
                    _.each(connector.objectTypes, _.bind(function(ot) {
                        this.model.resources.push("system/" + connector.name + "/" + ot);
                    }, this));
                }, this));

                this.parentRender(_.bind(function() {
                    // Gets the JSON schema for the JSONEditor, the JSON is treated as a handlebars template for string translation
                    _.chain(this.model.module_types)
                        .keys()
                        .sortBy(function(key) {return key;})
                        .each(function(moduleName) {
                            this.$el.find("#group-copy .moduleType").append("<option value='" + moduleName + "'>" + moduleName +  "</option>");

                            $.ajax({
                                url: "templates/admin/settings/authentication/" + moduleName + ".json",
                                type: "GET",
                                success: _.bind(function(jsonTemplate) {
                                    jsonTemplate = Handlebars.compile(jsonTemplate)();
                                    jsonTemplate = $.parseJSON(jsonTemplate);

                                    // The following code updates the enum values for the queryOnResource property
                                    // Internal/Anonymous User Modules will only need access to the internal repo and can be excluded as that value is set in the template.
                                    if (jsonTemplate.templateName !== "INTERNAL_USER" && jsonTemplate.templateName !== "STATIC_USER" &&
                                        _.has(jsonTemplate.mainSchema, "properties") &&
                                        _.has(jsonTemplate.mainSchema.properties, "queryOnResource") &&
                                        _.has(jsonTemplate.mainSchema.properties.queryOnResource, "enum")) {

                                        jsonTemplate.mainSchema.properties.queryOnResource["enum"] = _.clone(this.model.resources);

                                        // Client Cert Modules have access to an additional resource: "security/truststore"
                                        if (jsonTemplate.templateName === "CLIENT_CERT") {
                                            jsonTemplate.mainSchema.properties.queryOnResource["enum"].push("security/truststore");
                                        }
                                    }

                                    this.model.module_types[moduleName] = jsonTemplate;
                                }, this)
                            });
                        }, this);


                    this.$el.find(".authenticationModules .group-body")
                        .accordion({
                            header: "> div > .list-header",
                            collapsible: true,
                            icons: false,
                            event: false,
                            active: false
                        })
                        .sortable({
                            axis: "y",
                            handle: ".list-header",
                            stop: function(event, ui) {
                                // IE doesn't register the blur when sorting
                                // so trigger focusout handlers to remove .ui-state-focus
                                ui.item.children(".list-header").triggerHandler("focusout");

                                // Refresh accordion to handle new order
                                $(this).accordion("refresh");
                            }
                        });
                    this.loadDefaults();
                }, this));
            }, this));
        },

        toggleAdvanced: function(e) {
            this.$el.find(e.target).parentsUntil(".form").find(".advancedShowHide").toggleClass("fa fa-caret-down");
            this.$el.find(e.target).parentsUntil(".form").find(".advancedShowHide").toggleClass("fa fa-caret-right");
            this.$el.find(e.target).parentsUntil(".advancedForm").find(".well").first().toggle();
        },

        loadJWTTokenTimeSettings: function() {
            var props = this.model.defaultAuth.serverAuthContext.sessionModule.properties,
                convertToSeconds = function(mins) {
                    return parseInt(mins, 10) * 60;
                };

            if (props.maxTokenLifeMinutes) {
                props.maxTokenLifeSeconds = props.maxTokenLifeSeconds || convertToSeconds(props.maxTokenLifeMinutes);
            }

            if (props.tokenIdleTimeMinutes) {
                props.tokenIdleTimeSeconds = props.tokenIdleTimeSeconds || convertToSeconds(props.tokenIdleTimeMinutes);
            }

            this.$el.find("#maxTokenLifeSeconds").val(props.maxTokenLifeSeconds);
            this.$el.find("#tokenIdleTimeSeconds").val(props.tokenIdleTimeSeconds);
        },

        //  Gets the current authentication JSON file and creates UI modules for each one
        loadDefaults: function() {
            ConfigDelegate.readEntity("authentication").then(_.bind(function (auth) {
                this.model.defaultAuth = auth;
                this.loadJWTTokenTimeSettings();
                this.$el.find("#sessionOnly").val(auth.serverAuthContext.sessionModule.properties.sessionOnly.toString());

                _(auth.serverAuthContext.authModules).each(function (module) {
                    var jsonEditorBasicFormat,
                        jsonEditorAdvancedFormat,
                        tempVar,
                        tempKey = this.addNewModule(false);

                    this.newModuleTypeSet(null, tempKey, module);
                    this.model.modules[tempKey].module.find(".moduleType").prop('disabled', true);
                    this.toggleModule(this.model.modules[tempKey].module);

                    jsonEditorBasicFormat = this.model.modules[tempKey].basicEditor.getValue();
                    jsonEditorAdvancedFormat = this.model.modules[tempKey].advancedEditor.getValue();

                    jsonEditorBasicFormat.enabled = module.enabled;
                    jsonEditorAdvancedFormat.customProperties = [];

                    function addProperty(jsonEditor, value, property) {
                        if (property === "propertyMapping") {
                            _(value).each(function(propertyMapValue, propertyMapKey) {
                                // Generic propertyMapping property: is truthy only if the property exists on the main object
                                if (_(jsonEditor.propertyMapping).has(propertyMapKey)) {
                                    jsonEditor.propertyMapping[propertyMapKey] = propertyMapValue;

                                    // The following two cases would normally be found on the main object,
                                    // but because of formatting from json editor they must be called out directly.
                                } else if (propertyMapKey === "userRoles") {
                                    jsonEditor.propertyMapping.userorgroup = propertyMapValue;

                                } else if (propertyMapKey ==="groupMembership") {
                                    tempVar = [];
                                    _(module.properties.groupRoleMapping).map(function(map, key){
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
                            $(module.defaultUserRoles).val(value.join(", "));
                        } else {
                            jsonEditor[property] = value;
                        }
                    }

                    if(module.name === "OPENAM_SESSION"){
                        //add amUIProperties
                        module.properties = _.extend(module.properties,_.pick(conf.globalData,this.model.amUIProperties));
                    }


                    _(module.properties).each(function(value, key) {
                        if (_(jsonEditorBasicFormat).has(key)) {
                            addProperty(jsonEditorBasicFormat, value, key);

                        } else if (_(jsonEditorAdvancedFormat).has(key)) {
                            addProperty(jsonEditorAdvancedFormat, value, key);

                        } else if (key !== "groupRoleMapping") {
                            jsonEditorAdvancedFormat.customProperties.push({
                                propertyName: key,
                                propertyType: value
                            });
                        }
                    }, this);

                    this.model.modules[tempKey].basicEditor.setValue(jsonEditorBasicFormat);
                    this.model.modules[tempKey].advancedEditor.setValue(jsonEditorAdvancedFormat);
                    this.makeCustomEditorChanges(tempKey);
                }, this);
            }, this));
        },

        newModuleTypeSet: function(e, id, module) {
            function split(val) {
                return val.split(/,\s*/);
            }
            function extractLast(term) {
                return split(term).pop();
            }
            var moduleId, moduleType, scriptData;

            if (e !== null) {
                moduleId = $(e.target).parents(".group").attr("id");
                moduleType = this.model.modules[moduleId].module.find(".moduleType").val();
            } else {
                moduleId = id;
                moduleType = module.name;
            }

            if (module) {
                scriptData = module.properties.augmentSecurityContext;
            }

            this.model.modules[moduleId].name = "";

            if (this.model.modules[moduleId].basicEditor !== null) {
                this.model.modules[moduleId].basicEditor.destroy();
                this.model.modules[moduleId].basicEditor = null;
            }

            if (this.model.modules[moduleId].advancedEditor !== null) {
                this.model.modules[moduleId].advancedEditor.destroy();
                this.model.modules[moduleId].advancedEditor = null;
            }

            if (moduleType !== "choose") {
                this.model.modules[moduleId].name = moduleType;
                this.model.modules[moduleId].basicEditor = new JSONEditor(this.model.modules[moduleId].module.find(".basicForm")[0], {
                    schema: this.model.module_types[moduleType].mainSchema,
                    uuid: moduleId
                });

                this.model.modules[moduleId].advancedEditor = new JSONEditor(this.model.modules[moduleId].module.find(".advancedForm")[0], {
                    schema: this.model.module_types[moduleType].advancedSchema,
                    uuid: moduleId
                });

                this.model.modules[moduleId].scriptEditor = InlineScriptEditor.generateScriptEditor({
                    "element": this.$el.find("#"+moduleId).find(".container-augmentSecurityContext .well"),
                    "eventName": "module" + moduleId + "ScriptEditor",
                    "scriptData": scriptData
                });

                this.model.modules[moduleId].defaultUserRoles = this.$el.find("#"+moduleId).find(".container-defaultUserRoles input")
                    // don't navigate away from the field on tab when selecting an item
                    .bind("keydown", function(event) {
                        if (event.keyCode === $.ui.keyCode.TAB &&
                            $(this).autocomplete("instance").menu.active ) {
                            event.preventDefault();
                        }
                    })
                    .autocomplete( {
                        minLength: 0,
                        source: _.bind(function(request, response) {
                            // delegate back to autocomplete, but extract the last term
                            response($.ui.autocomplete.filter(
                                this.model.defaultUserRoles, extractLast(request.term)
                            ));
                        }, this),
                        focus: function() {
                            // prevent value inserted on focus
                            return false;
                        },
                        select: function(event, ui) {
                            var terms = split(this.value);
                            // remove the current input
                            terms.pop();
                            // add the selected item
                            terms.push( ui.item.value );

                            // add placeholder to get the comma-and-space at the end
                            terms.push("");
                            this.value = terms.join(", ");
                            return false;
                        }
                    });

                this.model.modules[moduleId].basicEditor.on("change", _.bind(function () {
                    this.makeCustomEditorChanges(moduleId);
                }, this));

                this.model.modules[moduleId].advancedEditor.on("change", _.bind(function () {
                    this.makeCustomEditorChanges(moduleId);
                }, this));

                this.model.modules[moduleId].module.find(".moduleType").val(moduleType);
                this.model.modules[moduleId].module.find(".advancedForm > div > .ui-widget-content").hide();
                this.model.modules[moduleId].module.find(".advancedForm>div>h3").prepend("<i class='advancedShowHide fa fa-caret-right'></i>");
                //this.model.modules[moduleId].module.find(".basicForm>div>h3").prepend("<i class='advancedShowHide fa fa-lg fa-minus-square-o'></i>");
                this.makeCustomEditorChanges(moduleId);

            } else {
                this.$el.find("#" + moduleId + " .authModuleName").html($.t("templates.auth.defaultTitle"));
            }
        },

        /**
         * Clones the base module and creates an infrastructure for module.
         *
         * @returns {string} keyName
         */
        addNewModule: function(e) {
            if (e) {
                e.preventDefault();
            }

            var newModule = this.$el.find("#group-copy").clone(),
                keyName = "module_" + this.model.moduleIndex++;

            // clones hidden HTML and reassigns the id, adds it to the bottom of the acocrdion and opens it
            newModule.attr("id", keyName);

            this.$el.find(".authenticationModules .group-body").append(newModule);
            this.toggleModule($(newModule));

            this.model.modules[keyName] = {};
            this.model.modules[keyName].module = newModule;
            this.model.modules[keyName].basicEditor = null;
            this.model.modules[keyName].advancedEditor = null;
            this.model.modules[keyName].name = "";

            return keyName;
        },

        // When the module is opened or closed toggle the ui appropriately and update the display setting of the userorgroup fields
        toggleModule: function(which) {
            var selector,
                userorgroup = false,
                tempKey,
                userGroupEditorNode;

            if (which.target) {
                selector = $(which.target).closest(".group").find(".content");
            } else {
                selector = $(which).closest(".group").find(".content");
            }

            selector.toggleClass("collapse");

            if (selector.is(":visible")) {
                tempKey = $(which.target).closest(".group").attr("id");

                // The conditional display of roles or groupmembership doesn't load as expected from JsonEditor, custom code to show and hide is necessary
                if (this.model.modules[tempKey]) {
                    userGroupEditorNode = this.model.modules[tempKey].basicEditor.getEditor("root.propertyMapping.userorgroup") || this.model.modules[tempKey].advancedEditor.getEditor("root.propertyMapping.userorgroup");
                    if (typeof userGroupEditorNode !== "undefined" && userGroupEditorNode !== null) {
                        userorgroup = userGroupEditorNode.value;

                        if (_(userorgroup).isString() && userorgroup.length > 0) {
                            userGroupEditorNode.switchEditor(1);
                            this.$el.find("#" + tempKey + " .container-userorgroup select").val($.t("templates.auth.userRoles"));

                        } else if (_(userorgroup).isObject()) {
                            userGroupEditorNode.switchEditor(2);
                            this.$el.find("#" + tempKey + " .container-userorgroup select").val($.t("templates.auth.groupMembership"));

                        } else {
                            userGroupEditorNode.switchEditor(0);
                            this.$el.find("#" + tempKey + " .container-userorgroup select").val($.t("templates.auth.selectOption"));
                        }
                    }
                }
            }
        },

        trashModule: function(which) {
            var id = $(which.target).closest(".group").attr("id");
            this.$el.find("#" + id).remove();
            this.makeCustomEditorChanges();
            delete this.model.modules[id];
        },

        downModule: function(which) {
            var currentGroup = $(which.target).closest(".group");
            currentGroup.next(".group").after(currentGroup);
        },

        upModule: function(which) {
            var currentGroup = $(which.target).closest(".group");
            currentGroup.prev(".group").before(currentGroup);
        },

        makeCustomEditorChanges: function(which) {
            var toUpdate = which || null,
                basicEditor,
                advancedEditor,
                authMod,
                openamDeploymentUrl,
                openamLoginUrl;

            if (toUpdate) {
                basicEditor = this.model.modules[toUpdate].basicEditor.getValue();
                advancedEditor = this.model.modules[toUpdate].advancedEditor.getValue();
                authMod = this.model.modules[toUpdate].name;

                this.$el.find("#" + toUpdate + " .authModuleName").html(authMod);

                if (authMod === "STATIC_USER" && _.has(basicEditor, "username")) {
                    this.$el.find("#" + toUpdate + " .authModuleResource").html(basicEditor.username);
                } else if(_.has(basicEditor, "queryOnResource")) {
                    this.$el.find("#" + toUpdate + " .authModuleResource").html(basicEditor.queryOnResource);
                } else if(_.has(advancedEditor, "queryOnResource")) {
                    this.$el.find("#" + toUpdate + " .authModuleResource").html(advancedEditor.queryOnResource);
                }

                if ( (_.has(basicEditor, "enabled") && !basicEditor.enabled) || (_.has(advancedEditor, "enabled") && !advancedEditor.enabled)) {
                    $(this.model.modules[toUpdate].module).find(".authModuleDisabled").toggleClass("noteHidden", false);
                } else {
                    $(this.model.modules[toUpdate].module).find(".authModuleDisabled").toggleClass("noteHidden", true);
                }

                $(this.model.modules[toUpdate].module).find("input[type='hidden']").parent().hide();

                if(authMod === "OPENAM_SESSION"){
                    openamDeploymentUrl = $(this.model.modules[toUpdate].module).find("input[name*='openamDeploymentUrl']");
                    openamLoginUrl = $(this.model.modules[toUpdate].module).find("input[name*='openamLoginUrl']");
                    openamDeploymentUrl.focus(function(){
                        openamDeploymentUrl.attr("beforeValue",openamDeploymentUrl.val());
                    });
                    //unbinding here so that this function does not get bound multiple times
                    openamDeploymentUrl.unbind("blur").blur(_.bind(function(){
                        this.model.modules[toUpdate].advancedEditor.getEditor('root.openamLoginUrl').setValue(
                            openamLoginUrl.val().replace(openamDeploymentUrl.attr("beforeValue"),openamDeploymentUrl.val())
                        );
                    },this));
                }
            }
        },

        // Creates an Authentication JSON based off of the object the page was loaded with and any new or changed values
        submitAuthModules: function(e) {
            var allGroups = this.$el.find(".group-body .group").not("#group-copy"),
                authModules = [],
                tempID = null,
                tempEditor = null,
                tempModule = {},
                newAuth,
                tempUserRoles,
                amUISettingsProm;

            $(e.target).prop("disabled",true);

            // Each Auth Module
            _(allGroups).each(function(group) {
                tempID = $(group).attr("id");

                if (this.model.modules[tempID].name.length > 0) {
                    tempEditor = _.extend(this.model.modules[tempID].basicEditor.getValue(), this.model.modules[tempID].advancedEditor.getValue());
                    tempModule = {};
                    tempModule.name = this.model.modules[tempID].name;
                    tempModule.enabled = tempEditor.enabled;
                    tempModule.properties = {};
                    tempModule.properties.propertyMapping = {};

                    if(tempModule.name === "OPENAM_SESSION"){
                        amUISettingsProm = this.handleOpenAMUISettings(tempEditor,tempID,authModules);
                        //remove amUIProperties
                        tempEditor = _.omit(tempEditor,this.model.amUIProperties);
                        tempEditor.truststoreType = this.model.amTruststoreType;
                        tempEditor.truststoreFile = this.model.amTruststoreFile;
                        tempEditor.truststorePassword = this.model.amTruststorePassword;
                    }

                    // Set root level properties that are not objects
                    _.chain(tempEditor)
                        .omit(["authModule", "customProperties", "propertyMapping", "enabled", "augmentSecurityContext", "defaultUserRoles"])
                        .each(function (property, key) {
                            if ((_(property).isString() && property.length > 0) || (!_(property).isString())) {
                                tempModule.properties[key] = property;
                            }
                        });

                    _(tempEditor.customProperties).each(function (customProperty) {
                        tempModule.properties[customProperty.propertyName] = customProperty.propertyType;
                    });

                    _.chain(tempEditor.propertyMapping)
                        .omit(["userorgroup"])
                        .each(function (property, key) {
                            tempModule.properties.propertyMapping[key] = property;
                        });

                    tempUserRoles = $(this.model.modules[tempID].defaultUserRoles).val().split(", ");
                    tempModule.properties.defaultUserRoles = [];

                    tempModule.properties.defaultUserRoles = _.filter(tempUserRoles, function (role) {
                        return role.length > 0;
                    });

                    if (this.model.modules[tempID].scriptEditor.generateScript() !== null) {
                        tempModule.properties.augmentSecurityContext = this.model.modules[tempID].scriptEditor.generateScript();
                    }

                    // these are undefined for the STATIC_USER module which has no propertyMapping
                    if (typeof tempEditor.propertyMapping !== "undefined"
                        && tempEditor.propertyMapping !== null
                        && typeof tempEditor.propertyMapping.userorgroup !== "undefined"
                        && tempEditor.propertyMapping.userorgroup !== null) {

                        if (tempEditor.propertyMapping.userorgroup.length > 0) {
                            tempModule.properties.propertyMapping.userRoles = tempEditor.propertyMapping.userorgroup;

                        } else if (!_.isUndefined(tempEditor.propertyMapping.userorgroup.grpMembership)) {
                            tempModule.properties.propertyMapping.groupMembership = tempEditor.propertyMapping.userorgroup.grpMembership;
                            tempModule.properties.groupRoleMapping = {};

                            _(tempEditor.propertyMapping.userorgroup.groupRoleMapping).each(function (mapping) {
                                tempModule.properties.groupRoleMapping[mapping.roleName] = mapping.groupMapping;
                            });
                        }
                    }

                    authModules.push(tempModule);
                }
            }, this);

            if (!amUISettingsProm){
                amUISettingsProm = $.Deferred().resolve();
            }

            amUISettingsProm.then(_.bind(function(){
                newAuth = _(this.model.defaultAuth).clone();
                newAuth.serverAuthContext.authModules = authModules;

                // Set the session module properties and convert sessionOnly back to a boolean
                _(newAuth.serverAuthContext.sessionModule.properties).extend(form2js("sessionModuleForm", ".", true));
                newAuth.serverAuthContext.sessionModule.properties.maxTokenLifeMinutes = "";
                newAuth.serverAuthContext.sessionModule.properties.tokenIdleTimeMinutes = "";
                newAuth.serverAuthContext.sessionModule.properties.sessionOnly = newAuth.serverAuthContext.sessionModule.properties.sessionOnly === "true";

                ConfigDelegate.updateEntity("authentication", newAuth).then(function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "authSaveSuccess");
                    $(e.target).prop("disabled",false);
                });
            },this));
        },
        handleOpenAMUISettings: function(formVal,moduleId,authModules){
            var prom = $.Deferred(),
                amSettings = _.pick(formVal,this.model.amUIProperties,"enabled"),
                maxTokenLifeSeconds = this.$el.find("#sessionModuleForm").find("#maxTokenLifeSeconds"),
                tokenIdleTimeSeconds = this.$el.find("#sessionModuleForm").find("#tokenIdleTimeSeconds"),
                confirmed = function(){
                    SiteConfigurationDelegate.getConfiguration().then(function(uiConfig){
                        ConfigDelegate.updateEntity("ui/configuration", { configuration: _.extend(uiConfig, amSettings) }).then(function(){
                            prom.resolve();
                        });
                    });
                },
                resetTokenTimes = _.bind(function(){
                    maxTokenLifeSeconds.val(this.model.defaultTokenTime);
                    tokenIdleTimeSeconds.val(this.model.defaultTokenIdleTime);
                },this);

            amSettings.openamAuthEnabled = amSettings.enabled;
            delete amSettings.enabled;

            if(amSettings.openamAuthEnabled){
                if(amSettings.openamUseExclusively){
                    maxTokenLifeSeconds.val(this.model.amTokenTime);
                    tokenIdleTimeSeconds.val(this.model.amTokenTime);
                }

                if(!amSettings.openamUseExclusively && maxTokenLifeSeconds.val() === this.model.amTokenTime.toString() && tokenIdleTimeSeconds.val() === this.model.amTokenTime.toString()){
                    //Just in case the user forgot to reset these settings
                    //doing this so that the user will be able to login
                    resetTokenTimes();
                }

                //validate openamDeploymentUrl
                openamProxyDelegate.serverinfo(formVal.openamDeploymentUrl).then(_.bind(function(info){
                        if(info.cookieName){
                            //set openamSSOTokenCookieName for this module
                            authModules[parseInt(moduleId.replace("module_",""),10)].properties.openamSSOTokenCookieName = info.cookieName;
                            confirmed();
                        } else {
                            this.$el.find("#submitAuth").prop("disabled",false);
                            UIUtils.jqConfirm($.t("templates.auth.openamDeploymentUrlConfirmation"), confirmed);
                        }
                    },this),
                    _.bind(function(){
                        this.$el.find("#submitAuth").prop("disabled",false);
                        UIUtils.jqConfirm($.t("templates.auth.openamDeploymentUrlConfirmation"), confirmed);
                    },this));
            } else {
                if(maxTokenLifeSeconds.val() === this.model.amTokenTime.toString() && tokenIdleTimeSeconds.val() === this.model.amTokenTime.toString()){
                    //Just in case the user forgot to reset these settings
                    //doing this so that the user will be able to login
                    resetTokenTimes();
                }
                confirmed();
            }

            return prom;
        }
    });

    Handlebars.registerHelper("jsonEditor", function (jsonString) {
        return "{{" + jsonString + "}}";
    });

    return new AuthenticationView();
});

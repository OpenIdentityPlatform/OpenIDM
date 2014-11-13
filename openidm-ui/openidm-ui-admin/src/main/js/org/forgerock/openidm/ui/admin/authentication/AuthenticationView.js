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

/*global define, $, _, Handlebars, form2js, JSONEditor */

define("org/forgerock/openidm/ui/admin/authentication/AuthenticationView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/util/ScriptEditor",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate"
], function(AdminAbstractView, eventManager, constants, router, ConfigDelegate, ScriptEditor, ConnectorDelegate) {

    var AuthenticationView = AdminAbstractView.extend({
        template: "templates/admin/authentication/AuthenticationTemplate.html",
        element: "#authenticationContainer",
        noBaseTemplate: true,
        events: {
            "click .edit": "toggleModule",
            "click .trash": "trashModule",
            "click .down": "downModule",
            "click .up": "upModule",
            "click #addNew": "addNewModule",
            "click #submitAuth": "submitAuthModules",
            "click .form>div>h3": "toggleBasicAdvanced",
            "change #moduleType": "newModuleTypeSet"
        },

        model: {
            moduleIndex: 0,
            modules: {},
            defaultAuth: {},
            module_types: {
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
                "openidm-authorize",
                "openidm-cert",
                "openidm-reg",
                "openidm-task-manager"
            ],
            resources: []
        },

        render: function (args, callback) {
            // Set JSONEditor defaults
            _(JSONEditor.defaults.options).extend({
                disable_edit_json: true,
                disable_array_reorder: true,
                disable_collapse: true,
                disable_properties: true,
                show_errors: 'always',
                template: 'handlebars',
                theme: 'jqueryui',
                no_additional_properties: false,
                required_by_default: true
            });

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
                                url: "templates/admin/authentication/" + moduleName + ".json",
                                type: "GET",
                                success: _.bind(function(jsonTemplate) {
                                    jsonTemplate = Handlebars.compile(jsonTemplate)();
                                    jsonTemplate = $.parseJSON(jsonTemplate);


                                    // The following code updates the enum values for the queryOnResource property
                                    // Internal User Modules will only need access to the internal repo and can be excluded as that value is set in the template.
                                    if (jsonTemplate.templateName !== "INTERNAL_USER" &&
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
                            header: "> div > h3",
                            collapsible: true,
                            icons: false,
                            event: false,
                            active: false
                        })
                        .sortable({
                            axis: "y",
                            handle: "h3",
                            stop: function(event, ui) {
                                // IE doesn't register the blur when sorting
                                // so trigger focusout handlers to remove .ui-state-focus
                                ui.item.children("h3").triggerHandler("focusout");

                                // Refresh accordion to handle new order
                                $(this).accordion("refresh");
                            }
                        });
                    this.loadDefaults();
                }, this));
            }, this));
        },

        toggleBasicAdvanced: function(e) {
            this.$el.find(e.target).parentsUntil(".form").find(".advancedShowHide").toggleClass("fa-minus-square-o");
            this.$el.find(e.target).parentsUntil(".form").find(".advancedShowHide").toggleClass("fa-plus-square-o");
            this.$el.find(e.target).parentsUntil(".form").find(".ui-widget-content").first().toggle();
        },

        //  Gets the current authentication JSON file and creates UI modules for each one
        loadDefaults: function() {
            ConfigDelegate.readEntity("authentication").then(_.bind(function (auth) {
                this.model.defaultAuth = auth;

                this.$el.find("#maxTokenLifeMinutes").val(auth.serverAuthContext.sessionModule.properties.maxTokenLifeMinutes);
                this.$el.find("#tokenIdleTimeMinutes").val(auth.serverAuthContext.sessionModule.properties.tokenIdleTimeMinutes);
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
                    schema: this.model.module_types[moduleType].mainSchema
                });

                this.model.modules[moduleId].advancedEditor = new JSONEditor(this.model.modules[moduleId].module.find(".advancedForm")[0], {
                    schema: this.model.module_types[moduleType].advancedSchema
                });

                this.model.modules[moduleId].scriptEditor = ScriptEditor.generateScriptEditor({
                    "element": this.$el.find("#"+moduleId).find(".container-augmentSecurityContext .ui-widget-content"),
                    "eventName": " ",
                    "deleteElement": false,
                    "deleteCallback": _.bind(function() {
                        this.model.modules[moduleId].scriptEditor.clearScriptHook();
                    }, this),
                    "scriptData": scriptData,
                    "saveCallback": _.noop()
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
                this.model.modules[moduleId].module.find(".advancedForm>div>h3").prepend("<i class='advancedShowHide fa fa-lg fa-plus-square-o'></i>");
                this.model.modules[moduleId].module.find(".basicForm>div>h3").prepend("<i class='advancedShowHide fa fa-lg fa-minus-square-o'></i>");
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
        addNewModule: function() {
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
                selector = $(which.target).closest(".group").find(".ui-accordion-content");
            } else {
                selector = $(which).closest(".group").find(".ui-accordion-content");
            }

            selector.toggle();

            if (selector.is(":visible")) {
                tempKey = $(which.target).closest(".group").attr("id");

                // The conditional display of roles or groupmembership doesn't load as expected from JsonEditor, custom code to show and hide is necessary
                if (this.model.modules[tempKey]) {
                    userGroupEditorNode = this.model.modules[tempKey].basicEditor.getEditor("root.propertyMapping.userorgroup") || this.model.modules[tempKey].advancedEditor.getEditor("root.propertyMapping.userorgroup");
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

        // Checks for errors and sets the title value on form change
        makeCustomEditorChanges: function(which) {
            var toUpdate = which || null,
                basicEditor,
                advancedEditor,
                authMod,
                errors,
                error = false;

            if (toUpdate) {
                basicEditor = this.model.modules[toUpdate].basicEditor.getValue();
                advancedEditor = this.model.modules[toUpdate].advancedEditor.getValue();
                authMod = this.model.modules[toUpdate].name;
                errors = this.model.modules[toUpdate].module.find(".ui-state-error:visible");

                if(_.has(basicEditor, "queryOnResource")) {
                    this.$el.find("#" + toUpdate + " .authModuleName").html(authMod + " - " + basicEditor.queryOnResource);
                } else if(_.has(advancedEditor, "queryOnResource")) {
                    this.$el.find("#" + toUpdate + " .authModuleName").html(authMod + " - " + advancedEditor.queryOnResource);
                } else {
                    this.$el.find("#" + toUpdate + " .authModuleName").html(authMod);
                }

                if ( (_.has(basicEditor, "enabled") && !basicEditor.enabled) || (_.has(advancedEditor, "enabled") && !advancedEditor.enabled)) {
                    $(this.model.modules[toUpdate].module).find(".authModuleDisabled").toggleClass("noteHidden", false);
                } else {
                    $(this.model.modules[toUpdate].module).find(".authModuleDisabled").toggleClass("noteHidden", true);
                }

                if (errors.length > 0) {
                    $(this.model.modules[toUpdate].module).find(".authModuleErrors").toggleClass("noteHidden", false);
                } else {
                    $(this.model.modules[toUpdate].module).find(".authModuleErrors").toggleClass("noteHidden", true);
                }

                $(this.model.modules[toUpdate].module).find(".form input[type='hidden']").parent().hide();
            }

            _(this.$el.find(".authModuleErrors")).each(function(module) {
                if (!$(module).hasClass("noteHidden")) {
                    error = true;
                }
            }, this);

            if (error) {
                this.$el.find("#authErrorMessage").show();
                this.$el.find("#submitAuth").prop('disabled', true);

            } else {
                this.$el.find("#authErrorMessage").hide();
                this.$el.find("#submitAuth").prop('disabled', false);
            }
        },

        // Creates an Authentication JSON based off of the object the page was loaded with and any new or changed values
        submitAuthModules: function() {
            var allGroups = this.$el.find(".group-body .group").not("#group-copy"),
                authModules = [],
                tempID = null,
                tempEditor = null,
                tempModule = {},
                newAuth,
                tempUserRoles;

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

                    if (this.model.modules[tempID].scriptEditor.getScriptHook() !== null) {
                        tempModule.properties.augmentSecurityContext = this.model.modules[tempID].scriptEditor.getScriptHook().script;
                    }

                    if (tempEditor.propertyMapping.userorgroup.length > 0) {
                        tempModule.properties.propertyMapping.userRoles = tempEditor.propertyMapping.userorgroup;

                    } else if (!_.isUndefined(tempEditor.propertyMapping.userorgroup.grpMembership)) {
                        tempModule.properties.propertyMapping.groupMembership = tempEditor.propertyMapping.userorgroup.grpMembership;
                        tempModule.properties.groupRoleMapping = {};

                        _(tempEditor.propertyMapping.userorgroup.groupRoleMapping).each(function (mapping) {
                            tempModule.properties.groupRoleMapping[mapping.roleName] = mapping.groupMapping;
                        });
                    }

                    if (_(this.model.modules[tempID].scriptEditor.getScriptHook().script).isObject()) {
                        tempModule.properties.augmentSecurityContext = this.model.modules[tempID].scriptEditor.getScriptHook().script;
                    }
                    authModules.push(tempModule);
                }
            }, this);

            newAuth = _(this.model.defaultAuth).clone();
            newAuth.serverAuthContext.authModules = authModules;

            // Set the session module properties and convert sessionOnly back to a boolean
            _(newAuth.serverAuthContext.sessionModule.properties).extend(form2js("sessionModuleForm", ".", true));
            newAuth.serverAuthContext.sessionModule.properties.sessionOnly = newAuth.serverAuthContext.sessionModule.properties.sessionOnly === "true";

            ConfigDelegate.updateEntity("authentication", newAuth).then(function() {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "authSaveSuccess");
            });
        }
    });

    Handlebars.registerHelper("jsonEditor", function (jsonString) {
        return "{{" + jsonString + "}}";
    });

    return new AuthenticationView();
});
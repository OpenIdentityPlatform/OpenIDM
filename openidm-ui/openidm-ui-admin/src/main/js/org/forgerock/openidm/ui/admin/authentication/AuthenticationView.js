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
    "org/forgerock/openidm/ui/admin/util/ScriptEditor"
], function(AdminAbstractView, eventManager, constants, router, ConfigDelegate, ScriptEditor) {

    var AuthenticationView = AdminAbstractView.extend({
        template: "templates/admin/authentication/AuthenticationTemplate.html",
        events: {
            "click .edit": "toggleModule",
            "click .trash": "trashModule",
            "click .down": "downModule",
            "click .up": "upModule",
            "click #addNew": "addNewModule",
            "click #submitAuth": "submitAuthModules"
        },
        moduleIndex: 0,
        modules: {},
        defaultAuth: {},

        render: function (args, callback) {

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

            this.parentRender(_.bind(function(){
                $(".authenticationModules .group-body")
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
                        stop: function( event, ui ) {
                            // IE doesn't register the blur when sorting
                            // so trigger focusout handlers to remove .ui-state-focus
                            ui.item.children( "h3" ).triggerHandler( "focusout" );

                            // Refresh accordion to handle new order
                            $( this ).accordion( "refresh" );
                        }
                    });
                this.loadDefaults();
            }, this));
        },

        //  Gets the current authentication JSON file and creates UI modules for each one
        loadDefaults: function() {
            ConfigDelegate.readEntity("authentication").then(_.bind(function (auth) {
                var tempKey,
                    jsonEditorFormat,
                    tempVar;

                this.defaultAuth = auth;

                $("#maxTokenLifeMinutes").val(auth.serverAuthContext.sessionModule.properties.maxTokenLifeMinutes);
                $("#tokenIdleTimeMinutes").val(auth.serverAuthContext.sessionModule.properties.tokenIdleTimeMinutes);
                $("#sessionOnly").val(auth.serverAuthContext.sessionModule.properties.sessionOnly.toString());

                _(auth.serverAuthContext.authModules).each(function(module) {
                    tempKey = this.addNewModule(false);

                    this.modules[tempKey].scriptEditor = ScriptEditor.generateScriptEditor({
                        "element": $("#"+tempKey).find(".container-augmentSecurityContext .ui-widget-content"),
                        "eventName": "",
                        "deleteElement": false,
                        "deleteCallback": _.bind(function(){
                            this.modules[tempKey].scriptEditor.clearScriptHook();
                        }, this),
                        "scriptData": module.properties.augmentSecurityContext
                    });


                    jsonEditorFormat = this.modules[tempKey].editor.getValue();
                    this.toggleModule(this.modules[tempKey].module);

                    jsonEditorFormat.enabled = module.enabled;
                    jsonEditorFormat.name = module.name;
                    jsonEditorFormat.customProperties = [];


                    _(module.properties).each(function(property, key) {
                        if (_(jsonEditorFormat).has(key)) {

                            if (key === "propertyMapping") {
                                _(property).each(function(propertyMapValue, propertyMapKey) {

                                    // Generic propertyMapping property: is truthy only if the property exists on the main object
                                    if (_(jsonEditorFormat.propertyMapping).has(propertyMapKey)) {
                                        jsonEditorFormat.propertyMapping[propertyMapKey] = propertyMapValue;

                                        // The following two cases would normally be found on the main object,
                                        // but because of formatting from json editor they must be called out directly.
                                    } else if (propertyMapKey === "userRoles") {
                                        jsonEditorFormat.propertyMapping.userorgroup = propertyMapValue;
                                    } else if (propertyMapKey ==="groupMembership") {
                                        tempVar = [];
                                        _(module.properties.groupRoleMapping).map(function(map, key){
                                            tempVar.push({
                                                roleName: key,
                                                groupMapping: map
                                            });
                                        });

                                        jsonEditorFormat.propertyMapping.userorgroup = {
                                            grpMembership: propertyMapValue,
                                            groupRoleMapping: tempVar
                                        };
                                    }
                                }, this);

                                // Root level property
                            } else {
                                jsonEditorFormat[key] = property;
                            }
                            // Custom property on the root level
                        } else if (key !== "groupRoleMapping") {
                            jsonEditorFormat.customProperties.push({
                                propertyName: key,
                                propertyType: property
                            });
                        }

                    }, this);

                    this.modules[tempKey].editor.setValue(jsonEditorFormat);
                    this.makeCustomEditorChanges(tempKey);
                }, this);
            }, this));
        },

        // When the module is opened or closed toggle the ui appropriately and update the display setting of the userorgroup fields
        toggleModule: function(which) {
            var selector, userorgroup = false, tempKey;

            if (which.target) {
                selector = $(which.target).closest(".group").find(".ui-accordion-content");
            } else {
                selector = $(which).closest(".group").find(".ui-accordion-content");
            }

            selector.toggle();

            if (selector.is(":visible")) {
                tempKey = $(which.target).closest(".group").attr("id");

                // The conditional display of roles or groupmembership doesn't load as expected from JsonEditor, custom code to show and hide is necessary
                if (this.modules[tempKey]) {
                    userorgroup = this.modules[tempKey].editor.getEditor("root.propertyMapping.userorgroup").value || false;

                    if (_(userorgroup).isString()) {
                        this.modules[tempKey].editor.getEditor("root.propertyMapping.userorgroup").switchEditor(1);
                        $("#" + tempKey + " .container-userorgroup select").val($.t("templates.auth.userRoles"));

                    } else if (_(userorgroup).isObject()) {
                        this.modules[tempKey].editor.getEditor("root.propertyMapping.userorgroup").switchEditor(2);
                        $("#" + tempKey + " .container-userorgroup select").val($.t("templates.auth.groupMembership"));

                    } else {
                        this.modules[tempKey].editor.getEditor("root.propertyMapping.userorgroup").switchEditor(0);
                        $("#" + tempKey + " .container-userorgroup select").val($.t("templates.auth.selectOption"));
                    }
                }
            }
        },

        trashModule: function(which) {
            var id = $(which.target).closest(".group").attr("id");
            $("#" + id).remove();
            this.makeCustomEditorChanges();
            delete this.modules[id];
        },

        downModule: function(which) {
            var currentGroup = $(which.target).closest(".group");
            currentGroup.next(".group").after(currentGroup);
        },

        upModule: function(which) {
            var currentGroup = $(which.target).closest(".group");
            currentGroup.prev(".group").before(currentGroup);
        },

        addNewModule: function() {
            var newModule = $("#group-copy").clone(),
                keyName = "module_"+this.moduleIndex++;

            // clones hidden HTML and reassigns the id, adds it to the bottom of the acocrdion and opens it
            newModule.attr("id", keyName);

            $(".authenticationModules .group-body").append(newModule);
            this.toggleModule($(newModule));

            this.modules[keyName] = {};
            this.modules[keyName].module = newModule;
            this.modules[keyName].editor = (new JSONEditor(newModule.find(".form")[0], {
                schema: {
                    title: $.t("templates.auth.authFieldsetName"),
                    type: "object",
                    properties: {
                        name: {
                            title: $.t("templates.auth.type"),
                            type: "string",
                            "enum": ["INTERNAL_USER", "IWA", "MANAGED_USER", "CLIENT_CERT", "PASSTHROUGH"],
                            required: true,
                            "default": "INTERNAL_USER"
                        },

                        enabled: {
                            title: $.t("templates.auth.moduleEnabled"),
                            type: "boolean",
                            required: true,
                            "default": true
                        },

                        queryOnResource: {
                            title: $.t("templates.auth.queryOnResource"),
                            type: "string",
                            minLength: 1
                        },

                        queryId: {
                            title: $.t("templates.auth.queryId"),
                            type: "string"
                        },

                        defaultUserRoles: {
                            title: $.t("templates.auth.defaultUserRole"),
                            type: "array",
                            format: "table",
                            items: {
                                type: "string",
                                title: $.t("templates.auth.role")
                            }
                        },

                        propertyMapping: {
                            type: "object",
                            title: $.t("templates.auth.propertyMapping"),
                            properties: {
                                authenticationId: {
                                    title: $.t("templates.auth.authId"),
                                    type: "string"
                                },
                                userCredential: {
                                    title: $.t("templates.auth.userCred"),
                                    type: "string"
                                },

                                userorgroup: {
                                    title: $.t("templates.auth.userOrGroup"),
                                    oneOf: [
                                        {
                                            title: $.t("templates.auth.selectOption"),
                                            type: "string",
                                            format: "hidden"
                                        }, {
                                            title: $.t("templates.auth.userRoles"),
                                            $ref: "#/definitions/UserRoles"
                                        }, {
                                            title: $.t("templates.auth.groupMembership"),
                                            $ref: "#/definitions/GroupMembership"
                                        }
                                    ]
                                }
                            }
                        },

                        augmentSecurityContext: {
                            title: $.t("templates.auth.augmentSecurityContext"),
                            type: "object"
                        },

                        customProperties: {
                            title: $.t("templates.auth.customProp"),
                            type: "array",
                            items: {
                                type: "object",
                                title: $.t("templates.auth.property"),
                                headerTemplate: "{{self.propertyName}}",
                                properties: {
                                    propertyName: {
                                        title: $.t("templates.auth.propertyName"),
                                        type: "string"
                                    },
                                    propertyType: {
                                        title: $.t("templates.auth.propertyType"),
                                        oneOf: [
                                            {
                                                type: "string",
                                                title: $.t("templates.auth.string")
                                            }, {
                                                title: $.t("templates.auth.simpleArray"),
                                                type: "array",
                                                format: "table",
                                                items: {
                                                    type: "string",
                                                    title: $.t("templates.auth.value")
                                                }
                                            }
                                        ]
                                    }
                                }
                            }
                        }
                    },

                    definitions: {
                        UserRoles: {
                            type: "string",
                            title: ""
                        },

                        GroupMembership: {
                            type: "object",
                            properties: {
                                grpMembership: {
                                    type: "string",
                                    title: $.t("templates.auth.groupMembership")
                                },
                                groupRoleMapping: {
                                    title: $.t("templates.auth.groupRoleMapping"),
                                    type: "array",
                                    "default": [
                                        {roleName:"openidm-admin", groupMapping: []}
                                    ],
                                    items: {
                                        type: "object",
                                        title: $.t("templates.auth.role"),
                                        headerTemplate: "{{self.roleName}}",
                                        properties: {
                                            roleName: {
                                                type: "string",
                                                title: $.t("templates.auth.roleName")
                                            },
                                            groupMapping: {
                                                title: $.t("templates.auth.groupMappings"),
                                                type: "array",
                                                format: "table",
                                                items: {
                                                    type: "string",
                                                    title: $.t("templates.auth.group")
                                                }
                                            }
                                        }

                                    }
                                }
                            }
                        }
                    }
                }
            }));

            this.modules[keyName].editor.on("change", _.bind(function(){
                this.makeCustomEditorChanges(keyName);
            }, this));

            this.makeCustomEditorChanges(keyName);

            return keyName;
        },

        // Checks for errors and sets the title value on form change
        makeCustomEditorChanges: function(which) {
            var toUpdate = which || null,
                editor,
                authMod,
                errors,
                error = false;

            if (toUpdate) {
                editor = this.modules[toUpdate].editor.getValue();
                authMod = editor.name;
                errors = this.modules[toUpdate].module.find(".ui-state-error:visible");

                // Set title
                if (editor.queryOnResource) {
                    $("#" + toUpdate + " .authModuleName").html(authMod + " - " + editor.queryOnResource);
                } else {
                    $("#" + toUpdate + " .authModuleName").html(authMod);
                }

                // show / hide disabled note
                if (!editor.enabled) {
                    $(this.modules[toUpdate].module).find(".authModuleDisabled").toggleClass("noteHidden", false);
                } else {
                    $(this.modules[toUpdate].module).find(".authModuleDisabled").toggleClass("noteHidden", true);
                }

                // show / hide errors note
                if (errors.length > 0) {
                    $(this.modules[toUpdate].module).find(".authModuleErrors").toggleClass("noteHidden", false);
                } else {
                    $(this.modules[toUpdate].module).find(".authModuleErrors").toggleClass("noteHidden", true);
                }

                // Hide the whole hidden group
                $(this.modules[toUpdate].module).find(".form input[type='hidden']").parent().hide();
            }

            _($(".authModuleErrors")).each(function(module) {
                if (!$(module).hasClass("noteHidden")) {
                    error = true;
                }
            }, this);

            if (error) {
                $("#authErrorMessage").show();
                $("#submitAuth").prop('disabled', true);

            } else {
                $("#authErrorMessage").hide();
                $("#submitAuth").prop('disabled', false);
            }
        },

        // Creates an Authentication JSON based off of the object the page was loaded with and any new or changed values
        submitAuthModules: function() {
            var allGroups = $(".group-body .group").not("#group-copy"),
                authModules = [],
                tempID = null,
                tempEditor = null,
                tempModule = {},
                newAuth;

            // Each Auth Module
            _(allGroups).each(function(group) {
                tempID = $(group).attr("id");
                tempEditor = this.modules[tempID].editor.getValue();
                tempModule = {};

                tempModule.name = tempEditor.name;
                tempModule.enabled = tempEditor.enabled;
                tempModule.properties = {};
                tempModule.properties.propertyMapping = {};

                // Set root level properties that are not objects
                _.chain(tempEditor)
                    .omit(["authModule", "customProperties", "propertyMapping", "enabled", "augmentSecurityContext"])
                    .each(function(property, key) {
                        if ( (_(property).isString() && property.length > 0) || (!_(property).isString())) {
                            tempModule.properties[key] = property;
                        }
                    });


                _(tempEditor.customProperties).each(function(customProperty) {
                    tempModule.properties[customProperty.propertyName] = customProperty.propertyType;
                });

                _.chain(tempEditor.propertyMapping)
                    .omit(["userorgroup"])
                    .each(function(property, key) {
                        tempModule.properties.propertyMapping[key] = property;
                    });


                if (!_(tempEditor.augmentSecurityContext.file).isEmpty() ){
                    tempModule.properties.augmentSecurityContext = {};
                    tempModule.properties.augmentSecurityContext.type = tempEditor.augmentSecurityContext.type;
                    tempModule.properties.augmentSecurityContext.file = tempEditor.augmentSecurityContext.file;

                } else if (!_(tempEditor.augmentSecurityContext.source).isEmpty()) {
                    tempModule.properties.augmentSecurityContext = {};
                    tempModule.properties.augmentSecurityContext.type = tempEditor.augmentSecurityContext.type;
                    tempModule.properties.augmentSecurityContext.source = tempEditor.augmentSecurityContext.source;
                }

                if (this.modules[tempID].module.find(".container-userorgroup select").val() === $.t("templates.auth.userRoles")) {
                    tempModule.properties.propertyMapping.userRoles = tempEditor.propertyMapping.userorgroup;

                } else if (this.modules[tempID].module.find(".container-userorgroup select").val() === $.t("templates.auth.groupMembership")) {
                    tempModule.properties.propertyMapping.groupMembership = tempEditor.propertyMapping.userorgroup.grpMembership;
                    tempModule.properties.groupRoleMapping = {};

                    _(tempEditor.propertyMapping.userorgroup.groupRoleMapping).each(function(mapping) {
                        tempModule.properties.groupRoleMapping[mapping.roleName] = mapping.groupMapping;
                    });
                }

                if (_(this.modules[tempID].scriptEditor.getScriptHook().script).isObject()) {
                    tempModule.properties.augmentSecurityContext = this.modules[tempID].scriptEditor.getScriptHook().script;
                }
                authModules.push(tempModule);
            }, this);

            newAuth = _(this.defaultAuth).clone();
            newAuth.serverAuthContext.authModules = authModules;

            // Set the session module properties and convert sessionOnly back to a boolean
            _(newAuth.serverAuthContext.sessionModule.properties).extend(form2js("sessionModuleForm", ".", true));
            newAuth.serverAuthContext.sessionModule.properties.sessionOnly = newAuth.serverAuthContext.sessionModule.properties.sessionOnly === "true";

            ConfigDelegate.updateEntity("authentication", newAuth).then(function() {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "authSaveSuccess");
            });
        }
    });

    return new AuthenticationView();
});


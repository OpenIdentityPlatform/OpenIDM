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
 * Copyright 2011-2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/role/RoleEntitlementsEditView", [
    "jquery",
    "underscore",
    "jsonEditor",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/commons/ui/common/components/Messages",
    "bootstrap-dialog",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function($, _, JSONEditor, AbstractView, eventManager, constants, cookieHelper, uiUtils, resourceDelegate, messagesManager, BootstrapDialog, conf, InlineScriptEditor, configDelegate) {
    var RoleEntitlementsEditView = AbstractView.extend({
        element: "#dialogs",
        template: "templates/admin/role/RoleEntitlementsEditViewTemplate.html",
        noBaseTemplate: true,
        events: {
            "click #btn-add-attribute": "addAttribute",
            "click .btn-delete-attribute": "deleteAttribute",
            "click .mappingLink": "closeDialog",
            "click #addMapping": "addMapping",
            "click .removeMapping": "removeMapping"
        },
        closeDialog: function(e) {
            this.bsDialog.close();
        },
        render: function (args, role, assignmentName, listView, callback) {
            var _this = this,
                dialogTitle = (assignmentName) ? $.t("templates.admin.RoleEntitlementsTemplate.editEntitlement",{entitlementName: assignmentName}) : $.t("templates.admin.RoleEntitlementsTemplate.addEntitlement"),
                removeAssignmentNameFromRoute = _.bind(function() {
                    if(this.data.assignmentRouteParam) {
                        this.data.args.pop();
                        eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "adminEditManagedObjectView", args: this.data.args});
                    }
                }, this);

            this.currentDialog = $('<div id="roleEntitlementsEditContainer"></div>');
            this.data.args = args;
            this.data.role = role;
            this.data.listView = listView;
            this.data.assignment = this.data.role.assignments[assignmentName];
            this.data.assignmentName = assignmentName;
            this.data.JSONEditors = [];
            this.data.serviceUrl = resourceDelegate.getServiceUrl(args);
            this.data.assignmentRouteParam = args[3];

            $('#dialogs').append(this.currentDialog);

            this.setElement(this.currentDialog);

            this.bsDialog = BootstrapDialog.show({
                title: dialogTitle,
                type: BootstrapDialog.TYPE_DEFAULT,
                message: this.currentDialog,
                size: BootstrapDialog.SIZE_WIDE,
                onshown : function (dialogRef) {
                    _this.loadData(callback);
                },
                onhide: function() {
                    removeAssignmentNameFromRoute();
                },
                buttons: [{
                    label: $.t("common.form.cancel"),
                    id:"entitlementEditDialogCancel",
                    action: function(dialogRef) {
                        dialogRef.close();
                    }
                }, {
                    label: $.t('common.form.save'),
                    id: "entitlementEditDialogSubmit",
                    cssClass: "btn-primary",
                    action: function(dialogRef) {
                        _this.saveAssignment(function() {
                            dialogRef.close();
                        });
                    }
                }]
            });
        },
        loadData: function(callback) {
            configDelegate.readEntity("sync").then(_.bind(function(sync) {
                this.data.sync = _.omit(sync,"_id");
                this.data.mappings = _.filter(sync.mappings, function(m) {
                                        return _.contains(m.assignmentsToMap, this.data.assignmentName);
                                     }, this);
                this.data.availableMappings = _.filter(sync.mappings, function(m) {
                                                return !_.contains(m.assignmentsToMap, this.data.assignmentName);
                                              }, this);

                uiUtils.renderTemplate(this.template, this.$el,
                        _.extend({}, conf.globalData, this.data),
                        _.bind(function () {
                            if(!this.data.assignment) {
                                this.data.newAssignment = true;
                                this.addAttribute();
                            } else {
                                this.setJSONEditors();
                                this.data.newAssignment = false;
                            }
                            this.setAttributeOperationsPopovers();
                            this.setLinkQualifers();
                            this.setScripts(callback);
                        }, this), "replace");
            }, this));
        },
        convertAttibuteValueToJSONEditor: function(attrName, attrValue) {
            var editor = new JSONEditor(this.$el.find("[attrName=" + attrName + "]")[0], {
                disable_edit_json: true,
                disable_array_reorder: true,
                disable_collapse: true,
                schema:{},
                uuid: attrName
            });

            editor.on('change', _.bind(function () {
                this.$el.find(".compactJSON div.form-control>:input").addClass("form-control");
            }, this));

            editor.setValue(attrValue);

            return editor;
        },
        setJSONEditors: function() {
            this.data.JSONEditors = _.map(this.data.assignment.attributes, function(attr) {
                return this.convertAttibuteValueToJSONEditor(attr.name, attr.value);
            }, this);
        },
        setScripts: function(callback) {
            var emptyScript = {
                    type: "text/javascript",
                    globals: {},
                    file: ""
                },
                onAssignmentScriptProm = $.Deferred(),
                onUnassignmentScriptProm = $.Deferred();

            this.data.onAssignmentScript = InlineScriptEditor.generateScriptEditor({
                "element": this.$el.find(".event-onAssignment"),
                "eventName": "onAssignment",
                "noValidation": true,
                "scriptData": (this.data.assignment) ? this.data.assignment.onAssignment : emptyScript
            }, function() {
                onAssignmentScriptProm.resolve();
            });

            this.data.onUnassignmentScript = InlineScriptEditor.generateScriptEditor({
                "element": this.$el.find(".event-onUnassignment"),
                "eventName": "onUnassignment",
                "noValidation": true,
                "scriptData": (this.data.assignment) ? this.data.assignment.onUnassignment : emptyScript
            }, function() {
                onUnassignmentScriptProm.resolve();
            });

            $.when(onAssignmentScriptProm, onUnassignmentScriptProm).then(callback);
        },
        addAttribute: function(e) {
            var newAttr = this.$el.find("#attributeTemplate").find(".list-group-item").clone();

            if(e) {
                e.preventDefault();
            }

            newAttr.find(".attribute-value").attr("attrName","newAttribute");

            this.$el.find("#assignment-attributes").append(newAttr);

            this.data.JSONEditors.push(this.convertAttibuteValueToJSONEditor("newAttribute", ""));

            newAttr.find(".attribute-value").removeAttr("attrName");

            this.setAttributeOperationsPopovers();
        },
        deleteAttribute: function(e) {
            var editorIndex = this.$el.find(".assignment-attributes .list-group-item").find(e.target).closest(".list-group-item").index();

            e.preventDefault();

            //remove the JSONEditor for this attribute
            this.data.JSONEditors.splice(editorIndex,1);

            $(e.target).closest(".list-group-item").remove();
        },
        getAttributes: function() {
            return _.map($(".assignment-attributes .list-group-item"), function(attrElement, index) {
                var attr = {};

                attr.name = $(attrElement).find(".attribute-name input").val();
                attr.value = this.data.JSONEditors[index].getValue();
                attr.assignmentOperation = $(attrElement).find(".hiddenOnAssignment").val();
                attr.unassignmentOperation = $(attrElement).find(".hiddenOnUnassignment").val();

                return attr;
            }, this);
        },
        saveAssignment: function(callback) {
            var invalid = false,
                doSave = _.bind(function() {
                    var onAssignment = this.data.onAssignmentScript.generateScript(),
                        onUnassignment = this.data.onUnassignmentScript.generateScript(),
                        linkQualifiers = this.$el.find("#assignmentLinkQualifiers").val().split(",");

                    this.data.role.assignments[this.data.assignmentName] = { attributes: this.getAttributes() };

                    if(linkQualifiers.length && linkQualifiers[0].length) {
                        this.data.role.assignments[this.data.assignmentName].linkQualifiers = linkQualifiers;
                    }

                    if(onAssignment) {
                        this.data.role.assignments[this.data.assignmentName].onAssignment = onAssignment;
                    }

                    if(onUnassignment) {
                        this.data.role.assignments[this.data.assignmentName].onUnassignment = onUnassignment;
                    }

                    resourceDelegate.updateResource(this.data.serviceUrl, this.data.role._id, this.data.role, _.bind(function() {
                        var msg = (this.data.newAssignment) ? "templates.admin.RoleEntitlementsTemplate.assignmentAddSuccess" : "templates.admin.RoleEntitlementsTemplate.assignmentSaveSuccess";
                        this.data.role._rev++;
                        messagesManager.messages.addMessage({"message": $.t(msg,{ assignment: this.data.assignmentName })});
                        if(callback) {
                            callback();
                        }
                        this.data.listView.render(this.data.args, this.data.role);
                    }, this));
                }, this);

            if(!this.data.assignment) {
                this.data.assignmentName = this.$el.find(".assignment-name").val();
                if(this.data.role.assignments[this.data.assignmentName]) {
                    invalid = true;
                }
            }

            if(invalid) {
                messagesManager.messages.addMessage({"type": "error","message": $.t("templates.admin.RoleEntitlementsTemplate.entitlementDuplicationError")});
            } else {
                doSave();
            }
        },
        setAttributeOperationsPopovers: function() {
            var btns = this.$el.find(".assignment-attributes .btn-toggle-attribute-operations");

            _.each(btns, function(btn) {
                var operations = $(btn).closest(".attribute-operations"),
                    template = operations.find(".operations-popover"),
                    hiddenOnAssignment = operations.find(".hiddenOnAssignment"),
                    hiddenOnUnassignment = operations.find(".hiddenOnUnassignment");

                $(btn).popover({
                    placement:'bottom',
                    html: true,
                    content: template.html()
                }).on('shown.bs.popover', function () {
                    var popup = $(this),
                        operations = popup.closest(".attribute-operations");

                    $(this).next('.popover').find('.btn-hide-attribute-operations').click(function (e) {
                        var popoverForm = operations.find(".popover"),
                            onAssignment = popoverForm.find("[name=onAssignment]").val(),
                            onUnassignment = popoverForm.find("[name=onUnassignment]").val();

                        e.preventDefault();

                        hiddenOnAssignment.val(onAssignment);

                        hiddenOnUnassignment.val(onUnassignment);

                        popup.popover('hide');
                    });

                    $(this).next('.popover').find('[name=onAssignment]').val(hiddenOnAssignment.val());
                    $(this).next('.popover').find('[name=onUnassignment]').val(hiddenOnUnassignment.val());
                });
            });

        },
        mappingAction: function(mappingName,addMapping) {
            var msg = (addMapping) ? "addMappingSuccess" : "removeMappingSuccess";

            this.data.sync.mappings = _.map(this.data.sync.mappings, function(mapping) {
                if(mapping.name === mappingName) {
                    if(addMapping) {
                        if(!mapping.assignmentsToMap) {
                            mapping.assignmentsToMap = [];
                        }
                        if(!_.contains(mapping.assignmentsToMap, this.data.assignmentName)) {
                            mapping.assignmentsToMap.push(this.data.assignmentName);
                        }
                    } else {
                        mapping.assignmentsToMap = _.reject(mapping.assignmentsToMap, function(a) {
                                                        return a === this.data.assignmentName;
                                                   }, this);
                    }
                }

                return mapping;
            }, this);

            configDelegate.updateEntity("sync", this.data.sync).then(_.bind(function() {
                this.loadData(_.bind(function() {
                    this.$el.find('[href="#role-edit-mappings"]').click();
                    messagesManager.messages.addMessage({"message": $.t("templates.admin.RoleEntitlementsTemplate." + msg,{ assignmentName: this.data.assignmentName, mappingName: mappingName })});
                }, this));
            }, this));
        },
        addMapping: function(e) {
            var mappingName = this.$el.find("#mappingSelection").val();

            e.preventDefault();

            if(mappingName.length) {
                this.mappingAction(mappingName, true);
            }
        },
        removeMapping: function(e) {
            var mappingName = $(e.target).closest(".removeMapping").attr("mapping");

            e.preventDefault();

            this.mappingAction(mappingName);
        },
        setLinkQualifers: function() {
            this.$el.find('#assignmentLinkQualifiers').selectize({
                plugins: ['remove_button'],
                delimiter: ',',
                persist: false,
                create: function(input) {
                    return {
                        value: input,
                        text: input
                    };
                }
            });
        }
    });

    return new RoleEntitlementsEditView();
});

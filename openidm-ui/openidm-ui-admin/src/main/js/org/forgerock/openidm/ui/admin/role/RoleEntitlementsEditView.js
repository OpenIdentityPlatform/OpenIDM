/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All rights reserved.
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

/*global define, $, form2js, _, JSONEditor */

/**
 * @author huck.elliott
 */
define("org/forgerock/openidm/ui/admin/role/RoleEntitlementsEditView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/commons/ui/common/components/Messages",
    "bootstrap-dialog",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor"
], function(AbstractView, eventManager, constants, cookieHelper, uiUtils, resourceDelegate, messagesManager, BootstrapDialog, conf, InlineScriptEditor) {
    var RoleEntitlementsEditView = AbstractView.extend({
        element: "#dialogs",
        template: "templates/admin/role/RoleEntitlementsEditViewTemplate.html",
        noBaseTemplate: true,
        events: {
            "click #btn-add-attribute": "addAttribute",
            "click .btn-delete-attribute": "deleteAttribute"
        },
        render: function (args, role, assignmentName, listView, callback) {
            var _this = this,
                dialogTitle = (assignmentName) ? $.t("templates.admin.RoleEntitlementsTemplate.editEntitlement",{entitlementName: assignmentName}) : $.t("templates.admin.RoleEntitlementsTemplate.addEntitlement");

            this.currentDialog = $('<div id="roleEntitlementsEditContainer"></div>');
            this.data.args = args;
            this.data.role = role;
            this.data.listView = listView;
            this.data.assignment = this.data.role.assignments[assignmentName];
            this.data.assignmentName = assignmentName;
            this.data.JSONEditors = [];
            this.data.serviceUrl = resourceDelegate.getServiceUrl(args);

            $('#dialogs').append(this.currentDialog);

            this.setElement(this.currentDialog);
            
            
            BootstrapDialog.show({
                title: dialogTitle,
                type: BootstrapDialog.TYPE_DEFAULT,
                message: this.currentDialog,
                size: BootstrapDialog.SIZE_WIDE,
                onshown : function (dialogRef) {
                    uiUtils.renderTemplate(_this.template, _this.$el,
                            _.extend(conf.globalData, _this.data),
                            function () {
                                if(!_this.data.assignment) {
                                    _this.data.newAssignment = true;
                                    _this.addAttribute();
                                } else {
                                    _this.setJSONEditors();
                                    _this.data.newAssignment = false;
                                }
                                _this.setScripts();
                                _this.setAttributeOperationsPopovers();
                                
                                if(callback){
                                    callback();
                                }
                            }, "replace");
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
        convertAttibuteValueToJSONEditor: function(attrName, attrValue) {
            var editor = new JSONEditor(this.$el.find("[attrName=" + attrName + "]")[0], {
                disable_edit_json: true,
                disable_array_reorder: true,
                disable_collapse: true,
                schema:{}
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
        setScripts: function() {
            var emptyScript = {
                type: "text/javascript",
                globals: {},
                file: ""
            };
            
            this.data.onAssignmentScript = InlineScriptEditor.generateScriptEditor({
                "element": this.$el.find(".event-onAssignment"),
                "eventName": "onAssignment",
                "noValidation": true,
                "scriptData": (this.data.assignment) ? this.data.assignment.onAssignment : emptyScript
            });
            
            this.data.onUnassignmentScript = InlineScriptEditor.generateScriptEditor({
                "element": this.$el.find(".event-onUnassignment"),
                "eventName": "onUnassignment",
                "noValidation": true,
                "scriptData": (this.data.assignment) ? this.data.assignment.onUnassignment : emptyScript
            });
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
                        onUnassignment = this.data.onUnassignmentScript.generateScript();
                    
                    this.data.role.assignments[this.data.assignmentName] = { attributes: this.getAttributes() };

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
            
        }
    }); 
    
    return new RoleEntitlementsEditView();
});



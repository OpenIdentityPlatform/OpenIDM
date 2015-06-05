/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All Rights Reserved
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

/*global require, define, QUnit, $ */

define([
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/resource/EditResourceView",
    "org/forgerock/openidm/ui/common/resource/ResourceEditViewRegistry",
    "org/forgerock/openidm/ui/admin/role/EditRoleView",
    "org/forgerock/openidm/ui/admin/role/RoleEntitlementsListView",
    "org/forgerock/openidm/ui/admin/role/RoleEntitlementsEditView",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/openidm/ui/common/resource/GenericEditResourceView",
    "../mocks/editRole",
    "../mocks/editResource"
], function (
        constants, 
        router, 
        eventManager, 
        conf,
        editResourceView, 
        resourceEditViewRegistry, 
        editRoleView, 
        roleEntitlementsListView, 
        roleEntitlementsEditView, 
        inlineScriptEditor, 
        genericEditResourceView,
        editRoleMock,
        editResourceMock
 ) {

    return {
        executeAll: function (server, callback) {

            module('Edit Resource Data UI Functions');

            QUnit.asyncTest("Edit Role View", function () {
                var registrySpy = sinon.spy(resourceEditViewRegistry,"getEditViewModule"),
                    editRoleSpy = sinon.spy(editRoleView,"render"),
                    childView,
                    args = ["managed","role"],
                    roleEntitlementsListRenderStub = sinon.stub(roleEntitlementsListView,"render", function() {
                        roleEntitlementsListRenderStub.restore();
                    }),
                    roleEntitlementsEditRenderStub = sinon.stub(roleEntitlementsEditView,"render", function() {
                        roleEntitlementsEditRenderStub.restore();
                    }),
                    deleteTest = function() {
                        editRoleView.deleteRole(null,function() {
                            QUnit.ok(true,"Successfully deleted role");
                            QUnit.start();
                        });

                        $("body").one("shown.bs.modal", function () {
                            QUnit.ok($(".bootstrap-dialog-title").text().indexOf("Confirm") > -1, "Delete confirmation dialog opened");
                            $(".bootstrap-dialog-footer-buttons").find(".btn-primary").click();
                        });
                    };
                
                editRoleMock(server);
                
                //managed/role is an example of an edit view override that allows custom editing of a specific managed object
                editResourceView.render(args);
                
                //make sure that the ResourceEditViewRegistry is returning the proper view
                QUnit.ok(registrySpy.calledWith("role"), "ResourceEditViewRegistry.getEditViewModule() called with the 'role' as the resource argument");
                
                childView = registrySpy.returnValues[0];
                QUnit.equal(childView.template, "templates/admin/role/EditRoleViewTemplate.html", "The GenericEditResourceView is properly overridden by EditRoleView");
                
                resourceEditViewRegistry.getEditViewModule.restore();
                
                QUnit.ok(_.isEqual(editRoleSpy.args[0][0],args), "EditRoleView gets the correct arguments from resourceEditViewRegistry");
                
                QUnit.ok(childView.data.newRole,"Based on the args passed to the EditRoleView the 'newRole' variable was properly set to true");
                
                editRoleView.render.restore();
                
                editRoleView.render(args, _.bind(function() {
                    QUnit.ok(!editRoleView.$el.find("[href=#role-entitlements]").is(":visible") && !editRoleView.$el.find("[href=#role-users]").is(":visible"), "Entitlements and Users tabs hidden");
                    
                    this.$el.find('[name="role[properties][name]"]').val("newRole");
                    this.$el.find('[name="role[properties][description]"]').val("newRole description");
                    this.saveRole(null, _.bind(function() {
                        QUnit.ok(_.isEqual(editRoleView.data.args,this.args[0][0]), "New Role successfully saved");
                        QUnit.equal(location.hash, "#resource/managed/role/edit/" +  _.last(editRoleView.data.args), "URL route changed to the correct value after save");
                        QUnit.ok(editRoleView.$el.find("[href=#role-entitlements]").is(":visible") && editRoleView.$el.find("[href=#role-users]").is(":visible"), "Entitlements and Users tabs visible");
                        
                        
                        roleEntitlementsListView.render(editRoleView.data.args, editRoleView.data.role, function() {
                            editRoleView.$el.find(".add-assignment").click();
                            
                            QUnit.ok(_.isEqual(roleEntitlementsEditRenderStub.args[0], [editRoleView.data.args,editRoleView.data.role,null, roleEntitlementsListView]), "RoleEntitlementsEditView.render passed the correct arguments");
                            
                            roleEntitlementsEditView.render(editRoleView.data.args,editRoleView.data.role,null, roleEntitlementsListView, function() {
                                QUnit.ok($(".bootstrap-dialog-title").text().indexOf("Add an Entitlement") > -1, "Add entitlement dialog opened");
                                roleEntitlementsEditView.$el.find("input.assignment-name").val("myEntitlement");
                                roleEntitlementsEditView.data.JSONEditors[0].setValue("myAttrVal");
                                
                                roleEntitlementsEditView.saveAssignment(function() {
                                    roleEntitlementsEditView.bsDialog.close();
                                    QUnit.ok(_.has(editRoleView.data.role.assignments,"myEntitlement"), "New Entitlement successfully saved")
                                    deleteTest();
                                });
                            });
                        });
                        
                    }, editRoleSpy));
                }, editRoleView));
            });

            QUnit.asyncTest("Generic Edit Resource View", function () {
                var registrySpy = sinon.spy(resourceEditViewRegistry,"getEditViewModule"),
                    editResourceSpy = sinon.spy(genericEditResourceView,"render"),
                    childView,
                    args = ["managed","user"],
                    deleteTest = function() {
                        genericEditResourceView.deleteObject(null,function() {
                            QUnit.ok(true,"Successfully deleted user");
                            QUnit.start();
                        });
                        
                        $("body").one("shown.bs.modal", function () {
                            QUnit.ok($(".bootstrap-dialog-title").text().indexOf("Confirm") > -1, "Delete confirmation dialog opened");
                            $(".bootstrap-dialog-footer-buttons").find(".btn-primary").click();
                        });
                    };
                
                editResourceMock(server);
                
                //managed/role is an example of an edit view override that allows custom editing of a specific managed object
                editResourceView.render(args);
                
                childView = registrySpy.returnValues[0];
                
                QUnit.equal(childView.template, "templates/admin/resource/EditResourceViewTemplate.html", "The GenericEditResourceView loaded")
                resourceEditViewRegistry.getEditViewModule.restore();
                
                QUnit.ok(_.isEqual(editResourceSpy.args[0][0],args), "GenericEditResourceView gets the correct arguments from resourceEditViewRegistry");
                
                QUnit.ok(childView.data.newObject,"Based on the args passed to the GenericEditResourceView the 'newObject' variable was properly set to true");
                
                genericEditResourceView.render.restore();
                
                genericEditResourceView.render(args, _.bind(function() {
                    QUnit.ok(!editRoleView.$el.find("#linkedSystemsTabHeader").is(":visible"), "Linked Systems tab hidden");
                    this.editor.editors["root.userName"].setValue("newUser");
                    this.editor.editors["root.givenName"].setValue("New");
                    this.editor.editors["root.sn"].setValue("User");
                    this.editor.editors["root.mail"].setValue("newUser@newUserSite.com");
                    
                    conf.loggedUser = { roles: ["ui-admin"] };
                    this.save(null, function() {
                        QUnit.ok(_.isEqual(genericEditResourceView.data.args,editResourceSpy.args[0][0]), "New User successfully saved");
                        QUnit.equal(location.hash, "#resource/managed/user/edit/" +  _.last(genericEditResourceView.data.args), "URL route changed to the correct value after save");
                        
                        //duplicated server calls giving me the already edited object
                        //change the properties that we will test to the actual oldObject values
                        genericEditResourceView.oldObject.manager = "";
                        genericEditResourceView.oldObject.rev = "1";
                        genericEditResourceView.oldObject.roles = ["openidm-authorized"];
                        
                        originalObject = $.extend(true,{},genericEditResourceView.oldObject);
                        
                        genericEditResourceView.save(null, function() {
                            QUnit.ok(true,"Role and Manager values successfully edited and saved");
                            deleteTest();
                        });
                    });
                }, genericEditResourceView));
            });
        }
    };
});

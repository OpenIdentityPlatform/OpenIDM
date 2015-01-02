/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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
    "sinon",
    "org/forgerock/openidm/ui/admin/delegates/BrowserStorageDelegate",
    "org/forgerock/openidm/ui/admin/delegates/SyncDelegate",
    "org/forgerock/openidm/ui/admin/util/MappingUtils",
    "org/forgerock/openidm/ui/admin/mapping/AddPropertyMappingDialog",
    "org/forgerock/openidm/ui/admin/mapping/EditPropertyMappingDialog",
    "org/forgerock/openidm/ui/admin/mapping/MappingBaseView",
    "org/forgerock/openidm/ui/admin/mapping/PropertiesView",
    "../mocks/mapping/propertiesViewLoad"
], function (sinon, browserStorageDelegate, syncDelegate, mappingUtils, addPropertyMappingDialog, editPropertyMappingDialog, mappingBaseView, PropertiesView, propertiesViewLoadMock) {
    var mappingName = "systemLdapAccounts_managedUser";
    
    browserStorageDelegate.set(mappingName + "_Properties", "test");
    
    return {
        executeAll: function (server) {
            
            module('Property Mapping UI Functions');
            
            QUnit.asyncTest("PropertiesView Tests", function () {
                propertiesViewLoadMock(server);

                PropertiesView.render([mappingName], function () {
                    var browserStorageDelegate = require("org/forgerock/openidm/ui/admin/delegates/BrowserStorageDelegate"),
                        setNumRepresentativePropsLineSpy;

                    QUnit.equal($('#mappingContent').length, 1, "Page Successfully Loads");
                    
                    //currentMapping() test
                    QUnit.equal(PropertiesView.currentMapping().name, mappingName, "currentMapping() returns proper value");
                    
                    //clear changes button test
                    QUnit.equal($("div.changesPending:visible").length, 1,"Changes Pending message displayed");
                    $("#clearChanges").click();
                    QUnit.equal(browserStorageDelegate.get(mappingName + "_Properties"), null, "Clear Changes button clears changed mapping properties from session storage");
                    QUnit.equal($("div.changesPending:visible").length, 0,"Changes Pending message successfully hidden");
                    
                    //PropertiesGrid tests
                    QUnit.equal(PropertiesView.$el.find(".ui-jqgrid").length, 1, "Properties grid loaded");
                    QUnit.equal(PropertiesView.$el.find(".ui-jqgrid tr:gt(1)").length, PropertiesView.currentMapping().properties.length, "Correct number of properties displayed in grid");
                    //row click test
                    PropertiesView.$el.find(".ui-jqgrid tr#displayName").click();
                    QUnit.equal(window.location.hash,"#property/" + mappingName + "/displayName","Clicking grid row changes address to proper route");
                    
                    //numRepresentativeProperties tests
                    setNumRepresentativePropsLineSpy = sinon.spy(PropertiesView,"setNumRepresentativePropsLine");
                    $("#numRepresentativeProps").val(2).keyup();
                    QUnit.equal(browserStorageDelegate.get(mappingName + "_numRepresentativeProps",true), 2, "Number of representative properties successfully changed");
                    QUnit.equal(setNumRepresentativePropsLineSpy.called, true, "setNumRepresentativePropsLine successfully called after change to Number of representative properties");
                    browserStorageDelegate.set(mappingName + "_numRepresentativeProps", 4,true);
                    setNumRepresentativePropsLineSpy.restore();
                    
                    //remove property tests
                    PropertiesView.$el.find("[target=displayName]").click();
                    QUnit.equal($("#jqConfirm").length, 1, "Remove property confirmation successfully opened");
                    $(".ui-dialog").find('button:contains("Cancel")').click();
                    QUnit.equal($("#jqConfirm").length, 0, "Remove property confirmation successfully cancelled");
                    PropertiesView.$el.find("[target=displayName]").click();
                    $(".ui-dialog").find('button:contains("Ok")').click();
                    QUnit.notEqual(browserStorageDelegate.get(mappingName + "_Properties").length, PropertiesView.currentMapping().properties.length, "Property successfully removed from current mapping properties");
                    browserStorageDelegate.remove(mappingName + "_Properties");
                    
                    //add property button test
                    $(".addProperty").click();
                    QUnit.ok(_.contains(window.location.hash, "_new"), "Add Property button successfully changes address to appropriate route");
                    
                    QUnit.start();
                });
            });
            
            QUnit.asyncTest("AddPropertyMappingDialog Tests", function () {
                var browserStorageDelegate = require("org/forgerock/openidm/ui/admin/delegates/BrowserStorageDelegate");
                propertiesViewLoadMock(server);
                mappingBaseView.data = {};
                browserStorageDelegate.remove(mappingName + "_Properties");
               
               PropertiesView.render([mappingName], function () {
                    //addPropertyMappingDialog tests
                   addPropertyMappingDialog.render([mappingName],function(){
                       var propsBefore = browserStorageDelegate.get(mappingName + "_Properties");
                       QUnit.equal($("#propertyAddForm").length, 1, "Add Property Dialog successfully loaded");
                       addPropertyMappingDialog.$el.find("[name=propertyList]").val("test");
                       addPropertyMappingDialog.$el.find("input[type=submit]").click();
                       QUnit.notEqual(browserStorageDelegate.get(mappingName + "_Properties").length,propsBefore.length,"Property mapping successfully added");
                       QUnit.equal(window.location.hash, "#property/" + mappingName + "/test","After adding new property mapping the url changes to correct route")
                       $(".ui-dialog-titlebar-close").click();
                       QUnit.equal($("#propertyAddForm").length, 0, "Add Property Dialog successfully closed");
                       
                       QUnit.start();
                   });
                });
            });
            
            QUnit.asyncTest("EditPropertyMappingDialog Tests", function () {
                var browserStorageDelegate = require("org/forgerock/openidm/ui/admin/delegates/BrowserStorageDelegate");
                propertiesViewLoadMock(server);
                mappingBaseView.data = {};
                browserStorageDelegate.remove(mappingName + "_Properties");
                
                PropertiesView.render([mappingName], function () {
                    //editPropertyMappingDialog tests
                    editPropertyMappingDialog.render([mappingName, "displayName"], function(){
                            var propsBefore = browserStorageDelegate.get(mappingName + "_Properties"),
                                propEditForm = editPropertyMappingDialog.$el.find("#propertyEditForm");
                            QUnit.equal($("#propertyEditForm").length, 1, "Edit Property Dialog successfully loaded");
                            
                            propEditForm.find("[name=source]").val("test").trigger("change");
                            propEditForm.find("input[type=submit]").click();
                            QUnit.equal(editPropertyMappingDialog.$el.find("#Property_List").find(".validation-message").text(),"You must select a valid property name.","Invalid property message displayed correctly");
                            QUnit.ok(editPropertyMappingDialog.$el.find("#Property_List").find("input[type=submit]:disabled").length > 0, "Submit button properly disabled");
                            
                            propEditForm.find("[name=source]").val("sn").trigger("change");
                            QUnit.equal(editPropertyMappingDialog.data.property.source,"sn","Property mapping source successfully changed");
                            QUnit.equal(editPropertyMappingDialog.$el.find("#Property_List").find("input[type=submit]:disabled").length,0, "Submit button properly enabled");
                            propEditForm.find("input[type=submit]").click();
                            QUnit.notDeepEqual(browserStorageDelegate.get(mappingName + "_Properties"),propsBefore,"Property mapping successfully updated");
                            
                            $(".ui-dialog-titlebar-close").click();
                            QUnit.equal($("#propertyEditForm:visible").length, 0, "Edit Property Dialog successfully closed");
                            
                            QUnit.start();
                    });
                });
            });
        }
    };

}); 
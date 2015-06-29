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

/*global require, define, QUnit, window, $, module */

define([
    "sinon",
    "org/forgerock/openidm/ui/admin/delegates/SyncDelegate",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingUtils",
    "org/forgerock/openidm/ui/admin/mapping/properties/AddPropertyMappingDialog",
    "org/forgerock/openidm/ui/admin/mapping/properties/EditPropertyMappingDialog",
    "org/forgerock/openidm/ui/admin/util/LinkQualifierUtils",
    "org/forgerock/openidm/ui/admin/mapping/properties/RoleEntitlementsView",
    "org/forgerock/openidm/ui/admin/mapping/properties/AttributesGridView",
    "org/forgerock/openidm/ui/admin/mapping/MappingBaseView",
    "org/forgerock/openidm/ui/admin/mapping/PropertiesView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "../mocks/mapping/propertiesViewLoad"
], function (sinon,
             syncDelegate,
             mappingUtils,
             addPropertyMappingDialog,
             editPropertyMappingDialog,
             LinkQualifierUtils,
             RoleEntitlementsView,
             AttributesGridView,
             mappingBaseView,
             PropertiesView,
             conf,
             propertiesViewLoadMock) {
    var mappingName = "systemLdapAccounts_managedUser";


    return {
        executeAll: function (server) {

            module('Property Mapping UI Functions');

            QUnit.asyncTest("PropertiesView Tests", function () {
                $("#qunit-fixture").append("<div id=\"attributesGrid\"></div>");

                propertiesViewLoadMock(server);
                var syncDetails = {
                    "sync": {"mappings":[{"target":"managed/user","properties":[{"target":"displayName","source":"cn"},{"target":"description","source":"description"},{"target":"givenName","source":"givenName"},{"target":"mail","source":"mail"},{"target":"telephoneNumber","source":"telephoneNumber"},{"target":"sn","source":"sn"},{"target":"userName","source":"uid"}],"source":"system/ldap/account","policies":[{"action":"UPDATE","situation":"CONFIRMED"},{"action":"UPDATE","situation":"FOUND"},{"action":"CREATE","situation":"ABSENT"},{"action":"EXCEPTION","situation":"AMBIGUOUS"},{"action":"CREATE","situation":"MISSING"},{"action":"DELETE","situation":"SOURCE_MISSING"},{"action":"IGNORE","situation":"UNQUALIFIED"},{"action":"IGNORE","situation":"UNASSIGNED"}],"name":"systemLdapAccounts_managedUser"},{"target":"system/ldap/account","links":"systemLdapAccounts_managedUser","properties":[{"target":"givenName","source":"givenName"},{"target":"sn","source":"sn"},{"target":"cn","source":"","transform":{"type":"text/javascript","source":"source.displayName || (source.givenName + ' ' + source.sn);"}},{"target":"uid","source":"userName"},{"target":"description","source":"description","condition":{"type":"text/javascript","source":"!!object.description"}},{"target":"mail","source":"mail"},{"target":"userPassword","transform":{"type":"text/javascript","source":"openidm.decrypt(source);"},"source":"password","condition":{"type":"text/javascript","source":"object.password != null"}},{"target":"telephoneNumber","source":"telephoneNumber","condition":{"type":"text/javascript","source":"!!object.telephoneNumber"}}],"source":"managed/user","onCreate":{"type":"text/javascript","source":"target.dn = 'uid=' + source.userName + ',ou=People,dc=example,dc=com';"},"policies":[{"action":"UPDATE","situation":"CONFIRMED"},{"action":"LINK","situation":"FOUND"},{"action":"CREATE","situation":"ABSENT"},{"action":"IGNORE","situation":"AMBIGUOUS"},{"action":"IGNORE","situation":"MISSING"},{"action":"DELETE","situation":"SOURCE_MISSING"},{"action":"IGNORE","situation":"UNQUALIFIED"},{"action":"IGNORE","situation":"UNASSIGNED"}],"name":"managedUser_systemLdapAccounts"},{"target":"managed/user","properties":[],"source":"system/ldap/__ALL__","policies":[{"action":"ASYNC","situation":"ABSENT"},{"action":"ASYNC","situation":"ALL_GONE"},{"action":"ASYNC","situation":"AMBIGUOUS"},{"action":"ASYNC","situation":"CONFIRMED"},{"action":"ASYNC","situation":"FOUND"},{"action":"ASYNC","situation":"FOUND_ALREADY_LINKED"},{"action":"ASYNC","situation":"LINK_ONLY"},{"action":"ASYNC","situation":"MISSING"},{"action":"ASYNC","situation":"SOURCE_IGNORED"},{"action":"ASYNC","situation":"SOURCE_MISSING"},{"action":"ASYNC","situation":"TARGET_IGNORED"},{"action":"ASYNC","situation":"UNASSIGNED"},{"action":"ASYNC","situation":"UNQUALIFIED"}],"name":"sourceLdap__ALL___managedUser"}]},
                    "mappingName": "systemXmlfileAccounts_managedUser"
                };

                    AttributesGridView.setCurrentMapping(syncDetails.sync.mappings[0]);
                    AttributesGridView.setSyncConfig(syncDetails.sync);
                    LinkQualifierUtils.setLinkQualifier(["default", "test"], syncDetails.sync.mappings[0].name);

                    conf.globalData.sampleSource = {"givenName":"Aaccf","employeeType":"employee","cn":"Aaccf Amar","dn":"uid=user.0,ou=People,dc=example,dc=com","telephoneNumber":"+1 685 622 6202","ldapGroups":["cn=Manager,ou=Groups,dc=example,dc=com"],"uid":"user.0","mail":"user.0@maildomain.net","sn":"Amar","description":"This is the description for Aaccf Amar.","_id":"uid=user.0,ou=People,dc=example,dc=com"};

                    AttributesGridView.render([mappingName], function () {
                        var setNumRepresentativePropsLineSpy;

                        QUnit.equal($('#attributesGrid').length, 1, "Page Successfully Loads");

                        //getCurrentMapping() test
                        QUnit.equal(AttributesGridView.getCurrentMapping().name, mappingName, "getCurrentMapping() returns proper value");
                        QUnit.equal(AttributesGridView.$el.find("div.changesPending:visible").length, 0,"Changes Pending message not shown");

                        //PropertiesGrid tests
                        QUnit.equal(AttributesGridView.$el.find(".ui-jqgrid").length, 1, "Properties grid loaded");
                        QUnit.equal(AttributesGridView.$el.find(".ui-jqgrid tr:gt(1)").length, AttributesGridView.getCurrentMapping().properties.length, "Correct number of properties displayed in grid");

                        QUnit.equal(AttributesGridView.$el.find(".ui-jqgrid tr.jqgrow:eq(1)").find("td:eq(2) .text-muted").length, 1, "Sample successfully displayed");

                        QUnit.equal(AttributesGridView.$el.find(".ui-jqgrid tr.jqgrow:eq(1)").find("td:eq(2) .text-muted").html(), "(This is the description for Aaccf Amar.)", "Default value successfully displayed");

                        //row click test
                        AttributesGridView.$el.find(".ui-jqgrid tr.jqgrow:first").click();

                        QUnit.equal(window.location.hash,"#properties/systemLdapAccounts_managedUser/1", "Clicking grid row changes address to proper route");

                        //numRepresentativeProperties tests
                        setNumRepresentativePropsLineSpy = sinon.spy(AttributesGridView, "setNumRepresentativePropsLine");
                        $("#numRepresentativeProps").val(2).keyup();

                        QUnit.equal(AttributesGridView.getNumRepresentativeProps(), 2, "Number of representative properties successfully changed");
                        QUnit.equal(setNumRepresentativePropsLineSpy.called, true, "setNumRepresentativePropsLine successfully called after change to Number of representative properties");
                        AttributesGridView.setNumRepresentativeProps(4);

                        setNumRepresentativePropsLineSpy.restore();
                        //remove property tests
                        $("body").one("shown.bs.modal", function (e) {

                            $(e.target).one("hidden.bs.modal", function () {
                                //clear changes button test
                                QUnit.equal(AttributesGridView.$el.find("div.changesPending:visible").length, 1,"Changes Pending message displayed");

                                QUnit.notEqual(AttributesGridView.model.mappingProperties.length, AttributesGridView.getCurrentMapping().properties.length, "Property successfully removed from current mapping properties");

                                $("#clearChanges").click();
                                QUnit.equal(AttributesGridView.model.mappingProperties, null, "Clear Changes button clears changed mapping properties from session storage");
                                QUnit.equal(AttributesGridView.$el.find("div.changesPending:visible").length, 0,"Changes Pending message successfully hidden");
                                $(".bootstrap-dialog").unbind("hidden.bs.modal");
                                QUnit.start();
                            });

                            // confirm the removal of the displayName property
                            $(".btn-primary", e.target).click();
                        });
                        AttributesGridView.$el.find(".removePropertyBtn[target=displayName]").click();

                    });
            });


            QUnit.asyncTest("AddPropertyMappingDialog Tests", function () {
                $("#qunit-fixture").append("<div id=\"attributesGrid\"></div>");

                propertiesViewLoadMock(server);
                mappingBaseView.data = {};
                AttributesGridView.model.mappingProperties = null;
                delete PropertiesView.route;

                PropertiesView.render([mappingName], function () {
                    //addPropertyMappingDialog tests
                    addPropertyMappingDialog.render([mappingName],function() {
                        QUnit.equal($("#propertyMappingDialogForm").length, 1, "Add Property Dialog successfully loaded");

                        addPropertyMappingDialog.$el.find("[name=propertyList]").val("test");
                        $(".bootstrap-dialog .btn-primary").click();

                        _.delay(function(){
                            QUnit.equal(_.last(AttributesGridView.model.mappingProperties).target,"test","Property mapping successfully added");

                            QUnit.equal(window.location.hash, "#properties/" + mappingName + "/9","After adding new property mapping the url changes to correct route");

                            QUnit.equal($("#propertyAddForm").length, 0, "Add Property Dialog successfully closed");
                            QUnit.start();
                        }, 400);
                    });
                });
            });

            /*
            This entire test needs to be looked at. I believe the data it is looking for changed. Along with that and the bootstrap dialog change
            it should probably just be rewritten.


            QUnit.asyncTest("EditPropertyMappingDialog Tests", function () {
                var browserStorageDelegate = require("org/forgerock/openidm/ui/admin/delegates/BrowserStorageDelegate");
                propertiesViewLoadMock(server);

                mappingBaseView.data = {};

                browserStorageDelegate.remove(mappingName + "_Properties");

                delete PropertiesView.route;

                PropertiesView.render([mappingName], function () {
                    //editPropertyMappingDialog tests

                    editPropertyMappingDialog.render([mappingName, 1], function(){
                        var propsBefore = browserStorageDelegate.get(mappingName + "_Properties"),
                            propEditForm = editPropertyMappingDialog.$el;

                        QUnit.equal($("#propertyMappingDialogForm").length, 1, "Edit Property Dialog successfully loaded");

                        //propEditForm.find("[name=source]").val("test").trigger("change");

                        //QUnit.equal(editPropertyMappingDialog.$el.find("#Property_List").find(".validation-message").text(),"You must select a valid property name.","Invalid property message displayed correctly");
                        //QUnit.ok($(".bootstrap-dialog .btn-primary:disabled").length > 0, "Submit button properly disabled");

                        //propEditForm.find("[name=source]").val("sn").trigger("change");
                        //QUnit.equal(editPropertyMappingDialog.data.property.source,"sn","Property mapping source successfully changed");
                        //QUnit.equal(editPropertyMappingDialog.$el.find("#Property_List").find("input[type=submit]:disabled").length,0, "Submit button properly enabled");

                        //$(".bootstrap-dialog .btn-default").click();

                        //QUnit.notDeepEqual(browserStorageDelegate.get(mappingName + "_Properties"),propsBefore,"Property mapping successfully updated");

                        //QUnit.equal($("#propertyEditForm:visible").length, 0, "Edit Property Dialog successfully closed");

                        QUnit.start();
                    });
                });
            });
            */

        }
    };

});

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
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/admin/mapping/AddMappingView",
    "org/forgerock/openidm/ui/admin/MapResourceView",
    "../mocks/addMapping"
], function (constants,
             router,
             eventManager,
             addMappingView,
             MapResourceView,
             addMapping) {

    return {
        executeAll: function (server) {

            module('Add Mapping UI Functions');

            QUnit.asyncTest("Add Mapping View", function () {
                /*
                 * Mocks include:
                 * config/sync (mappings: systemLdapAccounts_managedUser and managedUser_systemLdapAccounts)
                 * config/managed (objects: role, apple, user, test)
                 * system?_action=test (resources: ldap - account, group;)
                 */
                addMapping(server);
                addMappingView.render([], function () {
                    QUnit.equal(addMappingView.$el.find(".help-link").length, 1, "Help successfully detected");

                    QUnit.equal(addMappingView.$el.find(".back-bar a").length, 1, "Return link loaded");

                    QUnit.equal(addMappingView.$el.find("#resourceConnectorContainer .resource-body").length, 1, "Resources successfully loaded");

                    QUnit.equal(addMappingView.$el.find("#resourceManagedContainer .resource-body").length, 4, "Managed Objects successfully loaded");

                    addMappingView.$el.find("#resourceConnectorContainer .resource-body:first .add-resource-button").trigger("click");
                    addMappingView.$el.find("#resourceManagedContainer .resource-body:first .add-resource-button").trigger("click");

                    QUnit.equal(addMappingView.$el.find("#mappingSource .mapping-resource:visible .resource-given-name").text(), 'ldap', "Source system successfully selected");
                    QUnit.equal(addMappingView.$el.find("#mappingTarget .mapping-resource:visible .resource-given-name").text(), 'apple', "Target system successfully selected");

                    addMappingView.$el.find(".mapping-swap").trigger("click");

                    QUnit.equal(addMappingView.$el.find("#mappingTarget .mapping-resource:visible .resource-given-name").text(), 'ldap', "Swap successful");

                    $("body").one("shown.bs.modal", function () {

                        QUnit.equal($(".bootstrap-dialog:visible .bootstrap-dialog-title").text(), "Create mapping.", "Create mapping dialog successfully shown");

                        QUnit.equal($(".bootstrap-dialog .mappingName").val(), "managedApple_sourceLdapAccount", "Mapping name auto generated successfully");

                        $(".bootstrap-dialog").one("hidden.bs.modal", function () {
                            QUnit.equal($(".bootstrap-dialog").length, 0, "Create mapping dialog closed");

                            MapResourceView.$el.find("#mappingSource .select-resource").trigger("click");
                            MapResourceView.$el.find("#mappingTarget .select-resource").trigger("click");

                            QUnit.equal(addMappingView.$el.find("#mappingSource .mapping-resource-empty:visible").length, 1, "Removal of source successful");
                            QUnit.equal(addMappingView.$el.find("#mappingTarget .mapping-resource-empty:visible").length, 1, "Removal of target successful");

                            QUnit.start();

                        });

                        $(".bootstrap-dialog #mappingSaveCancel").click();

                    });

                    MapResourceView.$el.find("#createMapping").trigger("click");

                });

            });
        }
    };
});

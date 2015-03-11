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
    "org/forgerock/openidm/ui/admin/ResourcesView",
    "../mocks/adminInit",
    "../mocks/addMapping"
], function (constants, router, eventManager, addMappingView, resourcesView, adminInit, addMapping) {

    return {
        executeAll: function (server) {

            adminInit(server);

            module('Add Mapping UI Functions');

            QUnit.asyncTest("Add Mapping View", function () {

                addMapping(server);

                addMappingView.render([], function () {
                    QUnit.equal(addMappingView.$el.find(".help").length, 1, "Help successfully detected");

                    QUnit.equal(addMappingView.$el.find("#resourceConnectorContainer .resource-body").length, 1, "Resources successfully loaded");

                    QUnit.equal(addMappingView.$el.find("#resourceManagedContainer .resource-body").length, 4, "Managed Objects successfully loaded");

                    addMappingView.$el.find("#resourceConnectorContainer .resource-body:first .add-resource-button").trigger("click");
                    addMappingView.$el.find("#resourceManagedContainer .resource-body:first .add-resource-button").trigger("click");

                    setTimeout(function(){
                        QUnit.equal(addMappingView.$el.find("#mappingSource .mapping-resource:visible").length, 1, "Add mapping source successful");
                        QUnit.equal(addMappingView.$el.find("#mappingTarget .mapping-resource:visible").length, 1, "Add target source successful");

                        QUnit.equal(addMappingView.$el.find("#mappingTarget .mapping-resource:visible").length, 1, "Add target source successful");

                        addMappingView.$el.find(".mapping-swap").trigger("click");

                        setTimeout(function(){
                            QUnit.equal(addMappingView.$el.find("#mappingTarget .mapping-resource select").length, 1, "Swap successful");

                            addMappingView.$el.find(".add-mapping").trigger("click");

                            setTimeout(function(){
                                QUnit.equal($(".bootstrap-dialog").length, 1, "Create mapping dialog successfully shown");

                                QUnit.ok($(".bootstrap-dialog .mappingName").length > 0, "Mapping name auto generated successfully");

                                $(".bootstrap-dialog .btn-default").click();

                                _.delay(function(){
                                    QUnit.equal($(".bootstrap-dialog").length, 0, "Create mapping dialog closed");

                                    addMappingView.$el.find("#mappingSource .select-resource").trigger("click");
                                    addMappingView.$el.find("#mappingTarget .select-resource").trigger("click");

                                    QUnit.equal(addMappingView.$el.find("#mappingSource .mapping-resource-empty:visible").length, 1, "Removal of source successful");
                                    QUnit.equal(addMappingView.$el.find("#mappingTarget .mapping-resource-empty:visible").length, 1, "Removal of target successful");

                                    QUnit.start();

                                }, 800);

                            }, 400);

                        }, 100);

                    }, 100);
                });

            });
        }
    };
});
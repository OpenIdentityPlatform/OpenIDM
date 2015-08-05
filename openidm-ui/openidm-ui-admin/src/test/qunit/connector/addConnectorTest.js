/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
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
    "org/forgerock/openidm/ui/admin/connector/AddConnectorView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "../mocks/connectorAddEdit",
], function (constants, router, eventManager, addConnector, ValidatorsManager, connectorMock) {

    return {
        executeAll: function (server) {
            module('Admin Add Connector UI Functions');

            QUnit.asyncTest("Add Connector", function () {
                var stubConnectorChange;

                connectorMock(server);

                addConnector.render([], function () {
                    //Base connector tests
                    QUnit.equal(addConnector.$el.find(".back-bar a").length, 1, "Return link loaded");

                    QUnit.equal(addConnector.$el.find("#connectorName").length, 1, "Connector base loaded");

                    QUnit.equal(addConnector.$el.find("#csvFilePath").length, 1, "Connector details loaded");

                    QUnit.equal(addConnector.$el.find("#connectorType option").length, 4, "Connector select populated");

                    QUnit.ok(addConnector.$el.find("#submitConnector").is(':disabled'), "Connector submit button disabled at start");

                    addConnector.$el.find("#connectorName").val("test");
                    addConnector.$el.find("#connectorName").change();
                    addConnector.$el.find("#csvFilePath").val("test");
                    addConnector.$el.find("#csvFilePath").change();
                    addConnector.$el.find("#csvUniqueAttribute").val("csvUniqueAttribute");
                    addConnector.$el.find("#csvUniqueAttribute").change();
                    addConnector.$el.find("#csvNameAttribute").val("csvUniqueAttribute");
                    addConnector.$el.find("#csvNameAttribute").change();

                    QUnit.ok(addConnector.$el.find("#submitConnector").not(':disabled'), "Connector submit button enabled after changes");

                    addConnector.$el.find("#connectorType").val("org.forgerock.openicf.connectors.xml.XMLConnector_1.1.0.2");
                    addConnector.$el.find("#connectorType").change();

                    stubConnectorChange = sinon.stub(ValidatorsManager, "validateAllFields", function(el){
                        stubConnectorChange.restore();

                        ValidatorsManager.validateAllFields(el);
                        _.delay(function () {
                            QUnit.ok(addConnector.$el.find("#submitConnector").is(':disabled'), "Connector submit button disabled after type change");
                            QUnit.start();
                        }, 10);
                    });
                });
            });
        }

    };

});

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
    "org/forgerock/openidm/ui/admin/connector/AddEditConnectorView",
    "../mocks/connectorAddEdit",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate"
], function (constants, router, eventManager, addEditConnector, connectorMock, ConfigDelegate, ConnectorDelegate) {

    return {
        executeAll: function (server) {
            module('Admin Add Connector UI Functions');

            QUnit.asyncTest("Add Connector", function () {
                var savePromise = $.Deferred(),
                    stubbedSave,
                    stubbedValidate,
                    saveCheck = false;

                connectorMock(server);

                addEditConnector.render([], function () {

                    QUnit.equal(addEditConnector.$el.find(".title-bar a").length, 1, "Help successfully detected");

                    QUnit.equal(addEditConnector.$el.find("#connectorName").length, 1, "General connector details successfully loaded");

                    QUnit.ok(addEditConnector.$el.find("#connectorEnabled").val() === "true", "Connector defaults to enabled");

                    QUnit.ok(addEditConnector.$el.find("#connectorType optgroup").length > 0, "Connector types successfully loaded");

                    QUnit.equal(addEditConnector.$el.find(".form-button-bar .button").length, 4, "Correct number of buttons loaded");

                    QUnit.ok(addEditConnector.$el.find("#validateConnector").is(":disabled") === true, "Validation is disabled");

                    addEditConnector.$el.find("#connectorName").val("test");
                    addEditConnector.$el.find("#connectorName").trigger("keyup");

                    addEditConnector.$el.find("#filePath").val("test");
                    addEditConnector.$el.find("#filePath").trigger("keyup");

                    addEditConnector.$el.find("#uniqueAttribute").val("test");
                    addEditConnector.$el.find("#uniqueAttribute").trigger("keyup");

                    addEditConnector.$el.find("#nameAttribute").val("test");
                    addEditConnector.$el.find("#nameAttribute").trigger("keyup");

                    addEditConnector.$el.find("#passwordAttribute").val("test");
                    addEditConnector.$el.find("#passwordAttribute").trigger("keyup");

                    QUnit.ok(addEditConnector.$el.find("#validateConnector").is(":disabled") === false, "Validation is enabled");

                    stubbedValidate = sinon.stub(ConnectorDelegate, "testConnector", function(details){
                        var validatePromise = $.Deferred();

                        validatePromise.resolve(
                            {
                                "configurationProperties":{

                                },
                                "_id":"provisioner.openicf/ldap",
                                "enabled":true,
                                "objectTypes" : {}
                            }
                        );

                        return validatePromise;
                    });

                    addEditConnector.$el.find("#validateConnector").trigger("click");

                    QUnit.ok(addEditConnector.$el.find("#addEditConnector").is(":disabled") === false, "Validation of connector successful");

                    stubbedSave = sinon.stub(ConfigDelegate, "createEntity", function(details){
                        savePromise.resolve();

                        saveCheck = true;

                        return savePromise;
                    });

                    addEditConnector.$el.find("#addEditConnector").trigger("click");

                    QUnit.ok(saveCheck, "Save successful");

                    stubbedValidate.restore();
                    stubbedSave.restore();
                    QUnit.start();
                });
            });
        }

    };

});

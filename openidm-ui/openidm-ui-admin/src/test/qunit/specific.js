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
 * Copyright 2014-2015 ForgeRock AS.
 */

/*global require, define, QUnit, $ */

define([
    "org/forgerock/commons/ui/common/main/Configuration",
    "./managedobjects/managedObjectsTest",
    "./resources/resourceTest",
    "./mapping/addMappingTest",
    "./mapping/reconTests",
    "./mapping/propertyMappingTest",
    "./mapping/correlationTest",
    "./mapping/linkQualifierTest",
    "./connector/editConnectorTest",
    "./connector/addConnectorTest",
    "./resourceData/editResourceViewTest"
], function (Configuration, moTest, resourceTest, addMappingTest, reconTests, propertyMappingTest, correlationTest, linkQualifierTest, editConnectorTest, addConnectorTest, editResourceViewTest) {

    return {
        executeAll: function (server) {

            QUnit.testStart(function (testDetails) {
                var lqu = require("org/forgerock/openidm/ui/admin/util/LinkQualifierUtils"),
                    connectorDelegate = require("org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate");
                connectorDelegate.deleteCurrentConnectorsCache();
                lqu.model.linkQualifier = [];
                Configuration.loggedUser = { "userName": "openidm-admin", "roles": ["ui-admin"] }
            });

            QUnit.moduleDone(function() {
                $(".bootstrap-dialog").remove();
            });

            addMappingTest.executeAll(server);
            resourceTest.executeAll(server);
            reconTests.executeAll(server);
            correlationTest.executeAll(server);
            linkQualifierTest.executeAll(server);
            moTest.executeAll(server);
            propertyMappingTest.executeAll(server);
            editConnectorTest.executeAll(server);
            addConnectorTest.executeAll(server);
            editResourceViewTest.executeAll(server);
        }
    };
});

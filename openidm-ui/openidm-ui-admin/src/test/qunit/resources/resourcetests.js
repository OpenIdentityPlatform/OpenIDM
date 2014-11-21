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
    "../../../../target/test/libs/sinon-1.10.3.js",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/admin/managed/AddEditManagedView",
    "org/forgerock/openidm/ui/admin/ResourcesView",
    "../mocks/adminInit",
    "../mocks/resourceDetails"
], function (sinon, constants, router, eventManager, addEditManagedView, resourcesView, adminInit, resourceDetails) {

    return {
        executeAll: function (server) {

            adminInit(server);

            module('Admin Resource UI Functions');

            QUnit.asyncTest("Resource View", function () {

                resourceDetails(server);

                resourcesView.render([], function () {
                    var viewManager = require("org/forgerock/commons/ui/common/main/ViewManager");

                    QUnit.equal(resourcesView.$el.find("#resourceConnectorContainer .resource-body").length, 2, "Connectors and add Connector successfully added");

                    QUnit.equal(resourcesView.$el.find("#resourceManagedContainer .resource-body").length, 5, "Managed Objects and add Managed Object successfully added");

                    QUnit.start();
                });
            });
        }
    };
});
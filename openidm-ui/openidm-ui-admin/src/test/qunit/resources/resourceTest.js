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
    "org/forgerock/openidm/ui/admin/connector/ConnectorListView",
    "../mocks/resourceDetails"
], function (constants, router, eventManager, ConnectorListView, resourceDetails) {

    return {
        executeAll: function (server, callback) {

            module('Admin Resource UI Functions');

            QUnit.asyncTest("Connector List View", function () {

                resourceDetails(server);

                ConnectorListView.render([], function () {

                    QUnit.equal(ConnectorListView.$el.find("#resourceConnectorContainer .card-container").length, 1, "Connectors and add Connector successfully added");

                    QUnit.equal(ConnectorListView.$el.find("#resourceManagedContainer .card-container").length, 4, "Managed Objects and add Managed Object successfully added");

                    QUnit.equal(ConnectorListView.$el.find(".btn-toolbar .btn-group").length, 3, "Button bar with correct number of actions found");

                    QUnit.equal(ConnectorListView.$el.find(".subtitle-bar a").length, 2, "Help successfully detected");

                    ConnectorListView.$el.find("#resourceConnectorContainer .card-container:first").find(".dropdown-toggle").trigger("click");

                    QUnit.equal($(".btn-group.open").length, 1, "Drop down successfully open");

                    QUnit.start();
                });
            });
        }
    };
});

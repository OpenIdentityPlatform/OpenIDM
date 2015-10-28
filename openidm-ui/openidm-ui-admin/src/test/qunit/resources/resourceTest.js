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
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/admin/connector/ConnectorListView",
    "org/forgerock/openidm/ui/admin/managed/ManagedListView",
    "../mocks/resourceDetails"
], function (constants, router, eventManager, ConnectorListView, ManagedListView, resourceDetails) {

    return {
        executeAll: function (server, callback) {

            module('Admin Resource UI Functions');

            QUnit.asyncTest("Connector List View", function () {

                resourceDetails(server);

                ConnectorListView.render([], function () {

                    QUnit.equal(ConnectorListView.$el.find("#resourceConnectorContainer .card-container").length, 1, "Connectors and add Connector successfully added");

                    ConnectorListView.$el.find("#resourceConnectorContainer .card-container:first").find(".dropdown-toggle").trigger("click");

                    QUnit.equal(ConnectorListView.$el.find(".subtitle-bar a").length, 1, "Help successfully detected");

                    QUnit.equal($(".btn-group.open").length, 1, "Drop down successfully open");

                    QUnit.start();
                });
            });

            QUnit.asyncTest("Managed Objects List View", function () {

                resourceDetails(server);

                ManagedListView.render([], function () {

                    QUnit.equal(ManagedListView.$el.find("#resourceManagedContainer .card-container").length, 4, "Managed Objects and add Managed Object successfully added");

                    QUnit.equal(ManagedListView.$el.find(".btn-toolbar .btn-group").length, 1, "Button bar with correct number of actions found");

                    QUnit.equal(ManagedListView.$el.find(".subtitle-bar a").length, 1, "Help successfully detected");

                    QUnit.start();
                });
            });


        }
    };
});

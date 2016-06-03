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
 * Copyright 2015 ForgeRock AS.
 */

/*global require, define, QUnit, $ */

define([
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/admin/connector/EditConnectorView",
    "../mocks/connectorAddEdit",
    "org/forgerock/openidm/ui/admin/objectTypes/ObjectTypesDialog"
], function (constants, router, eventManager, editConnector, connectorMock, ObjectTypesDialog) {

    return {
        executeAll: function (server) {
            module('Admin Edit Connector UI Functions');

            QUnit.asyncTest("Edit Connector", function () {
                connectorMock(server);

                editConnector.render(["provisioner.openicf_ldap"], function () {
                    //Base connector tests
                    QUnit.equal(editConnector.$el.find(".back-bar a").length, 1, "Return link loaded");

                    QUnit.ok(editConnector.$el.find(".page-header .page-type").html() === "LDAP Connector - 1.4.0.0", "Connector type set");

                    QUnit.ok(editConnector.$el.find(".page-header h1").html() === "ldap", "Connector name set");

                    QUnit.equal(editConnector.$el.find(".nav-tabs li").length, 4, "Tabs detected and tabdrop prepared");

                    QUnit.ok(editConnector.$el.find("#connectorEnabled").val() === "true", "Connector defaults to enabled");

                    editConnector.$el.find("input[name='configurationProperties.host']").val("1234");
                    editConnector.$el.find("input[name='configurationProperties.host']").change();

                    QUnit.equal(editConnector.$el.find(".message .connector-pending").length, 1, "Connector change message displayed");

                    //Object type tests
                    QUnit.equal(editConnector.$el.find("#selectObjectConfig").length, 1, "Prdefined object type dropdown displayed");

                    QUnit.equal(editConnector.$el.find("#objectTypesTab table tbody tr").length, 2, "Object type table populated");

                    //Sync tests
                    QUnit.equal(editConnector.$el.find("#syncTab .sources option").length, 2, "Sync schedule sources loaded");

                    editConnector.$el.find("#syncTab .addLiveSync").click();

                    QUnit.equal(editConnector.$el.find("#syncTab .liveSyncScheduleContainer").length, 1, "Sync schedule added");

                    QUnit.equal(editConnector.$el.find("#syncTab #retryOptions option").length, 3, "LiveSync options loaded");

                    editConnector.$el.find("#syncTab #retryOptions").val("0");
                    editConnector.$el.find("#syncTab #retryOptions").change();

                    QUnit.ok(editConnector.$el.find("#syncTab .postActionBlock").not(":visible"), "Retry options changed");

                    QUnit.equal(editConnector.$el.find(".message .sync-pending").length, 1, "Sync change message displayed");

                    //More Object type tests
                    editConnector.$el.find("#objectTypesTab table tbody tr").first().find(".delete-objectType").click();

                    QUnit.equal(editConnector.$el.find("#objectTypesTab table tbody tr").length, 1, "Delete successful");

                    QUnit.equal(editConnector.$el.find(".message .objecttype-pending").length, 1, "Object type change message displayed");

                    QUnit.start();
                });
            });
        }

    };

});

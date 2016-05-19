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
 * Copyright 2016 ForgeRock AS.
 */

define([
    "org/forgerock/openidm/ui/admin/mapping/MappingListView"
], function (MappingListView) {
    QUnit.module('Mapping List Tests');

    QUnit.test("Mapping missing check", function () {
        var testResource = {};

        testResource = MappingListView.setCardState(testResource, "managed", "managed/user", {objects: []});

        QUnit.equal(testResource.isMissing, true, "Detected missing managed object from mapping list");

        testResource = {
            config: "config/provisioner.openicf/ldap",
            displayName: "LDAP Connector",
            connectorRef: {
                connectorName : "org.identityconnectors.ldap.LdapConnector"
            }
        };

        testResource = MappingListView.setCardState(testResource, "ldap", "system/ldap/account", {objects: []});

        QUnit.equal(testResource.isMissing, undefined, "Connector properly detected and not set to missing state");

        testResource = {};

        testResource = MappingListView.setCardState(testResource, "ldap", "system/ldap/account", {objects: []});

        QUnit.equal(testResource.isMissing, true, "Connector missing and set to missing state");
    });
});

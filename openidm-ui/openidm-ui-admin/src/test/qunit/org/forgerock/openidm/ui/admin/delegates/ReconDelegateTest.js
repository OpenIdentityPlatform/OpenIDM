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
 * Portions Copyright 2026 3A Systems, LLC.
 */

define([
    "jquery",
    "sinon",
    "org/forgerock/openidm/ui/admin/delegates/ReconDelegate"
], function ($, sinon, ReconDelegate) {
    QUnit.module('ReconDelegate Tests');

    function capturedQueryFilter(call) {
        var stub = sinon.stub(ReconDelegate, "serviceCall", function () {
                return $.Deferred().promise();
            }),
            url;

        try {
            call();
            url = stub.firstCall.args[0].url;
        } finally {
            stub.restore();
        }
        return decodeURIComponent(url.substring("?_queryFilter=".length));
    }

    QUnit.test("getLastAuditForObjectId escapes the recon id and the object id", function (assert) {
        var filter = capturedQueryFilter(function () {
            ReconDelegate.getLastAuditForObjectId('r"1', "sourceObjectId", "system/ldap/account/a\\%41");
        });

        assert.equal(filter, 'reconId eq "r\\"1" and sourceObjectId eq "system/ldap/account/a\\\\%41"');
    });

    QUnit.test("getNewLinksFromRecon escapes the recon id and the end date", function (assert) {
        var filter = capturedQueryFilter(function () {
            ReconDelegate.getNewLinksFromRecon('r"1', "2026\\%41");
        });

        assert.equal(filter, 'reconId eq "r\\"1" and !(entryType eq "summary") and timestamp gt "2026\\\\%41"');
    });
});

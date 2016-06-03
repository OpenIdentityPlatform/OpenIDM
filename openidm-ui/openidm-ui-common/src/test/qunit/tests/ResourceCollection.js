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
/*globals QUnit */
define([
    "lodash",
    "sinon",
    "org/forgerock/openidm/ui/common/resource/ResourceCollection"
], function (_, sinon, ResourceCollection) {
    QUnit.module('ResourceCollection Functions');

    QUnit.test("initialize properly", function () {
        var rc = new ResourceCollection([], {
            url: "/testEndpoint",
            _queryFilter: 'true',
            isSystemResource: true
        });
        QUnit.equal(rc.queryParams.sortKey, "_sortKeys", "queryParams uses _sortKeys");
        QUnit.equal(rc.queryParams.pageSize, "_pageSize", "queryParams uses _pageSize");
        QUnit.equal(rc.queryParams._totalPagedResultsPolicy, "ESTIMATE", "queryParams uses _totalPagedResultsPolicy=ESTIMATE");
    });

});

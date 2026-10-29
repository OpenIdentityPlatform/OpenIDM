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
    "underscore",
    "sinon",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function ($, _, sinon, ResourceDelegate, ConfigDelegate) {
    QUnit.module('ResourceDelegate Tests');

    QUnit.test("normalizeRelationshipTypes reads the type arrays of Nullable relationships", function (assert) {
        var properties = ResourceDelegate.normalizeRelationshipTypes({
            manager: { type: ["relationship", "null"] },
            reports: { type: ["array", "null"], items: { type: "relationship" } },
            roles: { type: "array", items: { type: ["null", "relationship"] } },
            teams: { type: ["array", "null"], items: { type: ["relationship", "null"] } },
            authzRoles: { type: "array", items: { type: "relationship" } },
            description: { type: ["string", "null"] },
            aliases: { type: ["array", "null"], items: { type: "string" } },
            tags: { type: ["array", "null"] },
            union: { type: ["relationship", "string"] }
        });

        assert.strictEqual(properties.manager.type, "relationship");
        assert.strictEqual(properties.reports.type, "array");
        assert.strictEqual(properties.roles.items.type, "relationship");
        assert.strictEqual(properties.teams.type, "array", "a Nullable collection of Nullable relationships");
        assert.strictEqual(properties.teams.items.type, "relationship");
        assert.deepEqual(properties.authzRoles, { type: "array", items: { type: "relationship" } });
        assert.deepEqual(properties.description.type, ["string", "null"], "the type array of a property is kept");
        assert.deepEqual(properties.aliases.type, ["array", "null"], "and of an array of other items");
        assert.deepEqual(properties.tags, { type: ["array", "null"] }, "and of an array without items");
        assert.deepEqual(properties.union.type, ["relationship", "string"], "and of a union");
    });

    QUnit.test("getSchema reads the type arrays of Nullable relationships in all managed object schemas", function (assert) {
        var done = assert.async(),
            readEntity = sinon.stub(ConfigDelegate, "readEntity", function () {
                return $.Deferred().resolve({
                    objects: [
                        { name: "user", schema: { properties: { manager: { type: ["relationship", "null"] } } } },
                        { name: "role", schema: { properties: {
                            members: { type: ["array", "null"], items: { type: "relationship" } }
                        } } }
                    ]
                });
            });

        ResourceDelegate.getSchema(["managed", "user"]).always(function () {
            readEntity.restore();
        }).then(function (schema) {
            assert.strictEqual(schema.properties.manager.type, "relationship");
            assert.strictEqual(_.findWhere(schema.allSchemas, { name: "role" }).schema.properties.members.type, "array",
                "the schemas of the related managed objects are read the same way");
            done();
        });
    });

    QUnit.test("queryStringForSearchableFields escapes and URL-encodes the search text", function (assert) {
        var queryString = ResourceDelegate.queryStringForSearchableFields(["userName", "sn"], 'a"b\\&c'),
            parts = queryString.split("&");

        assert.equal(decodeURIComponent(parts[0]), 'userName sw "a\\"b\\\\&c" or sn sw "a\\"b\\\\&c"',
            "the filter is escaped, and its & does not end the parameter");
        assert.deepEqual(parts.slice(1), ["_pageSize=10", "_fields=*"]);
    });
});

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
    "org/forgerock/openidm/ui/admin/role/util/UserQueryFilterEditor"
], function ($, UserQueryFilterEditor) {
    QUnit.module('UserQueryFilterEditor Tests');

    QUnit.test("createNameDropdown treats an unknown property name as text, not HTML", function (assert) {
        var editor = new UserQueryFilterEditor(),
            hostile = 'x"><b>y</b>',
            select;

        editor.model = { sourceProps: ["userName"] };

        select = editor.createNameDropdown($("<input>").val(hostile));

        assert.equal(select.find("option").length, 2, "exactly one option is added for the unknown value");
        assert.equal(select.find("b").length, 0, "no element is created from the value");
        assert.equal(select.find("option").last().attr("value"), "/" + hostile, "option value is the raw text");
        assert.equal(select.find("option").last().text(), hostile, "option label is the raw text");
        assert.equal(select.val(), "/" + hostile, "the unknown value is selected");
    });

    QUnit.test("createNameDropdown keeps an unknown JSON-pointer name as-is and labels it without the slash", function (assert) {
        var editor = new UserQueryFilterEditor(),
            select;

        editor.model = { sourceProps: ["userName"] };

        select = editor.createNameDropdown($("<input>").val("/custom"));

        assert.equal(select.find("option").length, 2);
        assert.equal(select.find("option").last().attr("value"), "/custom");
        assert.equal(select.find("option").last().text(), "custom");
        assert.equal(select.val(), "/custom");
    });

    QUnit.test("createNameDropdown selects a known property without adding a duplicate option", function (assert) {
        var editor = new UserQueryFilterEditor(),
            select;

        editor.model = { sourceProps: ["userName", "mail"] };

        select = editor.createNameDropdown($("<input>").val("/mail"));

        assert.equal(select.find("option").length, 2, "no extra option is added");
        assert.equal(select.val(), "/mail");
    });
});

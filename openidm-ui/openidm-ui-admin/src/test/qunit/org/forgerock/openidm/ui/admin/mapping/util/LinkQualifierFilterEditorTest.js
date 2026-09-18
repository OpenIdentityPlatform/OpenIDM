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
    "org/forgerock/openidm/ui/admin/mapping/util/LinkQualifierFilterEditor"
], function ($, LinkQualifierFilterEditor) {
    QUnit.module('LinkQualifierFilterEditor Tests');

    QUnit.test("createNameDropdown treats an unknown property name as text, not HTML", function (assert) {
        var editor = new LinkQualifierFilterEditor(),
            hostile = '/object/x"><b>y</b>',
            select;

        editor.model = { sourceProps: ["userName"] };
        editor.$el = $('<div><input class="name"></div>');
        editor.$el.find(".name").val(hostile);

        editor.createNameDropdown();
        select = editor.$el.find("select.name");

        assert.equal(select.length, 1, "the input is replaced by a select");
        assert.equal(select.find("option").length, 3, "Link Qualifier + source prop + exactly one option for the unknown value");
        assert.equal(select.find("b").length, 0, "no element is created from the value");
        assert.equal(select.find("option").last().attr("value"), hostile, "option value is the raw text");
        assert.equal(select.find("option").last().text(), 'x"><b>y</b>', "option label is the raw text without the /object/ prefix");
        assert.equal(select.val(), hostile, "the unknown value is selected");
    });

    QUnit.test("createNameDropdown selects a known property without adding a duplicate option", function (assert) {
        var editor = new LinkQualifierFilterEditor(),
            select;

        editor.model = { sourceProps: ["userName", "mail"] };
        editor.$el = $('<div><input class="name"></div>');
        editor.$el.find(".name").val("/object/mail");

        editor.createNameDropdown();
        select = editor.$el.find("select.name");

        assert.equal(select.find("option").length, 3, "Link Qualifier + two source props, no extra option");
        assert.equal(select.val(), "/object/mail");
    });
});

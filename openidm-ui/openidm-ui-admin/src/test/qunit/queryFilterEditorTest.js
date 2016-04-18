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
    "org/forgerock/openidm/ui/admin/mapping/util/QueryFilterEditor",
    "lodash"
],
 function ( QueryFilterEditor, _ ) {
     var queryFilterEditor = new QueryFilterEditor(),
     node = {
         "op": "or",
         "children": [
             { "name": "", "value": "", "tag": "equalityMatch", "children": [], "op": "expr" },
             { "name": "", "value": "", "tag": "equalityMatch", "children": [], "op": "expr" }
         ]
     };

    QUnit.module('QueryFilterEditor');

        QUnit.test('#serialize', function(assert) {

            var defaultVal = _.clone(node),
                expr = _.assign(_.clone(node), {op: 'expr', tag: "testTag", name: "testName", value: "testValue"}),
                noneVal = _.assign(_.clone(node), {op: 'none'}),
                notVal = _.assign(_.clone(node), {op: 'not'});

            assert.equal(queryFilterEditor.serialize(defaultVal), '( eq "" or eq "" )', '"default" ok');

            assert.equal(queryFilterEditor.serialize(expr), 'testName testTag "testValue"', '"expr" ok');

            assert.equal(queryFilterEditor.serialize(noneVal), '', '"none" ok');

            assert.equal(queryFilterEditor.serialize(notVal), '!( eq "" )', '"not" ok');
        });


});

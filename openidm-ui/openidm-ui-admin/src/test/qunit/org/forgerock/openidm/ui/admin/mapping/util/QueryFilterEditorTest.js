define([
    "org/forgerock/openidm/ui/admin/mapping/util/QueryFilterEditor",
    "lodash"
],
function(QueryFilterEditor, _ ) {
    var queryFilterEditor = new QueryFilterEditor(),
        deepFilledVal,
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
            expr = _.assign(_.clone(node), {op: "expr", tag: "testTag", name: "testName", value: "testValue"}),
            noneVal = _.assign(_.clone(node), {op: "none"}),
            notVal = _.assign(_.clone(node), {op: "not"}),
            filledVal = _.clone(node);

        assert.equal(queryFilterEditor.serialize(defaultVal), '(eq "" or eq "")', "default");

        assert.equal(queryFilterEditor.serialize(expr), 'testName testTag "testValue"', "expr");

        assert.equal(queryFilterEditor.serialize(noneVal), "", "none");

        assert.equal(queryFilterEditor.serialize(notVal), '!(eq "")', "not");

        filledVal.children = filledVal.children.map(function(child) {
            return _.assign(child, {"name":"a", "value": "b"});
        });

        assert.equal(queryFilterEditor.serialize(filledVal), '(a eq "b" or a eq "b")', "name/value");

        assert.equal(queryFilterEditor.serialize(deepFilledVal), '(a eq "b" or (a eq "c" and x eq "y") or (a eq "d" and x eq "z"))', "name/value deep");
    });

    deepFilledVal = {
        "op": "or",
        "children": [
            { "name": "a", "value": "b", "tag": "equalityMatch", "children": [], "op": "expr" },
            { "name": "", "value": "", "tag": "equalityMatch", "op": "and", "children": [
                { "name": "a", "value": "c", "tag": "equalityMatch", "children": [], "op": "expr" },
                { "name": "x", "value": "y", "tag": "equalityMatch", "children": [], "op": "expr" }
            ]},
            { "name": "", "value": "", "tag": "equalityMatch", "op": "and", "children": [
                { "name": "a", "value": "d", "tag": "equalityMatch", "children": [], "op": "expr" },
                { "name": "x", "value": "z", "tag": "equalityMatch", "children": [], "op": "expr" }
            ]}
        ]};
});

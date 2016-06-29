define([
    "lodash",
    "org/forgerock/openidm/ui/admin/util/FilterEditor"
], function (_, FilterEditor) {
    QUnit.module('FilterEditor Tests');
    var filterEditor = new FilterEditor(),
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
                    { "name": "x", "value": "z", "tag": "equalityMatch", "children": [], "op": "expr" },
                    { "name": "a", "value": "z", "tag": "equalityMatch", "children": [], "op": "expr" }
                ]}
            ]};

    QUnit.test("#createNode", function(assert) {
        var exprNode = { name: "", value: "", tag: "equalityMatch", children: [], op: "expr" },
            optionsNode = filterEditor.createNode({name: "firstName", value: "bob"});

        assert.deepEqual(filterEditor.createNode(), exprNode, "createNode returns an expression object when no options sent");

        assert.ok(
            optionsNode.name === "firstName" &&
            optionsNode.value === "bob",
            "createNode merges values passed in"
        );
    });

    QUnit.test("#deleteNode", function(assert) {
        var tree = _.cloneDeep(deepFilledVal),
            updatedTree = filterEditor.deleteNode(tree, ["","2","1"]),
            expectedTree = _.cloneDeep(deepFilledVal);

        expectedTree.children[2] = { "name": "", "value": "", "tag": "equalityMatch", "op": "and", "children": [
            { "name": "a", "value": "d", "tag": "equalityMatch", "children": [], "op": "expr" },
            { "name": "a", "value": "z", "tag": "equalityMatch", "children": [], "op": "expr" }
        ]};

        assert.deepEqual(updatedTree, expectedTree, "deleteNode will remove node at path");

        expectedTree = _.cloneDeep(expectedTree).children[2] = { "name": "a", "value": "d", "tag": "equalityMatch", "children": [], "op": "expr" };
        updatedTree = filterEditor.deleteNode(_.cloneDeep(updatedTree), ["","2","1"]);

        assert.deepEqual(updatedTree, updatedTree, "deleteNode checks for stranded nodes and prunes them");
    });

    QUnit.test("#getNode", function(assert) {
        var grabbedNode = filterEditor.getNode(_.clone(deepFilledVal), [2,1]),
            expectedNode = { "name": "x", "value": "z", "tag": "equalityMatch", "children": [], "op": "expr" },
            callGetNodeWithBadPath = function() {
                filterEditor.getNode(_.clone(deepFilledVal), ["2","1"]);
            };

        assert.deepEqual(grabbedNode, expectedNode, "getNode will return the node specified by path");
        assert.throws(callGetNodeWithBadPath, "expected array of numbers but got 2,1", "getNode throws error when given a bad path");
    });

    QUnit.test("#insertChildNode", function(assert) {
        var tree = _.cloneDeep(deepFilledVal),
            node = tree.children[2];
        filterEditor.insertChildNode(node, {name: "x", val: "d"});

        assert.equal(node.children.length, 4, "insertChildNode adds a new node the children collection of what is passed to it");
        assert.equal(node.children[3].name, "x", "insertChildNode addes properties specified to inserted node");
    });

    QUnit.test("#pruneStrandedNodes", function(assert) {
        var tree = _.cloneDeep(deepFilledVal),
            expectedTree = {
                "op": "or",
                "children": [
                    { "name": "a", "value": "b", "tag": "equalityMatch", "children": [], "op": "expr" },
                    { "name": "a", "value": "c", "tag": "equalityMatch", "children": [], "op": "expr" },
                    { "name": "a", "value": "d", "tag": "equalityMatch", "children": [], "op": "expr" }
                ]
            },
            prunedTree;

        // strand some children
        tree.children[1].children.pop();
        tree.children[2].children.pop();
        tree.children[2].children.pop();

        prunedTree = filterEditor.pruneStrandedNodes(tree);

        assert.deepEqual(prunedTree, expectedTree, "pruneStrandedNodes replaces single-child-parents with child expression nodes");

        tree = _.cloneDeep(deepFilledVal);
        expectedTree = { "name": "", "value": "", "tag": "equalityMatch", "op": "and", "children": [
            { "name": "a", "value": "c", "tag": "equalityMatch", "children": [], "op": "expr" },
            { "name": "x", "value": "y", "tag": "equalityMatch", "children": [], "op": "expr" }
        ]};

        tree.children.shift();
        tree.children.pop();

        prunedTree = filterEditor.pruneStrandedNodes(tree);

        assert.deepEqual(prunedTree, expectedTree, "pruneStrandedNodes replaces single-child-parents with child operation nodes");
    });

});

define([
    "org/forgerock/openidm/ui/common/resource/RelationshipArrayView",
    "org/forgerock/commons/ui/common/components/Messages",
    "lodash"
], function (RelationshipArrayView, messagesManager, _) {
    QUnit.module('RelationshipArrayView Tests');

    var relationshipArrayView = new RelationshipArrayView(),
        testPropValue = { testPropValue: "testPropValue"};

    //these two values must be present on the RelationshipArrayView to test getResourceCollectionDialogOptions
    relationshipArrayView.data.prop = { testProp: "testProp" };
    relationshipArrayView.schema = { testSchema: "testSchema"};

    QUnit.test("getResourceCollectionDialogOptions returns the correct options values", function () {
        var expectedOpts = {
                "property": {
                    "testProp": "testProp"
                },
                "schema": {
                    "testSchema": "testSchema"
                },
                "propertyValue" : undefined,
                "onChange" : (value, oldValue, newText, isFinalPromise) => {
                    return _this2.createRelationship(value).then(function () {
                        if (isFinalPromise) {
                            _this2.args.showChart = _this2.data.showChart;
                            _this2.render(_this2.args);
                            messagesManager.messages.addMessage({"message": $.t("templates.admin.ResourceEdit.addSuccess",{ objectTitle: _this2.data.prop.title })});
                        }
                    });
                },
                "multiSelect": true
            },
            resultOpts = relationshipArrayView.getResourceCollectionDialogOptions();

        //test the equality of the onChange event function
        //have to test this separately from the rest of the returned object
        //by toString'ing the function then removing all spaces
        QUnit.ok(expectedOpts.onChange.toString().replace(/\s/g,"") === resultOpts.onChange.toString().replace(/\s/g,""), "onChangeEvent option is correct when adding relationships");

        //test the rest of the object
        QUnit.assert.deepEqual(_.omit(resultOpts, "onChange"), _.omit(expectedOpts, "onChange"), "getResourceCollectionDialogOptions returns the correct options values when adding new relationships");

        //reset onchange and multiSelect for test when editing a relatioship
        expectedOpts.onChange = (value, oldValue, newText) => {
            return _this2.updateRelationship(value, oldValue).then( () => {
                _this2.render(_this2.args);
                messagesManager.messages.addMessage({"message": $.t("templates.admin.ResourceEdit.editSuccess",{ objectTitle: _this2.data.prop.title })});
            });
        };
        expectedOpts.multiSelect = false;
        expectedOpts.propertyValue = testPropValue;

        resultOpts = relationshipArrayView.getResourceCollectionDialogOptions(testPropValue);

        QUnit.ok(expectedOpts.onChange.toString().replace(/\s/g,"") === resultOpts.onChange.toString().replace(/\s/g,""), "onChangeEvent option is correct when editing a relationship");

        QUnit.assert.deepEqual(_.omit(resultOpts, "onChange"), _.omit(expectedOpts, "onChange"), "getResourceCollectionDialogOptions returns the correct options values when editing a relationship");
    });

});

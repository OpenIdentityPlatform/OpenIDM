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

/*global define */

define("org/forgerock/openidm/ui/admin/role/util/UserQueryFilterEditor", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/mapping/util/QueryFilterEditor",
    "org/forgerock/openidm/ui/admin/delegates/ScriptDelegate",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate"
], function ($, _,
             QueryFilterEditor,
             ScriptDelegate,
             ResourceDelegate) {

    var UserQueryFilterEditor = QueryFilterEditor.extend({
        events: {
            "change .expressionTree :input": "updateFilterNodeValue",
            "click .expressionTree .add-btn": "addFilterNode",
            "click .expressionTree .remove-btn": "removeFilterNode"
        },
        model: {

        },

        render: function (args, callback) {
            this.setElement(args.element);

            this.data = {
                config: {
                    ops: [
                        "and",
                        "or",
                        "not",
                        "expr"
                    ],
                    tags: [
                        "pr",
                        "equalityMatch",
                        "approxMatch",
                        "co",
                        "greaterOrEqual",
                        "gt",
                        "lessOrEqual",
                        "lt"
                    ]
                },
                showSubmitButton: false
            };

            ResourceDelegate.getSchema(["managed","user"]).then(_.bind(function(userSchema) {
              this.model.sourceProps = _.keys(userSchema.properties);

              this.data.filterString = args.queryFilter;

              if (this.data.filterString !== "") {
                  ScriptDelegate.parseQueryFilter(this.data.filterString).then(_.bind(function (queryFilterTree) {
                      this.data.queryFilterTree = queryFilterTree;
                      this.data.filter = this.transform(this.data.queryFilterTree);
                      this.delegateEvents(this.events);
                      this.renderExpressionTree(_.bind(function() {
                          this.changeToDropdown();
                      }, this));
                  }, this));
              } else {
                  this.data.filter = { "op": "none", "children": []};
                  this.delegateEvents(this.events);
                  this.renderExpressionTree(_.bind(function() {
                      this.changeToDropdown();
                  }, this));
              }

              if (callback) {
                  callback();
              }
            },this));
        },
        removeFilterNode: function(event) {
            this.removeNode(event, _.bind(function() {
                this.changeToDropdown();
            }, this));
        },
        addFilterNode : function(event) {
            this.addNodeAndReRender(event, _.bind(function() {
                this.changeToDropdown();
            }, this));
        },
        updateFilterNodeValue: function(event) {
            this.updateNodeValue(event, _.bind(function(){
                this.changeToDropdown();
            }, this));
        },
        changeToDropdown: function() {
            var _this = this;

            this.$el.find(".name").each(function(name, index){
                var currentSelect = this,
                    parentHolder = $(currentSelect).closest(".node"),
                    tempValue,
                    newSelect = _this.createNameDropdown(this);

                $(currentSelect).replaceWith(newSelect);

                $(newSelect).selectize({
                    create: true
                });

                $(newSelect)[0].selectize.setValue($(newSelect).val());

                $(newSelect)[0].selectize.on('option_add', function(value, data){
                    if(_this.model.previousSelectizeAdd !== value) {
                        _this.model.previousSelectizeAdd = "/" + value;

                        $(newSelect)[0].selectize.removeOption(value);
                        $(newSelect)[0].selectize.addOption({value: "/" + value, text: value});
                        $(newSelect)[0].selectize.addItem("/" + value);
                    }
                });
            });
        },
        createNameDropdown: function(input) {
            var baseElement = $('<select style="width:100%;" class="name form-control"></select>'),
                tempValue = $(input).val(),
                displayValue;

            _.each(this.model.sourceProps, function(source) {
                if(source !== undefined) {
                    baseElement.append('<option value="/' +source +'">' +source +'</option>');
                }
            });

            if(tempValue.length > 0 && baseElement.find("option[value='/" +tempValue  +"']").length === 0 && baseElement.find("option[value='/" +tempValue  +"']").length === 0) {
                displayValue = tempValue.replace("/", "");

                baseElement.append('<option value="/' +tempValue +'">' +displayValue +'</option>');
            }

            baseElement.val(tempValue);

            return baseElement;
        }
    });

    return UserQueryFilterEditor;

});

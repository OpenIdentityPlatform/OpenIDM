/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
 *
 * The
 * contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global define */

define("org/forgerock/openidm/ui/admin/mapping/util/LinkQualifierFilterEditor", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/mapping/util/QueryFilterEditor",
    "org/forgerock/openidm/ui/admin/util/QueryFilterUtils",
    "org/forgerock/openidm/ui/admin/util/LinkQualifierUtils"
], function ($, _,
             QueryFilterEditor,
             QueryFilterUtils,
             LinkQualifierUtils) {

    var LinkQualifierFilterEditor = QueryFilterEditor.extend({
        events: {
            "change .expressionTree :input": "updateLinkQualifierNodeValue",
            "click .expressionTree .add-btn": "addLinkQualifierNode",
            "click .expressionTree .remove-btn": "removeLinkQualifierNode"
        },
        model: {

        },

        render: function (args) {
            this.setElement(args.element);

            this.model.sourceProps = args.mapProps;

            this.model.linkQualifiers = LinkQualifierUtils.getLinkQualifier(args.mappingName);

            this.data = {
                config: {
                    ops: [
                        "and",
                        "or",
                        "expr"
                    ],
                    tags: [
                        "pr",
                        "equalityMatch",
                        "ne",
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

            this.data.filterString = args.queryFilter;
            if (this.data.filterString !== "") {
                this.data.queryFilterTree = QueryFilterUtils.convertFrom(this.data.filterString);
                if (_.isArray(this.data.queryFilterTree) && this.data.queryFilterTree.length === 1) {
                    this.data.filter = this.transform(this.data.queryFilterTree[0]);
                } else {
                    this.data.filter = this.transform(this.data.queryFilterTree);
                }
            } else {
                this.data.filter = { "op": "none", "children": []};
            }

            this.delegateEvents(this.events);

            this.renderExpressionTree(_.bind(function() {
                this.changeToDropdown();
            }, this));

            $(".bootstrap-dialog").removeAttr("tabindex");
        },
        removeLinkQualifierNode: function(event) {
            this.removeNode(event, _.bind(function() {
                this.changeToDropdown();
            }, this));
        },
        addLinkQualifierNode : function(event) {
            this.addNode(event, _.bind(function() {
                this.changeToDropdown();
            }, this));
        },
        updateLinkQualifierNodeValue: function(event) {
            this.updateNodeValue(event, _.bind(function(){
                this.changeToDropdown();
            }, this));
        },
        changeToDropdown: function() {
            var _this = this;

            this.createNameDropdown();

            this.$el.find(".name").each(function(name, index){
                var currentSelect = this,
                    parentHolder = $(currentSelect).closest(".node"),
                    tempValue;

                if($(currentSelect).val() === "/linkQualifier") {
                    tempValue = parentHolder.find(".value").val();
                    parentHolder.find(".value").replaceWith(_this.createLinkQualifierCombo());
                    parentHolder.find(".value").val(tempValue);
                }

                $(currentSelect).selectize({
                    create: true
                });

                $(currentSelect)[0].selectize.setValue($(currentSelect).val());

                $(currentSelect)[0].selectize.on('option_add', function(value, data){
                    if(_this.model.previousSelectizeAdd !== value) {
                        _this.model.previousSelectizeAdd = "/object/" + value;

                        $(currentSelect)[0].selectize.removeOption(value);
                        $(currentSelect)[0].selectize.addOption({value: "/object/" + value, text: value});
                        $(currentSelect)[0].selectize.addItem("/object/" + value);
                    }
                });

                $(this).bind("change", function() {
                    var value = $(this).val(),
                        parent = $(this).closest(".node");

                    if(value === "/linkQualifier") {
                        parent.find(".value").replaceWith(_this.createLinkQualifierCombo());
                    } else {
                        parent.find(".value").replaceWith('<input type="text" class="value form-control">');
                    }

                    parent.find(".value").trigger("change");
                });
            });
        },
        createNameDropdown: function() {
            var baseElement = $('<select style="width:100%;" class="name form-control"><option value="/linkQualifier">Link Qualifier</option></select>'),
                appendElement,
                tempValue,
                displayValue;

            _.each(this.model.sourceProps, function(source) {
                if(source !== undefined) {
                    baseElement.append('<option value="/object/' +source +'">' +source +'</option>');
                }
            });

            this.$el.find(".name").each(function(name, index){
                tempValue = $(this).val();
                appendElement = baseElement.clone();

                if(tempValue.length > 0 && appendElement.find("option[value='/object/" +tempValue  +"']").length === 0 && appendElement.find("option[value='" +tempValue  +"']").length === 0) {
                    displayValue = tempValue.replace("/object/", "");

                    appendElement.append('<option value="' +tempValue +'">' +displayValue +'</option>');
                }

                $(this).replaceWith(appendElement);

                appendElement.val(tempValue);
            });
        },
        createLinkQualifierCombo: function() {
            var baseElement =  $('<select style="width:100%;" class="value form-control"></select>');

            _.each(this.model.linkQualifiers, function(linkQualifier) {
                baseElement.append('<option value="' +linkQualifier +'">' +linkQualifier +'</option>');
            });

            return baseElement;
        }
    });

    return LinkQualifierFilterEditor;

});

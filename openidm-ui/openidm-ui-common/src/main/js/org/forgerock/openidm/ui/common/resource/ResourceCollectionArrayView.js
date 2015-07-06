/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
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

define("org/forgerock/openidm/ui/common/resource/ResourceCollectionArrayView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/openidm/ui/common/util/ResourceCollectionUtils",
    "org/forgerock/openidm/ui/common/resource/GenericEditResourceView"
], function($, _, AbstractView, constants, resourceDelegate, resourceCollectionUtils) {
    var ResourceCollectionArrayView = AbstractView.extend({
            template: "templates/admin/resource/ResourceCollectionArrayViewTemplate.html",
            noBaseTemplate: true,
            events: {
                "click .add-array-item": "addArrayItem",
                "click .remove-array-item": "removeArrayItem",
                "click .resourceListItem": "showResource"
            },
            render: function (args, callback) {
                var parentRender = _.bind(function() {
                        this.parentRender(_.bind(function() {
                            this.setupAutocomplete(this.data.prop, this.onChange);
                            this.convertToHuman();

                            if(callback) {
                                callback();
                            }
                        }, this));
                    }, this);

                if(args) {
                    this.element = args.element;
                    this.data.prop = args.prop;

                    this.onChange = args.onChange;

                    resourceDelegate.getSchema(this.data.prop.items.resourceCollection.path.split("/")).then(_.bind(function(schema) {
                        this.data.propTitle = schema.title || this.data.prop.title;

                        this.data.headerValues = resourceCollectionUtils.getHeaderValues(this.data.prop.items.resourceCollection.query.fields, schema.properties);

                        parentRender();
                    }, this));
                } else {
                    parentRender();
                }
            },
            setupAutocomplete: function(prop) {
                var autocompleteField = this.$el.parent().find("#autoCompleteResourceCollection_" + prop.propName),
                    onChange = _.bind(function(value) {
                        var newVal = this.data.prop.items.resourceCollection.path + "/" + value;

                        if(!_.contains(this.data.prop.value, newVal)) {
                            if(!this.data.prop.value) {
                                this.data.prop.value = [];
                            }
                            this.data.prop.value.push(newVal);
                        }
                    }, this);

                    resourceCollectionUtils.setupAutocompleteField(autocompleteField, prop, { onChange: onChange });
            },
            convertToHuman: function() {
                var listElements = this.$el.find(".resourceListItem");

                _.each(listElements, _.bind(function(element) {
                    var path = $(element).attr("resourcePath"),
                        getRowContents = function(txt) {
                            return _.map(txt.split(resourceCollectionUtils.displayTextDelimiter), function(val) {
                                return '<div class="col-xs-2">' +  val + '</div>';
                            }).join("");
                        };
                    if(path.indexOf(this.data.prop.items.resourceCollection.path) === 0) {
                        if(resourceCollectionUtils.resourceCollectionCache[path]) {
                            $(element).find(".deleteArrayItem").before(getRowContents(resourceCollectionUtils.resourceCollectionCache[path]));
                        } else {
                            resourceDelegate.readResource("/" + constants.context, path).then(_.bind(function(result){
                                var txt = resourceCollectionUtils.getDisplayText(this.data.prop, result);
                                $(element).find(".deleteArrayItem").before(getRowContents(txt));
                            }, this));
                        }
                    } else {
                        $(element).find(".deleteArrayItem").before(getRowContents(path));
                    }
                }, this));
            },
            addArrayItem: function(e) {
                if(e) {
                    e.preventDefault();
                }

                var value = this.$el.parent().find("#autoCompleteResourceCollection_" + this.data.prop.propName).val();

                if (value && value.length) {
                    this.data.prop.value.push({ "_ref": this.data.prop.items.resourceCollection.path + "/" + value });
                } else {
                    return;
                }

                this.render();
                this.onChange();
            },
            removeArrayItem: function(e) {
                var path = $(e.target).closest(".list-group-item").attr("resourcePath");

                if(e) {
                    e.preventDefault();
                }

                this.data.prop.value = _.reject(this.data.prop.value, function(val) { return val._ref === path; });

                this.render();
                this.onChange();
            },
            showResource: function(e) {
                if(!$(e.target).hasClass("fa-times")){
                    resourceCollectionUtils.showResource($(e.target).closest(".resourceListItem").attr("resourcePath"));
                }
            }
        });

    return ResourceCollectionArrayView;
});

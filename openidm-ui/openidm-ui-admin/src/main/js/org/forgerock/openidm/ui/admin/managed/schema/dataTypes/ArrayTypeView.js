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
 * Copyright 2017 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "handlebars",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/admin/managed/schema/dataTypes/ObjectTypeView",
    "org/forgerock/openidm/ui/admin/managed/schema/dataTypes/ArrayTypeView"
], function($, _,
    handlebars,
    AdminAbstractView,
    ObjectTypeView,
    NestedArrayTypeView
) {

    var ArrayTypeView = AdminAbstractView.extend({
        template: "templates/admin/managed/schema/dataTypes/ArrayTypeViewTemplate.html",
        noBaseTemplate: true,
        events: {},
        model: {},
        partials: [],
        /**
        * @param {object} args - example
                this.data.arrayTypeView.render({
                    elementId: "arrayTypeContainer",
                    propertyName: this.data.propertyName,
                    propertyRoute: this.args.join("/"),
                    items: this.data.property.items,
                    makeChanges: _.bind(this.saveProperty,this),
                    nestingIndex: 0
                });
        * @param {function} callback - a function to be executed after load
        */
        render: function(args, callback) {
            var refreshView = false;

            if (args) {
                this.element = "#" + args.elementId;
                this.data.propertyName = args.propertyName;
                this.data.propertyRoute = args.propertyRoute;
                this.data.items = args.items;
                this.makeChanges = args.makeChanges;
                this.data.nestingIndex = args.nestingIndex;
            }

            this.parentRender(() => {

                if (this.data.items) {
                    this.loadNestedView(this.data.items.type);
                } else {
                    this.$el.find(".nestedItemTypeContainer").hide();
                }
                this.setItemTypeChangeEvent();

                if (callback) {
                    callback();
                }

            });

        },
        getValue: function () {
            var itemType = this.$el.find("#itemTypeSelect_" + this.data.nestingIndex).val(),
                items = {
                    type: itemType
                };

            if (this.data.arrayTypeView) {
                items.items = this.data.arrayTypeView.getValue();
            }

            if (this.data.objectProperties) {
                items = _.extend(items,this.data.objectProperties.getValue());
            }

            return items;
        },
        setItemTypeChangeEvent: function () {
            this.$el.find("#itemTypeSelect_" + this.data.nestingIndex).on("change", (e) => {
                var type = $(e.target).val();

                this.$el.find("#nestedItemTypeContainer_" + this.data.nestingIndex).empty();

                this.loadNestedView(type);
            });
        },
        loadNestedView: function(type) {
            this.$el.find(".nestedItemTypeContainer").show();
            if (type === "object") {
                this.data.objectProperties = new ObjectTypeView();

                this.data.objectProperties.render({
                    elementId: "nestedItemTypeContainer_" + this.data.nestingIndex,
                    schema: this.data.items,
                    saveSchema: () => {
                        this.makeChanges();
                    },
                    parentObjectName: this.data.propertyName,
                    propertyRoute: this.data.propertyRoute,
                    isArrayItem: true
                });
            } else if (type === "array") {
                this.data.arrayTypeView = new ArrayTypeView();

                this.data.arrayTypeView.render({
                    elementId: "nestedItemTypeContainer_" + this.data.nestingIndex,
                    propertyName: this.data.propertyName,
                    propertyRoute: this.data.propertyRoute,
                    items: this.data.items.items || this.data.items,
                    makeChanges: this.makeChanges,
                    nestingIndex: parseInt(this.data.nestingIndex,10) + 1
                });
            } else {
                this.$el.find(".nestedItemTypeContainer").hide();
            }
        }
    });

    return ArrayTypeView;
});

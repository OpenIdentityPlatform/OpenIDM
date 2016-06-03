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
    "jquery",
    "underscore",
    "handlebars",
    "jsonEditor",
    "backbone",
    "backgrid",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/admin/managed/schema/dataTypes/EditResourceCollectionDialog",
    "org/forgerock/openidm/ui/admin/managed/schema/util/SchemaUtils"
], function($, _,
    handlebars,
    JSONEditor,
    Backbone,
    Backgrid,
    BackgridUtils,
    AdminAbstractView,
    EditResourceCollectionDialog,
    SchemaUtils
) {

    var RelationshipTypeView = AdminAbstractView.extend({
        template: "templates/admin/managed/schema/dataTypes/RelationshipTypeViewTemplate.html",
        noBaseTemplate: true,
        events: {
            "click .addNewRefPropertyButton" : "addNewRefProperty",
            "click .cancelEditRefProperty" : "cancelEditRefProperty",
            "click .saveEditRefProperty" : "saveEditRefProperty",
            "click .addNewResourceCollectionButton" : "addNewResourceCollection",
            "change .setValueOnchange" : "getValue",
            "blur .setValueOnchange" : "getValue"
        },
        model: {},
        partials: [
            "partials/managed/schema/_refPropertiesNewRow.html",
            "partials/managed/schema/_refPropertiesEditableRow.html"
        ],
        /**
        * @param {object} args - two properties { elementId : "#someElement", propertySchema: schemaObject }
        * @param {function} callback - a function to be executed after load
        */
        render: function(args, callback) {
            if (args) {
                this.element = "#" + args.elementId;
                this.data.elementId = args.elementId;
                this.propertySchema = args.propertySchema;

                if (this.propertySchema.type.itemType) {
                    this.data.currentValue = this.propertySchema.type.itemType;
                } else {
                    this.data.currentValue = this.propertySchema.type;
                }
            }

            this.parentRender(() => {
                this.loadPropertiesGrid();
                this.loadResourceCollectionGrid();
                if (callback) {
                    callback();
                }

            });

        },
        /**
        *   this function serves two purposes
        *   one: it returns this view's currentValue when needed
        *   two: it sets the current value of "reverseRelationship","reversePropertyName", and "validate" properties
        *        based on the current values of their respective input fields
        */
        getValue: function () {
            this.data.currentValue.reverseRelationship = this.$el.find(this.element + "-reverseRelationship").val() === "1";
            this.data.currentValue.reversePropertyName = this.$el.find(this.element + "-reversePropertyName").val();
            this.data.currentValue.validate = this.$el.find(this.element + "-validate").val() === "1";
            return this.data.currentValue;
        },
        loadPropertiesGrid: function () {
            var self = this,
                cols = [
                    {
                        name: "propertyName",
                        label: $.t("templates.managed.schemaEditor.propertyName"),
                        cell: "string",
                        sortable: false,
                        editable: false
                    },
                    {
                        name: "label",
                        label: $.t("templates.managed.schemaEditor.label"),
                        cell: "string",
                        sortable: false,
                        editable: false
                    },
                    {
                        label: "",
                        cell: BackgridUtils.ButtonCell([
                            {
                                className: "fa fa-times grid-icon col-sm-1 pull-right",
                                callback: function(e){
                                    //must add 1 to the row index here to take into account the hidden _id property
                                    var itemIndex = SchemaUtils.getClickedRowIndex(e) + 1;
                                    self.data.currentValue.properties._refProperties.properties.splice(itemIndex, 1);
                                    self.getValue();
                                    self.render();
                                }
                            },
                            {
                                // No callback necessary, the row click will trigger the edit
                                className: "fa fa-pencil grid-icon col-sm-1 pull-right"
                            },
                            {
                                className: "dragToSort fa fa-arrows grid-icon col-sm-1 pull-right"
                            }
                        ]),
                        sortable: false,
                        editable: false
                    }
                ],
                propertiesGrid,
                refProperties = _.reject(this.data.currentValue.properties._refProperties.properties, (refProp) => {
                    return refProp.propertyName === "_id";
                }),
                makeSortable = () => {
                    BackgridUtils.sortable({
                        "containers": [this.$el.find(".refPropertiesList tbody")[0]],
                        "rows": _.clone(refProperties, true)
                    }, _.bind(function(newOrder) {
                        //get the _id property
                        var idProp = this.data.currentValue.properties._refProperties.properties[0];
                        //put the _id property at the 0 index of the newOrder array
                        newOrder.unshift(idProp);

                        this.data.currentValue.properties._refProperties.properties = newOrder;
                        this.getValue();
                        this.render();
                    }, this));
                },
                addNewRow = $(handlebars.compile("{{> managed/schema/_refPropertiesNewRow}}")());

            this.model.refPropertiesCollection = new Backbone.Collection();

            _.each(refProperties, _.bind(function (refProp) {
                this.model.refPropertiesCollection.add(refProp);
            }, this));

            propertiesGrid = new Backgrid.Grid({
                columns: BackgridUtils.addSmallScreenCell(cols),
                collection: this.model.refPropertiesCollection,
                row: BackgridUtils.ClickableRow.extend({
                    callback: _.bind(function(e) {
                        var row = $(e.target).closest("tr"),
                            name = row.find("td:eq(0)").text(),
                            label = row.find("td:eq(1)").text(),
                            editableRow = $(handlebars.compile("{{> managed/schema/_refPropertiesEditableRow}}")({ name : name, label : label }));

                        e.preventDefault();
                        row.replaceWith(editableRow);
                        //hide the add row
                        this.$el.find(".refPropertiesNewRow").hide();
                    }, this)
                })
            });

            this.$el.find(".refPropertiesList").append(propertiesGrid.render().el);

            this.$el.find(".refPropertiesList tbody").append(addNewRow);

            makeSortable();
        },
        /**
        * adds a new refProperty to the properties grid
        */
        addNewRefProperty: function(e) {
            var name = this.$el.find(".newRefPropertyName").val(),
                label = this.$el.find(".newRefPropertyLabel").val(),
                newProp = {
                    propertyName: name,
                    label: label,
                    type: "string"
                };

            e.preventDefault();

            if (name.length) {
                this.data.currentValue.properties._refProperties.properties.push(newProp);
                this.getValue();
                this.render();
            }
        },
        /**
        * saves the editable row for a refProperty in the properties grid
        */
        saveEditRefProperty: function(e) {
            var row = $(e.target).closest("tr"),
                name = row.find(".editRefPropertyName").val(),
                label = row.find(".editRefPropertyLabel").val(),
                editedProp = {
                    propertyName: name,
                    label: label,
                    type: "string"
                },
                //must add 1 here to take into account the hidden _id property
                rowIndex = SchemaUtils.getClickedRowIndex(e) + 1;

            e.preventDefault();

            if (name.length) {
                this.data.currentValue.properties._refProperties.properties[rowIndex] = editedProp;
                this.render();
            }
        },
        /**
        * cancels editing a row in the properties grid
        */
        cancelEditRefProperty: function (e) {
            e.preventDefault();
            this.getValue();
            this.render();
        },
        /**
        * builds the resource collection grid
        */
        loadResourceCollectionGrid: function () {
            var self = this,
                cols = [
                    {
                        name: "label",
                        label: "Label",
                        cell: "string",
                        sortable: false,
                        editable: false
                    },
                    {
                        label: "",
                        cell: BackgridUtils.ButtonCell([
                            {
                                className: "fa fa-times grid-icon col-sm-1 pull-right",
                                callback: function(e){
                                    self.data.currentValue.resourceCollection.splice(SchemaUtils.getClickedRowIndex(e), 1);
                                    self.getValue();
                                    self.render();
                                }
                            },
                            {
                                // No callback necessary, the row click will trigger the edit
                                className: "fa fa-pencil grid-icon col-sm-1 pull-right"
                            }
                        ]),
                        sortable: false,
                        editable: false
                    }
                ],
                resourceCollectionGrid;

            this.model.resourceCollectionCollection = new Backbone.Collection();

            _.each(this.data.currentValue.resourceCollection, _.bind(function (resourceCollection) {
                this.model.resourceCollectionCollection.add(resourceCollection);
            }, this));

            resourceCollectionGrid = new Backgrid.Grid({
                columns: BackgridUtils.addSmallScreenCell(cols),
                collection: this.model.resourceCollectionCollection,
                row: BackgridUtils.ClickableRow.extend({
                    callback: _.bind(function(e) {
                        var rowIndex = SchemaUtils.getClickedRowIndex(e),
                            editDialog = new EditResourceCollectionDialog();

                        editDialog.render({
                            resourceCollectionIndex: rowIndex,
                            parent: this
                        });
                    }, this)
                })
            });

            this.$el.find(".resourceCollectionList").append(resourceCollectionGrid.render().el);
        },
        addNewResourceCollection: function (e) {
            var resourceCollectionDialog = new EditResourceCollectionDialog();

            e.preventDefault();

            resourceCollectionDialog.render({
                parent: this
            });
        }
    });

    return RelationshipTypeView;
});

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
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "bootstrap-dialog",
    "org/forgerock/openidm/ui/admin/util/AdminUtils",
    "org/forgerock/openidm/ui/admin/managed/schema/util/SchemaUtils",
    "backbone",
    "backgrid",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function($, _,
    handlebars,
    AbstractView,
    conf,
    constants,
    uiUtils,
    BootstrapDialog,
    AdminUtils,
    SchemaUtils,
    Backbone,
    Backgrid,
    BackgridUtils,
    UIUtils
) {
    var EditResourceCollectionDialog = AbstractView.extend({
        template: "templates/admin/managed/schema/dataTypes/EditResourceCollectionDialogTemplate.html",
        el: "#dialogs",
        data: {},
        model: {
            fields: {},
            sortKeys: {}
        },
        /**
        * @param {object} args - two properties { resourceCollectionIndex: 2, parent: parentRelationshipTypeView }
        * @param {function} callback - a function to be executed after load
        */
        render: function(args, onLoadCallback) {
            $.when(
                AdminUtils.getAvailableResourceEndpoints(),
                UIUtils.preloadPartial("partials/managed/schema/_resourceCollectionFieldNewRow.html"),
                UIUtils.preloadPartial("partials/managed/schema/_resourceCollectionFieldEditableRow.html")
            ).then((resources) => {
                var _this = this,
                    title = $.t("templates.managed.schemaEditor.addResourceCollection");

                //available resources to be used for Resources dropdown
                this.data.resources = resources;
                //default currentValue used when Adding a new resource collection
                this.data.currentValue = {
                    "path" : "",
                    "label" : "",
                    "query" : {
                        "queryFilter" : "",
                        "fields" : [],
                        "sortKeys" : []
                    }
                };

                this.onLoadCallback = onLoadCallback;

                this.parent = args.parent;

                if (_.isNumber(args.resourceCollectionIndex)) {
                    title = $.t("templates.managed.schemaEditor.editResourceCollection");
                    this.resourceCollectionIndex = args.resourceCollectionIndex;
                    this.data.currentValue = _.cloneDeep(this.parent.data.currentValue.resourceCollection[args.resourceCollectionIndex]);

                    //if this resource collection's value is not in the list of available resources add it
                    if(!_.contains(this.data.resources, this.data.currentValue.path)) {
                        this.data.resources.push(this.data.currentValue.path);
                    }
                }

                this.data.displayValue = JSON.stringify(this.data.currentValue,null,4);

                this.currentDialog = $('<div id="editResourceCollectionDialog"></div>');

                $('#dialogs').append(this.currentDialog);

                //change dialog
                this.dialog = BootstrapDialog.show({
                    title: title,
                    type: BootstrapDialog.TYPE_DEFAULT,
                    message: this.currentDialog,
                    size: BootstrapDialog.SIZE_WIDE,
                    cssClass : "objecttype-windows",
                    onshown : _.bind(function (dialogRef) {
                        this.loadTemplate();

                        if (this.onLoadCallback) {
                            this.onLoadCallback();
                        }
                    }, _this),
                    buttons: [
                        {
                            label: $.t('common.form.cancel'),
                            id: "resourceCollectionDialogCloseBtn",
                            action: function(dialogRef){
                                dialogRef.close();
                            }
                        },
                        {
                            label: $.t('common.form.save'),
                            cssClass: "btn-primary",
                            id: "resourceCollectionDialogSaveBtn",
                            action: function(dialogRef) {
                                _this.setTextFields();
                                if (_.isNumber(_this.resourceCollectionIndex)) {
                                    _this.parent.data.currentValue.resourceCollection[_this.resourceCollectionIndex] = _this.data.currentValue;
                                } else {
                                    _this.parent.data.currentValue.resourceCollection.push(_this.data.currentValue);
                                }
                                dialogRef.close();
                                _this.parent.render();
                            }
                        }
                    ]
                });
            });
        },
        loadTemplate: function (refreshView) {
            if (refreshView) {
                this.setTextFields();
            }
            uiUtils.renderTemplate(
                    this.template,
                    this.currentDialog,
                    _.extend({}, conf.globalData, this.data),
                    _.bind(function(){
                        this.setupResourceTypeField();
                        this.setupResourcePropertiesGrid(this.data.currentValue.path.split("/"), this.data.currentValue.query.fields);
                    }, this),
                    "replace"
                );
        },
        setupResourceTypeField: function() {
            var autocompleteField = this.currentDialog.find(".resourceSelectionDropdown"),
                opts = {
                    create: true,
                    onChange: (path) => {
                        this.data.currentValue.query.fields = [];
                        this.data.currentValue.query.sortKeys = this.data.currentValue.query.fields;
                        this.data.currentValue.path = path;
                        this.setupResourcePropertiesGrid(path.split("/"), []);
                    }
                };

            autocompleteField.selectize(opts);
        },
        /**
         * sets up editable grid with selectize inputs
         *
         * @param {string} path - resource path ("managed/user")
         * @param {array} listValue - an array of properties
         */
        setupResourcePropertiesGrid: function (path, listValue) {
            var listElement = this.currentDialog.find(".resourceCollectionFieldsList");

            //empty the existing
            listElement.empty();

            AdminUtils.findPropertiesList(path).then( (availableProps) => {
                var cols = [
                    {
                        name: "name",
                        label: $.t("templates.managed.schemaEditor.propertyName"),
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
                                    var itemIndex = SchemaUtils.getClickedRowIndex(e);
                                    this.data.currentValue.query.fields.splice(itemIndex, 1);
                                    this.data.currentValue.query.sortKeys.splice(itemIndex, 1);
                                    this.loadTemplate(true);
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
                    newRow,
                    makeSortable = () => {
                        BackgridUtils.sortable({
                            "containers": [listElement.find("tbody")[0]],
                            "rows": _.clone(this.data.currentValue.query.fields, true)
                        }, _.bind(function(newOrder) {
                            this.data.currentValue.query.fields = newOrder;
                            this.data.currentValue.query.sortKeys = newOrder;
                            this.loadTemplate(true);
                        }, this));
                    };
                /*
                    filter out the all props that are named "_id", are not of type string,
                    , are encrypted, or are already in the current list from the availableProps
                */
                availableProps = AdminUtils.filteredPropertiesList(availableProps, this.data.currentValue.query.fields);

                newRow = $(handlebars.compile("{{> managed/schema/_resourceCollectionFieldNewRow}}")({
                    availableProps: availableProps
                }));

                this.model.fields.resourceFieldsCollection = new Backbone.Collection();

                _.each(listValue, _.bind(function (prop) {
                    var propObject = {
                        name: prop
                    };
                    this.model.fields.resourceFieldsCollection.add(propObject);
                }, this));

                propertiesGrid = new Backgrid.Grid({
                    columns: BackgridUtils.addSmallScreenCell(cols),
                    collection: this.model.fields.resourceFieldsCollection,
                    row: BackgridUtils.ClickableRow.extend({
                        callback: _.bind(function(e) {
                            var row = $(e.target).closest("tr"),
                                propName = row.find("td:eq(0)").text(),
                                editableRow = $(handlebars.compile("{{> managed/schema/_resourceCollectionFieldEditableRow}}")({
                                    propName : propName,
                                    availableProps: availableProps
                                }));

                            e.preventDefault();
                            row.replaceWith(editableRow);
                            //set selectize dropdown
                            editableRow.find(".resourceCollectionFieldName").selectize({ create : true });
                            //hide the add row
                            editableRow.parent().find(".resourceCollectionFieldNewRow").hide();

                            editableRow.find(".cancelEditResourceCollectionFieldRow").click((e) => {
                                e.preventDefault();
                                this.loadTemplate(true);
                            });

                            editableRow.find(".saveEditResourceCollectionFieldRow").click((e) => {
                                this.saveEditRow(e);
                            });
                        }, this)
                    })
                });

                listElement.append(propertiesGrid.render().el);

                listElement.find("tbody").append(newRow);
                //selectize the dropdown
                newRow.find(".resourceCollectionFieldName").selectize({ create : true });

                newRow.find(".addNewResourceCollectionFieldButton").click((e) => {
                    this.saveEditRow(e, true);
                });

                makeSortable();

                if (this.data.currentValue.path.length) {
                    this.currentDialog.find(".resourceDependentList").show();
                }
            });
        },
        saveEditRow: function(e, isNew) {
            var row = $(e.target).closest("tr"),
                propName = row.find(".resourceCollectionFieldName").val(),
                rowIndex = SchemaUtils.getClickedRowIndex(e);

            e.preventDefault();

            if (propName.length) {
                if (isNew) {
                    this.data.currentValue.query.fields.push(propName);
                } else {
                    this.data.currentValue.query.fields[rowIndex] = propName;
                }

                this.loadTemplate(true);
            }
        },
        setTextFields: function () {
            this.data.currentValue.label = this.currentDialog.find(".resourceCollectionLabel").val();
            this.data.currentValue.query.queryFilter = this.currentDialog.find(".resourceCollectionQueryFilter").val();
        }
    });

    return EditResourceCollectionDialog;
});

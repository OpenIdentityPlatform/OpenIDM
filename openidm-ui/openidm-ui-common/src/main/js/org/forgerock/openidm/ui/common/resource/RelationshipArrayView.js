/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All rights reserved.
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

/*global define, sessionStorage */

define("org/forgerock/openidm/ui/common/resource/RelationshipArrayView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "backgrid",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractCollection",
    "org/forgerock/openidm/ui/common/util/ResourceCollectionUtils",
    "org/forgerock/openidm/ui/common/resource/ResourceCollectionSearchDialog",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "backgrid-paginator",
    "backgrid-selectall"
], function(
        $, 
        _, 
        AbstractView, 
        constants, 
        Backgrid, 
        BackgridUtils,
        resourceDelegate, 
        messagesManager,
        AbstractCollection,
        resourceCollectionUtils,
        resourceCollectionSearchDialog,
        uiUtils) {
    var RelationshipArrayView = AbstractView.extend({
        template: "templates/admin/resource/RelationshipArrayViewTemplate.html",
        noBaseTemplate: true,
        model: {},

        events: {
            "click .reload-grid-btn": "reloadGrid",
            "click .add-relationship-btn": "addRelationship",
            "click .remove-relationships-btn": "removeRelationships",
            "click .clear-filters-btn": "clearFilters"
        },

        clearFilters: function(event){
            if (event) {
                event.preventDefault();
            }
            event.preventDefault();
            this.render(this.args);
            this.$el.find('.clear-filters-btn').prop('disabled', true);
        },
        addRelationship: function (e) {
            if (e) {
                e.preventDefault();
            }
            
            this.openResourceCollectionDialog();
        },
        removeRelationships: function (e) {
            if (e) {
                e.preventDefault();
            }
            
            uiUtils.confirmDialog($.t("templates.admin.ResourceEdit.confirmDeleteSelected" ,{ objectTitle: this.data.prop.title }),  "danger", _.bind(function(){
                var promArr = [];
                
                _.each(this.data.selectedItems, _.bind(function(relationship) {
                    promArr.push(this.deleteRelationship(relationship));
                }, this));
                
                $.when.apply($,promArr).then(_.bind(function(proms){
                    this.reloadGrid(null, _.bind(function() {
                        messagesManager.messages.addMessage({"message": $.t("templates.admin.ResourceEdit.deleteSelectedSuccess",{ objectTitle: this.data.prop.title })});
                    },this));
                },this));
            }, this));
        },
        
        reloadGrid: function(event, callback){
            if(event) {
                event.preventDefault();
            }
            this.model.relationships.fetch();
            
            if (callback) {
                callback();
            }
        },
        getURL: function(){
            return "/" + constants.context + "/" + this.relationshipUrl;
        },
        getCols: function(){
            var _this = this,
                selectCol = {
                    name: "",
                    cell: "select-row",
                    headerCell: "select-all",
                    sortable: false,
                    editable: false
                },
                schema = this.schema,
                cols = [],
                relationshipPropName = this.data.prop.propName,
                relationshipProp = this.schema.properties[this.data.prop.propName].items;
            
            cols.push({
                "name": "details",
                "label": $.t("templates.admin.ResourceEdit.details"),
                "cell": Backgrid.Cell.extend({
                    render: function () {
                        var propertyValuePath = resourceCollectionUtils.getPropertyValuePath(this.model.attributes),
                            resourceCollectionIndex = resourceCollectionUtils.getResourceCollectionIndex(_this.schema, propertyValuePath, _this.data.prop.propName),
                            txt = resourceCollectionUtils.getDisplayText(_this.data.prop, this.model.attributes, resourceCollectionIndex);
                        
                        this.$el.html(txt);
                        
                        return this;
                    }
                }),
                sortable: false,
                editable: false
            });

            cols.push({
                "name": "type",
                "label": $.t("templates.admin.ResourceEdit.type"),
                "cell": Backgrid.Cell.extend({
                    render: function () {
                        var propertyValuePath = resourceCollectionUtils.getPropertyValuePath(this.model.attributes),
                            txt = _.map(propertyValuePath.split("/"), function (item) {
                                return item.charAt(0).toUpperCase() + item.slice(1);
                            }).join(" "),
                            relationshipProp = (_this.data.prop.items) ? _this.data.prop.items : _this.data.prop,
                            resourceCollection = _.findWhere(relationshipProp.resourceCollection,{ path: propertyValuePath });
                        
                        if (resourceCollection && resourceCollection.label) {
                            txt = resourceCollection.label;
                        }
                        
                        this.$el.html(txt);
                        
                        return this;
                    }
                }),
                sortable: false,
                editable: false
            });
            
            _.each(relationshipProp.properties._refProperties.properties, _.bind(function(col,colName){
                if(colName !== "_id"){
                    cols.push(
                            {
                                "name": "/_refProperties/" + colName,
                                "label": col.title || col.label || colName,
                                "headerCell": BackgridUtils.FilterHeaderCell,
                                "cell": "string",
                                "sortable": true,
                                "editable": false,
                                "sortType": "toggle"
                            }
                    );
                }
            }, this));
            
            cols.unshift(selectCol);

            return cols;
        },
        toggleActions: function() {
            if(this.data.selectedItems.length === 0) {
                this.$el.find('.remove-relationships-btn').prop('disabled',true);
            } else {
                this.$el.find('.remove-relationships-btn').prop('disabled',false);
            }
        },

        render: function(args, callback) {
            this.args = args;
            this.element = args.element;
            this.relationshipUrl = args.prop.relationshipUrl;
            this.schema = args.schema;
            this.data.prop = args.prop;
            this.data.addResource = $.t("templates.admin.ResourceEdit.addResource", { resource: args.prop.title });
            this.data.removeResource = $.t("templates.admin.ResourceEdit.removeSelectedResource", { resource: args.prop.title });
            this.data.grid_id = "relationshipArray-" + args.prop.propName;
            this.grid_id_selector = "#" + this.data.grid_id;
            this.data.selectedItems = [];

            this.parentRender(function() {
                
                this.buildRelationshipArrayGrid(this.getCols());

                if(callback) {
                    callback();
                }
            });
        },
        buildRelationshipArrayGrid: function (cols) {
            var _this = this,
                grid_id = this.grid_id_selector,
                url = this.getURL(),
                pager_id = grid_id + '-paginator',
                RelationshipCollection = AbstractCollection.extend({
                    url: url,
                    state: BackgridUtils.getState(cols[1].name),
                    queryParams: BackgridUtils.getQueryParams({
                        _queryFilter: 'true',
                        _fields: ''
                    })
                }),
                relationshipGrid,
                paginator;
            
            this.model.relationships = new RelationshipCollection();
            
            relationshipGrid = new Backgrid.Grid({
                className: "backgrid table table-hover",
                emptyText: $.t("templates.admin.ResourceList.noData"),
                columns: BackgridUtils.addSmallScreenCell(cols),
                collection: _this.model.relationships,
                row: BackgridUtils.ClickableRow.extend({
                    callback: function(e) {
                        var $target = $(e.target);

                        if ($target.is("input") || $target.is(".select-row-cell")) {
                            return;
                        }
                        
                        _this.openResourceCollectionDialog(this.model.attributes);
                    }
                })
            });

            paginator = new Backgrid.Extension.Paginator({
                collection: this.model.relationships,
                windowSize: 0
            });

            this.$el.find(grid_id).append(relationshipGrid.render().el);
            this.$el.find(pager_id).append(paginator.render().el);
            this.bindDefaultHandlers();

            this.model.relationships.getFirstPage();
        },

        onRowSelect: function (model, selected) {
            if (selected) {
                if (!_.contains(this.data.selectedItems, model.attributes)) {
                    this.data.selectedItems.push(model.attributes);
                }
            } else {
                this.data.selectedItems = _.without(this.data.selectedItems, model.id);
            }
            this.toggleActions();
            
        },

        bindDefaultHandlers: function () {
            var _this = this;
            
            this.model.relationships.on("backgrid:selected", _.bind(function (model, selected) {
                this.onRowSelect(model, selected);
            }, this));

            this.model.relationships.on("backgrid:sort", BackgridUtils.doubleSortFix);
        },
        
        deleteRelationship: function (value) {
            var deleteUrl = "/" + constants.context + "/" + this.data.prop.relationshipUrl;
            
            return resourceDelegate.deleteResource(deleteUrl, value._refProperties._id);
        },
        createRelationship: function (value) {
            var newVal = {
                    _ref: value._ref,
                    _refProperties: _.omit(value._refProperties,"_id","_rev")
                },
                patchUrl = "/" + constants.context + "/",
                patchUrlArray = this.data.prop.relationshipUrl.split("/");
            
            patchUrlArray.pop();
            
            patchUrl += patchUrlArray.join("/");
            
            return resourceDelegate.serviceCall({
                serviceUrl: patchUrl,
                url: "", 
                type: "PATCH",
                data: JSON.stringify([{
                    "operation": "add",
                    "field": "/" + this.data.prop.propName + "/-",
                    "value": newVal
                }])
            });
        },
        openResourceCollectionDialog: function (propertyValue) {
            var _this = this,
                opts = {
                property: _this.data.prop,
                propertyValue: propertyValue,
                schema: _this.schema
            };
            
            if (!propertyValue) {
                opts.onChange = function (value, newText) {
                    _this.createRelationship(value).then(function () {
                        _this.render(_this.args);
                    });
                };
            } else {
                opts.onChange = function (value, newText) {
                    _this.deleteRelationship(value).then(function () {
                        _this.createRelationship(value).then(function () {
                            _this.render(_this.args);
                        });
                    });
                };
            }
            
            resourceCollectionSearchDialog.render(opts);
        }
    });

    return RelationshipArrayView;
});

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
 * Copyright 2011-2016 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/common/resource/ListResourceView", [
    "jquery",
    "underscore",
    "handlebars",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "backgrid",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractModel",
     "org/forgerock/commons/ui/common/main/AbstractCollection",
    "org/forgerock/openidm/ui/common/resource/ResourceCollection",
    "backgrid-paginator",
    "backgrid-selectall"
], function($,
            _,
            Handlebars,
            AbstractView,
            eventManager,
            constants,
            cookieHelper,
            uiUtils,
            Backgrid,
            BackgridUtils,
            resourceDelegate,
            messagesManager,
            AbstractModel,
            AbstractCollection,
            ResourceCollection
        ) {
        var ListResourceView = AbstractView.extend({
            template: "templates/admin/resource/ListResourceViewTemplate.html",
            model: {},
            events: {
                "click #reloadGridBtn": "reloadGrid",
                "click #clearFiltersBtn": "clearFilters",
                "click #deleteSelected": "deleteSelected"
            },

            select: function(event) {
                event.stopPropagation();
            },

            reloadGrid: function(event){
                if(event) {
                    event.preventDefault();
                }
                this.render(this.data.args);
            },

            clearFilters: function(event){
                if (event) {
                    event.preventDefault();
                }
                event.preventDefault();
                this.render(this.data.args);
                this.$el.find('#clearFiltersBtn').prop('disabled', true);
            },
            getURL: function(){
                return "/" + constants.context + "/" + this.data.objectType + "/" + this.data.objectName;
            },
            getCols: function(){
                var prom = $.Deferred(),
                    setCols,
                    selectCol = {
                        name: "",
                        cell: "select-row",
                        headerCell: "select-all",
                        sortable: false,
                        editable: false
                    };

                $.when(resourceDelegate.getSchema(this.data.args)).then(_.bind(function(schema){
                    var cols = [],
                        unorderedCols = [];

                    this.schema = schema;

                    if(schema !== "invalidObject"){
                        this.data.validObject = true;
                        if(schema){
                            this.data.pageTitle = this.data.objectName;
                            if (schema.title && !this.data.isSystemResource) {
                                this.data.pageTitle = schema.title;
                            }

                            setCols = _.bind(function(properties, parentProp) {
                                _.each(properties, _.bind(function(col,colName){
                                    if(col.type === "object") {
                                        setCols(col.properties, colName);
                                    } else {
                                        if(col.searchable || this.data.isSystemResource){
                                            //if _id is in the schema properties and is searchable add it
                                            if(colName === "_id") {

                                                unorderedCols.push(
                                                    {
                                                        "name":"_id",
                                                        "key": true,
                                                        "label": col.title || colName,
                                                        "headerCell": BackgridUtils.FilterHeaderCell,
                                                        "cell": "string",
                                                        "sortable": true,
                                                        "editable": false,
                                                        "sortType": "toggle",
                                                        "minimumSearchChars": 2
                                                    }
                                                );
                                            } else {
                                                if(parentProp) {
                                                    colName = parentProp + "/" + colName;
                                                }
                                                unorderedCols.push(
                                                    {
                                                        "name": colName,
                                                        "label": col.title || colName,
                                                        "headerCell": BackgridUtils.FilterHeaderCell,
                                                        "cell": BackgridUtils.escapedStringCell(colName),
                                                        "sortable": true,
                                                        "editable": false,
                                                        "sortType": "toggle",
                                                        "minimumSearchChars": 2
                                                    }
                                                );
                                            }
                                        }
                                    }
                                }, this));
                            }, this);

                            setCols(schema.properties);

                            _.each(schema.order,function(prop){
                                var col = _.findWhere(unorderedCols, { name : prop });

                                if(col){
                                    cols.push(col);
                                }
                            });

                            _.each(_.difference(unorderedCols, cols), function(col) {
                                cols.push(col);
                            });

                            cols.unshift(selectCol);

                            if (cols.length === 1) {
                                prom.resolve(unorderedCols);
                            } else {
                                prom.resolve(cols);
                            }
                        } else {
                            this.data.pageTitle = this.data.objectName;
                            $.get(this.getURL() + '?_queryFilter=true&_pageSize=1').then(function(qry){
                                if(qry.result[0]){
                                    _.each(_.keys(qry.result[0]),function(col){
                                        if(col !== "_id"){
                                            cols.push(
                                                {
                                                    "name": col,
                                                    "label": col,
                                                    "headerCell": BackgridUtils.FilterHeaderCell,
                                                    "cell": "string",
                                                    "sortable": true,
                                                    "editable": false,
                                                    "sortType": "toggle",
                                                    "minimumSearchChars": 2
                                                }
                                            );
                                        }
                                    });
                                }

                                cols.unshift(selectCol);

                                prom.resolve(cols);
                            });
                        }
                    } else {
                        this.data.validObject = false;
                        prom.resolve(cols);
                    }
                },this));
                return prom;
            },
            objectNameClean: function() {
                return this.data.objectName.replace("/","_");
            },
            toggleDeleteSelected: function() {
                if(this.data.selectedItems.length === 0) {
                    this.$el.find('#deleteSelected').prop('disabled',true);
                } else {
                    this.$el.find('#deleteSelected').prop('disabled',false);
                }
            },
            deleteSelected: function(e) {
                e.preventDefault();

                uiUtils.confirmDialog($.t("templates.admin.ResourceEdit.confirmDeleteSelected"),  "danger", _.bind(function(){
                    var promArr = [];
                    _.each(this.data.selectedItems, _.bind(function(objectId) {
                        promArr.push(resourceDelegate.deleteResource(this.data.serviceUrl, objectId, null));
                    }, this));
                    $.when.apply($,promArr).then(_.bind(function(proms){
                        this.reloadGrid();
                        messagesManager.messages.addMessage({"message": $.t("templates.admin.ResourceEdit.deleteSelectedSuccess")});
                    },this));
                }, this));
            },

            render: function(args, callback) {
                this.data.args = args;
                this.data.addLinkHref = "#resource/" + args[0] + "/" + args[1] + "/add/";
                this.data.objectType = args[0];
                this.data.objectName = args[1];
                this.data.grid_id = args[0] + "ViewTable";
                this.grid_id_selector = "#" + this.data.grid_id;
                this.data.isSystemResource = false;
                this.data.serviceUrl = resourceDelegate.getServiceUrl(args);
                this.data.selectedItems = [];

                if (this.data.objectType === "system") {
                    this.data.isSystemResource = true;
                    this.data.objectName += "/" + args[2];
                    this.data.addLinkHref = "#resource/" + args[0] + "/" + args[1] + "/" + args[2] + "/add/";
                }

                this.getCols().then(_.bind(function(cols){
                    this.parentRender(function() {

                        this.buildResourceListGrid(cols);

                        if(callback) {
                            callback();
                        }
                    });
                },this));
            },
            buildResourceListGrid: function (cols) {
                var _this = this,
                    grid_id = this.grid_id_selector,
                    url = this.getURL(),
                    pager_id = grid_id + '-paginator',
                    ResourceModel = AbstractModel.extend({ "url": url }),
                    resourceGrid,
                    paginator,
                    state,
                    defaultSystemResourceSortKey;

                if (this.data.isSystemResource) {
                    _.map(this.schema.properties, function (val,key) {
                        if (!defaultSystemResourceSortKey && val.type === "string") {
                            defaultSystemResourceSortKey = key;
                        }
                    });

                    if (!defaultSystemResourceSortKey) {
                        defaultSystemResourceSortKey = cols[2].name;
                    }
                }

                if(cols.length !== 0) {
                    if (defaultSystemResourceSortKey) {
                        state = BackgridUtils.getState(defaultSystemResourceSortKey);
                    } else {
                        state = BackgridUtils.getState(cols[1].name);
                    }
                } else {
                    state = null;
                }

                this.model.resources = new ResourceCollection([], {
                    url: url,
                    state: state,
                    _queryFilter: 'true',
                    isSystemResource: this.data.isSystemResource
                });

                resourceGrid = new Backgrid.Grid({
                    className: "backgrid table table-hover",
                    emptyText: $.t("templates.admin.ResourceList.noData"),
                    columns: BackgridUtils.addSmallScreenCell(cols),
                    collection: _this.model.resources,
                    row: BackgridUtils.ClickableRow.extend({
                        callback: function(e) {
                            var $target = $(e.target),
                                args = _this.data.args,
                                routeName;

                            if ($target.is("input") || $target.is(".select-row-cell")) {
                                return;
                            }
                            routeName = (!_this.data.isSystemResource) ? "adminEditManagedObjectView" : "adminEditSystemObjectView";

                            args.push(this.model.id);

                            if(this.model.id) {
                                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: routeName, args: args});
                            }
                        }
                    })
                });

                paginator = new Backgrid.Extension.Paginator({
                    collection: this.model.resources,
                    goBackFirstOnSort: false,
                    windowSize: 0
                });

                this.$el.find(grid_id).append(resourceGrid.render().el);
                this.$el.find(pager_id).append(paginator.render().el);
                this.bindDefaultHandlers();

                this.model.resources.getFirstPage();
            },

            onRowSelect: function (model, selected) {
                if (selected) {
                    if (!_.contains(this.data.selectedItems, model.id)) {
                        this.data.selectedItems.push(model.id);
                    }
                } else {
                    this.data.selectedItems = _.without(this.data.selectedItems, model.id);
                }
                this.toggleDeleteSelected();

            },

            bindDefaultHandlers: function () {
                var _this = this;

                this.model.resources.on("backgrid:selected", _.bind(function (model, selected) {
                    this.onRowSelect(model, selected);
                }, this));

                this.model.resources.on("backgrid:sort", BackgridUtils.doubleSortFix);

                this.model.resources.on("sync", function (collection) {
                    var hasFilters = false;
                    _.each(collection.state.filters, function (filter) {
                        if (filter.value.length) {
                            hasFilters = true;
                        }
                    });

                    if (hasFilters) {
                        _this.$el.find('#clearFiltersBtn').prop('disabled', false);
                    } else {
                        _this.$el.find('#clearFiltersBtn').prop('disabled', true);
                    }
                });
            }
        });

        return new ListResourceView();
    });

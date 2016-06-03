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
 * Copyright 2011-2015 ForgeRock AS.
 */

/*global define, sessionStorage */

define("org/forgerock/openidm/ui/common/resource/RelationshipArrayView", [
    "jquery",
    "underscore",
    "handlebars",
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
    "d3",
    "backgrid-paginator",
    "backgrid-selectall"
], function(
        $,
        _,
        Handlebars,
        AbstractView,
        constants,
        Backgrid,
        BackgridUtils,
        resourceDelegate,
        messagesManager,
        AbstractCollection,
        resourceCollectionUtils,
        ResourceCollectionSearchDialog,
        uiUtils,
        d3) {
    var RelationshipArrayView = AbstractView.extend({
        template: "templates/admin/resource/RelationshipArrayViewTemplate.html",
        noBaseTemplate: true,
        model: {},

        events: {
            "click .reload-grid-btn": "reloadGrid",
            "click .add-relationship-btn": "addRelationship",
            "click .remove-relationships-btn": "removeRelationships",
            "click .clear-filters-btn": "clearFilters",
            "click .toggle-chart": "toggleChart"
        },

        toggleChart: function(event){
            if (event) {
                event.preventDefault();
            }

            if (this.data.showChart) {
                this.data.showChart = false;

                this.$el.find('.relationshipListContainer').show();
                this.$el.find('.relationshipGraphContainer').hide();
            } else {
                this.data.showChart = true;

                this.$el.find('.relationshipListContainer').hide();
                this.$el.find('.relationshipGraphContainer').show();
            }
        },

        clearFilters: function(event){
            if (event) {
                event.preventDefault();
            }

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

            uiUtils.confirmDialog($.t("templates.admin.ResourceEdit.confirmDeleteSelected"),  "danger", _.bind(function(){
                var promArr = [];

                _.each(this.data.selectedItems, _.bind(function(relationship) {
                    promArr.push(this.deleteRelationship(relationship));
                }, this));

                $.when.apply($,promArr).then(_.bind(function(proms){
                    this.reloadGrid(null, _.bind(function() {
                        messagesManager.messages.addMessage({"message": $.t("templates.admin.ResourceEdit.deleteSelectedSuccess")});
                    },this));
                },this));
            }, this));
        },

        reloadGrid: function(event, callback){
            if(event) {
                event.preventDefault();
            }
            this.render(this.args);
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

            this.hasRefProperties = _.toArray(relationshipProp.properties._refProperties.properties).length > 1;

            cols.push({
                "name": "details",
                "label": $.t("templates.admin.ResourceEdit.details"),
                "cell": Backgrid.Cell.extend({
                    render: function () {
                        var propertyValuePath = resourceCollectionUtils.getPropertyValuePath(this.model.attributes),
                            resourceCollectionIndex = resourceCollectionUtils.getResourceCollectionIndex(_this.schema, propertyValuePath, _this.data.prop.propName),
                            txt = Handlebars.Utils.escapeExpression(resourceCollectionUtils.getDisplayText(_this.data.prop, this.model.attributes, resourceCollectionIndex)),
                            link = '<a class="resourceEditLink" href="#resource/' + propertyValuePath + '/edit/' + this.model.attributes._id + '">' + txt + '</a>';

                        if (propertyValuePath.indexOf("repo") >= 0) {
                            link = "<span class='unEditable'>" + txt + "</span>";
                        }

                        this.$el.html(link);

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

            if (this.hasRefProperties) {
                this.$el.find('.clear-filters-btn').show();
            }

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
            this.data.showChart = args.showChart || false;

            this.parentRender(function() {

                this.buildRelationshipArrayGrid(this.getCols(), args.onGridChange).then(function () {
                    if(callback) {
                        callback();
                    }
                });
            });
        },
        buildRelationshipArrayGrid: function (cols, onGridChange) {
            var _this = this,
                grid_id = this.grid_id_selector,
                url = this.getURL(),
                pager_id = grid_id + '-paginator',
                RelationshipCollection = AbstractCollection.extend({
                    url: url,
                    state: BackgridUtils.getState("_id"),
                    queryParams: BackgridUtils.getQueryParams({
                        _queryFilter: 'true',
                        _fields: ''
                    })
                }),
                relationshipGrid,
                paginator;

            this.model.relationships = new RelationshipCollection();

            this.model.relationships.on('sync', function(){
                if (onGridChange) {
                    onGridChange();
                }
            });

            relationshipGrid = new Backgrid.Grid({
                className: "backgrid table table-hover",
                emptyText: $.t("templates.admin.ResourceList.noData"),
                columns: BackgridUtils.addSmallScreenCell(cols),
                collection: _this.model.relationships,
                row: BackgridUtils.ClickableRow.extend({
                    callback: function(e) {
                        var $target = $(e.target),
                            isInternal = this.model.attributes._ref.indexOf("repo/internal") === 0;

                        if (isInternal && !$target.is("input")) {
                            e.preventDefault();
                        }

                        if ($target.is("input") || $target.is(".select-row-cell") || $target.hasClass("resourceEditLink") || $target.is(".unEditable")) {
                            return;
                        }

                        if (_this.hasRefProperties || isInternal) {
                            _this.openResourceCollectionDialog(this.model.attributes);
                        } else {
                            location.href = $target.closest("tr").find(".resourceEditLink").attr("href");
                        }
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

            return this.model.relationships.getFirstPage();

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

            this.model.relationships.on("sync", _.bind(function (collection) {
                var hasFilters = false;
                _.each(collection.state.filters, function (filter) {
                    if (filter.value.length) {
                        hasFilters = true;
                    }
                });

                if (hasFilters) {
                    this.$el.find('.clear-filters-btn').prop('disabled', false);
                } else {
                    this.$el.find('.clear-filters-btn').prop('disabled', true);
                }

                if (collection.models.length) {
                    this.loadChart(collection.models);
                    this.$el.find(".toggle-chart-buttons").show();
                } else {
                    this.data.showChart = false;
                    this.$el.find(".toggle-chart-buttons").hide();
                }
            }, this));
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
                url: "/" + this.data.prop.propName + "?_action=create",
                type: "POST",
                data: JSON.stringify(newVal),
                errorsHandlers : {
                        "error": {
                            status: "400"
                        }
                    },
                error: function (e) {
                    if (e.status === 400 && e.responseJSON.message.indexOf("conflict with existing") > -1) {
                        messagesManager.messages.addMessage({ "type": "error", "message": $.t("templates.admin.ResourceEdit.conflictWithExistingRelationship") });
                    } else {
                        messagesManager.messages.addMessage({ "type": "error", "message": $.t("config.messages.CommonMessages.badRequestError") });
                    }
                }
            });
        },
        updateRelationship: function (value, oldValue) {
            var newVal = _.pick(value,"_ref","_refProperties"),
                oldVal = _.pick(oldValue,"_ref","_refProperties"),
                patchUrl = "/" + constants.context + "/" + this.data.prop.relationshipUrl;

            //make sure there is actually a change before updating
            if (_.isEqual(newVal,oldVal)) {
                return $.Deferred().resolve();
            } else {
                return resourceDelegate.patchResourceDifferences(patchUrl, {id: value._refProperties._id, rev: value._refProperties._rev}, oldVal, newVal);
            }
        },
        openResourceCollectionDialog: function (propertyValue) {
            var _this = this,
                opts = {
                property: _this.data.prop,
                propertyValue: propertyValue,
                schema: _this.schema
            };

            if (!propertyValue) {
                opts.onChange = function (value, oldValue, newText) {
                    _this.createRelationship(value).then(function () {
                        _this.args.showChart = _this.data.showChart;
                        _this.render(_this.args);
                    });
                };
            } else {
                opts.onChange = function (value, oldValue, newText) {
                    _this.updateRelationship(value, oldValue).then(function () {
                        _this.render(_this.args);
                    });
                };
            }

            new ResourceCollectionSearchDialog().render(opts);
        },
        loadChart: function(models) {
            this.$el.find("#relationshipGraphBody-" + this.data.prop.propName).empty();
            var elementSelector = "#relationshipGraphBody-" + this.data.prop.propName,
                treeData = {
                    "name" : this.data.prop.parentDisplayText,
                    "parent" : "null",
                    "children" : [

                    ]
                },
                margin = {
                    top: 20,
                    right: 120,
                    bottom: 20,
                    left: 350
                },
                width = 1024 - margin.right - margin.left,
                height = 500 - margin.top - margin.bottom,
                i = 0,
                tree = d3.layout.tree().size([height, width]),
                diagonal = d3.svg.diagonal().projection(function(d) {
                    return [d.y, d.x];
                }),
                svg = d3.select(elementSelector).append("svg")
                    .attr("width", width + margin.right + margin.left)
                    .attr("height", height + margin.top + margin.bottom)
                    .append("g")
                    .attr("transform", "translate(" + margin.left + "," + margin.top + ")"),
                root = null,
                update = function(source) {
                    var nodes = tree.nodes(root).reverse(),
                        links = tree.links(nodes),
                        nodeEnter,
                        node,
                        link;

                    //Normalize for fixed-depth.
                    nodes.forEach(function(data) { data.y = data.depth * 180; });

                    //Declare the nodes
                    node = svg.selectAll("g.node").data(nodes, function(data) {
                        if(!data.id) {
                            data.id = ++i;
                        }

                        return data.id;
                    });

                    //Enter the nodes.
                    nodeEnter = node.enter().append("g")
                        .attr("class", "node")
                        .attr("transform", function(data) {
                            return "translate(" + data.y + "," + data.x + ")";
                        });

                    //Add Circles
                    nodeEnter.append("circle")
                        .attr("r", 10)
                        .style("fill", "#fff");

                    //Add Text
                    nodeEnter.append("svg:a")
                        .attr("xlink:href", function(data){return data.url;})
                        .append("text")
                        .attr("x", function(data) {
                            return data.children || data._children ? -13 : 13;
                        })
                        .attr("dy", ".35em")
                        .attr("text-anchor", function(data) {
                            return data.children || data._children ? "end" : "start";
                        })
                        .text(function(data) { return data.name; })
                        .style("fill-opacity", 1);

                    //Generate the paths
                    link = svg.selectAll("path.link").data(links, function(d) {
                        return d.target.id;
                    });

                    //Add the paths
                    link.enter().insert("path", "g")
                        .attr("class", "link")
                        .attr("d", diagonal);
                };

            _.each(models, _.bind(function(model){
                var propertyValuePath = resourceCollectionUtils.getPropertyValuePath(model.attributes),
                    resourceCollectionIndex = resourceCollectionUtils.getResourceCollectionIndex(this.schema, propertyValuePath, this.data.prop.propName),
                    displayText = resourceCollectionUtils.getDisplayText(this.data.prop, model.attributes, resourceCollectionIndex);

                treeData.children.push({
                    "name" : displayText,
                    "parent" : "null",
                    "url" : "#resource/" + propertyValuePath + "/edit/" + model.attributes._id
                });
            }, this));

            root = treeData;

            update(root);
        }
    });

    return RelationshipArrayView;
});

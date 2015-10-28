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

define("org/forgerock/openidm/ui/admin/role/RoleUsersView", [
    "jquery",
    "underscore",
    "handlebars",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "backgrid",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractModel",
    "org/forgerock/commons/ui/common/main/AbstractCollection",
    "backgrid-paginator",
    "backgrid-selectall"
], function(
        $, 
        _, 
        Handlebars, 
        AbstractView, 
        eventManager, 
        constants, 
        cookieHelper, 
        Backgrid, 
        BackgridUtils,
        resourceDelegate, 
        messagesManager,
        AbstractModel,
        AbstractCollection) {
    var RoleUsersView = AbstractView.extend({
        element: "#role-users",
        template: "templates/admin/role/RoleUsersViewTemplate.html",
        noBaseTemplate: true,
        model: {},

        events: {
            "click .actionBtn": "performAction"
        },

        reloadGrid: function(event){
            if(event) {
                event.preventDefault();
            }
            this.model.users.fetch();
        },
        getURL: function(){
            return "/" + constants.context + "/managed/user";
        },
        getCols: function(){
            var _this = this,
                prom = $.Deferred(),
                args = _.clone(this.data.args,true),
                selectCol = {
                    name: "",
                    cell: "select-row",
                    headerCell: "select-all",
                    sortable: false,
                    editable: false
                };

            args[1] = "user";

            this.data.serviceUrl = resourceDelegate.getServiceUrl(args);

            resourceDelegate.getSchema(args).then(_.bind(function(schema){
                var cols = [],
                    unorderedCols = [];

                cols.push({
                    "name": "hasRole",
                    "label": "Has Role",
                    "cell": Backgrid.Cell.extend({
                        className: "checkMarkCell",
                        render: function () {
                            if(_.indexOf(this.model.get("roles"), "managed/role/" + _this.data.roleId) > -1) {
                                this.$el.html('<i class="fa fa-check"></i>');
                            } else {
                                this.$el.html("");
                            }
                            return this;
                        }
                    }),
                    sortable: false,
                    editable: false
                });

                if(schema){
                    _.each(schema.properties, _.bind(function(col,colName){
                        if(col.searchable){
                            unorderedCols.push(
                                    {
                                        "name": colName,
                                        "label": col.title || colName,
                                        "headerCell": BackgridUtils.FilterHeaderCell,
                                        "cell": "string",
                                        "sortable": true,
                                        "editable": false,
                                        "sortType": "toggle"
                                    }
                            );
                        }
                    }, this));

                    _.each(schema.order,function(prop){
                        var col = _.findWhere(unorderedCols, { name : prop });

                        if(col){
                            cols.push(col);
                        }
                    });
                    
                    cols.unshift(selectCol);
                    
                    if (cols.length === 1) {
                        prom.resolve(unorderedCols);
                    } else {
                        prom.resolve(cols);
                    }
                } else {
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
                                                "sortType": "toggle"
                                            }
                                    );
                                }
                            });
                        }

                        prom.resolve(cols);
                    });
                }
            },this));

            return prom;
        },
        toggleActions: function() {
            if(this.data.selectedItems.length === 0) {
                this.$el.find('.actionBtn').prop('disabled',true);
            } else {
                this.$el.find('.actionBtn').prop('disabled',false);
            }
        },
        performAction: function(e) {
            var action = $(e.target).attr("action"),
                promArr = [],
                successMsg;

            e.preventDefault();

            _.each(this.data.selectedItems, _.bind(function(objectId) {
                var rowdata = _.where(this.model.users.models,{ id: objectId })[0],
                    currentRole = "managed/role/" + this.data.roleId,
                    roles = rowdata.get("roles"),
                    hasRole = _.indexOf(roles, currentRole) > -1,
                    doUpdate = false;

                if(action === "remove") {
                    roles = _.reject(roles,function(role) { return role === currentRole; });
                    doUpdate = hasRole;
                    successMsg = $.t("templates.admin.RoleUsersTemplate.removeSelectedSuccess",{ roleId: this.data.role.properties.name });
                } else {
                    roles.push(currentRole);
                    doUpdate = !hasRole;
                    successMsg = $.t("templates.admin.RoleUsersTemplate.addSelectedSuccess",{ roleId: this.data.role.properties.name });
                }

                if(doUpdate) {
                    rowdata.set("roles",roles);
                    promArr.push(rowdata.save());
                }
            },this));

            $.when.apply($,promArr).then(_.bind(function(){
                this.render(this.data.args, this.data.role, _.bind(function() {
                    messagesManager.messages.addMessage({"message": successMsg});
                },this));
            },this));
        },

        render: function(args, role, callback) {
            var _this = this;
            this.data.args = args;
            this.data.roleId = args[2];
            this.data.role = role;
            this.data.grid_id = "roleUsersViewTable";
            this.grid_id_selector = "#" + this.data.grid_id;
            this.data.selectedItems = [];

            this.getCols().then(_.bind(function(cols){
                this.parentRender(function() {
                    
                    this.buildUserGrid(cols);

                    if(callback) {
                        callback();
                    }
                });
            },this));
        },
        buildUserGrid: function (cols) {
            var _this = this,
                grid_id = this.grid_id_selector,
                url = this.getURL(),
                pager_id = grid_id + '-paginator',
                UserModel = AbstractModel.extend({ "url": url }),
                UserCollection = AbstractCollection.extend({
                    url: url,
                    model: UserModel,
                    state: BackgridUtils.getState(cols[1].name),
                    queryParams: BackgridUtils.getQueryParams({
                        _queryFilter: 'true'
                    })
                }),
                userGrid,
                paginator;
            
            this.model.users = new UserCollection();
            
            userGrid = new Backgrid.Grid({
                className: "backgrid table table-hover",
                emptyText: $.t("templates.admin.ResourceList.noData"),
                columns: BackgridUtils.addSmallScreenCell(cols),
                collection: _this.model.users,
                row: BackgridUtils.ClickableRow.extend({
                    callback: function(e) {
                        var $target = $(e.target),
                            args = _this.data.args,
                            routeName;

                        if ($target.is("input") || $target.is(".select-row-cell")) {
                            return;
                        }
                        routeName = (!this.isSystemResource) ? "adminEditManagedObjectView" : "adminEditSystemObjectView";

                    args.push(this.model.id);

                    if(this.model.id) {
                        eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: routeName, args: args});
                    }
                }
                })
            });

            paginator = new Backgrid.Extension.Paginator({
                collection: this.model.users,
                windowSize: 0
            });

            this.$el.find(grid_id).append(userGrid.render().el);
            this.$el.find(pager_id).append(paginator.render().el);
            this.bindDefaultHandlers();

            this.model.users.getFirstPage();
        },

        onRowSelect: function (model, selected) {
            if (selected) {
                if (!_.contains(this.data.selectedItems, model.id)) {
                    this.data.selectedItems.push(model.id);
                }
            } else {
                this.data.selectedItems = _.without(this.data.selectedItems, model.id);
            }
            this.toggleActions();
            
        },

        bindDefaultHandlers: function () {
            var _this = this;
            
            this.model.users.on("backgrid:selected", _.bind(function (model, selected) {
                this.onRowSelect(model, selected);
            }, this));

            this.model.users.on("backgrid:sort", BackgridUtils.doubleSortFix);
        }
    });

    return new RoleUsersView();
});

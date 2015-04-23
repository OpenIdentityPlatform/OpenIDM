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

/*global define, $, form2js, _, Handlebars, sessionStorage */

/**
 * @author huck.elliott
 */
define("org/forgerock/openidm/ui/admin/role/RoleUsersView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/commons/ui/common/components/Messages"
], function(AbstractView, eventManager, constants, cookieHelper, uiUtils, resourceDelegate, messagesManager) {
    var RoleUsersView = AbstractView.extend({
        element: "#role-users",
        template: "templates/admin/role/RoleUsersViewTemplate.html",
        noBaseTemplate: true,
        
        events: {
            "click .actionBtn": "performAction"
        },
        
        reloadGrid: function(event){
            event.preventDefault();
            $(this.grid_id_selector).trigger('reloadGrid');
        },
        getURL: function(){
            return "/" + constants.context + "/managed/user";
        },
        getCols: function(){
            var prom = $.Deferred(),
                args = _.clone(this.data.args,true);
            
            args[1] = "user";
            
            this.data.serviceUrl = resourceDelegate.getServiceUrl(args);
            
            resourceDelegate.getSchema(args).then(_.bind(function(schema){
                var cols = [],
                    unorderedCols = [];
                
                cols.push({
                    "name": "hasRole",
                    "label": "Has Role",
                    "width": 100,
                    "search": false,
                    "sortable": false,
                    "align": "center"
                });

                if(schema){
                    _.each(schema.properties, _.bind(function(col,colName){
                        if(col.searchable || colName === "roles"){
                            unorderedCols.push(
                                    {
                                        "name": colName,
                                        "label": col.title || colName,
                                        "formatter": Handlebars.Utils.escapeExpression,
                                        "hidden": colName === "roles"
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
                                                "formatter": Handlebars.Utils.escapeExpression,
                                                "hidden": col === "_rev"
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
        getTotal: function(){
            var prom = $.Deferred();
            
            //$.get(this.getURL() + '?_queryId=get-users-of-direct-role&role=' + this.data.roleId).then(
            $.get(this.getURL() + '?_queryId=query-all-ids').then(
                function(qry){
                    prom.resolve(qry);
                },
                function(){
                    prom.resolve({ resultCount: 0 });
                }
            );
            
            return prom;
        },
        selectedRows: function() {
            return this.$el.find(this.grid_id_selector).jqGrid('getGridParam','selarrrow');
        },
        toggleActions: function() {
            if(this.selectedRows().length === 0) {
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
            
            _.each(this.selectedRows(), _.bind(function(objectId) {
                var rowdata = _.where(this.data.gridData.result,{ _id: objectId })[0],
                    currentRole = "managed/role/" + this.data.roleId,
                    hasRole = _.indexOf(rowdata.roles, currentRole) > -1,
                    doUpdate = false;
                    
                if(action === "remove") {
                    rowdata.roles = _.reject(rowdata.roles,function(role) { return role === currentRole; });
                    doUpdate = hasRole;
                    successMsg = $.t("templates.admin.RoleUsersTemplate.removeSelectedSuccess",{ roleId: this.data.role.properties.name });
                } else {
                    rowdata.roles.push(currentRole);
                    doUpdate = !hasRole;
                    successMsg = $.t("templates.admin.RoleUsersTemplate.addSelectedSuccess",{ roleId: this.data.role.properties.name });
                }
                
                if(doUpdate) {
                    promArr.push(resourceDelegate.updateResource(this.data.serviceUrl, rowdata._id, rowdata));
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

            $.when(this.getCols(), this.getTotal()).then(_.bind(function(cols, total){
                this.data.hasData = false;
                if(total.resultCount){
                    this.data.hasData = true;
                    this.parentRender(function() {
                        var _this = this,
                        grid_id = this.grid_id_selector,
                        pager_id = grid_id + '_pager',
                        rowNum = sessionStorage.getItem(this.data.grid_id + "ViewGridRows");
                        
                        uiUtils.buildJQGrid(this, this.data.grid_id, {
                            url: this.getURL(),
                            width: 920,
                            shrinkToFit: true,
                            rowList: [10,20,50],
                            rowNum: (rowNum) ? parseInt(rowNum, 10) : 10,
                            sortname: (cols[1]) ? cols[1].name : cols[0].name,
                            sortorder: 'asc',
                            colModel: cols,
                            pager: pager_id,
                            multiselect: true,
                            jsonReader : {
                                repeatitems: false,
                                root: function(obj){ return obj.result; },
                                id: "_id",
                                page: function(obj){ return _this.gridPage || 1; },
                                total: function(obj){ return Math.ceil(total.resultCount / ((rowNum) ? rowNum : 10)); },
                                records: function(obj){ return total.resultCount; }
                            },
                            beforeRequest: function(){
                                var posted_data = $(grid_id).jqGrid('getGridParam','postData');
                                _this.gridPage = posted_data.page;
                            },
                            onPaging: function(btn){
                                if(btn === "records"){
                                    var rows = $('.ui-pg-selbox').val();
                                    sessionStorage.setItem(_this.data.grid_id + "ViewGridRows", rows);
                                }
                            },
                            afterInsertRow: function(rowid , rowdata, rowelem) {
                                if(_.indexOf(rowdata.roles, "managed/role/" + _this.data.roleId) > -1) {
                                    $("#" + rowid +" td:eq(1)").html('<i class="fa fa-check"></i>');
                                }
                            },
                            loadComplete: _.bind(function(data) {
                                this.data.gridData = data;
                            }, this),
                            onSelectRow: _.bind(function() {
                                this.toggleActions();
                            }, this),
                            onSelectAll: _.bind(function() {
                                this.toggleActions();
                            }, this)
                        }, 
                        { 
                            search: true,
                            searchOperator: "sw",
                            suppressColumnChooser: true,
                            storageKey: this.data.grid_id,
                            serializeGridData: function(view, posted_data){
                                return '_id sw ""';
                            },
                            columnChooserOptions: { height: "auto", width: "auto" }
                        });
                        
                        if(callback) {
                            callback();
                        }
                    });
                } else {
                    this.parentRender();
                }
                
            },this));
        }   
    }); 
    
    return new RoleUsersView();
});



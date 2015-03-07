/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All rights reserved.
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
define("org/forgerock/openidm/ui/common/managed/ListManagedObjectView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/common/delegates/ManagedObjectDelegate"
], function(AbstractView, eventManager, constants, cookieHelper, uiUtils, managedObjectDelegate) {
    var hasFilters = function(){
            var search = false;
            $.each($('.ui-search-toolbar').find('input,select'),function(){
                if($(this).val().length > 0){
                    search = true;
                }
            });
            return search;
        },
        ListManagedObjectView = AbstractView.extend({
        template: "templates/admin/managed/ListManagedObjectViewTemplate.html",
        
        events: {
            "click #reloadGridBtn": "reloadGrid",
            "click #clearFiltersBtn": "clearFilters"
        },
        
        select: function(event) {
            event.stopPropagation();
        },
        
        reloadGrid: function(event){
            event.preventDefault();
            $(this.grid_id_selector).trigger('reloadGrid');
        },
        
        showObject: function(objectId) {
            if(objectId) {
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "adminEditManagedObjectView", args: [this.data.objectName,objectId]});
            }
        },
        
        clearFilters: function(event){
            var grid_id = this.grid_id_selector,
                post_data = sessionStorage.getItem(this.data.objectName + "ViewGridParams_preTranslation");
            
            if(post_data){
                post_data = JSON.parse(post_data);
            }
            
            event.preventDefault();
            
            $(grid_id).jqGrid('setGridParam',{search:false});

            $.extend(post_data, { filters: "" });

            _.each(post_data,function(v,k){
                if (k === "_search"){
                    post_data._search = false;
                }
                else if ($.inArray(k, ["nd", "sidx", "rows", "sord", "page", "filters"]) < 0) {
                    try {
                        delete post_data[k];
                    } catch (e) { }

                    $("#gs_" + $.jgrid.jqID(k), $(grid_id).get(0).grid.hDiv).val("");

                }
            });

            this.render([this.data.objectName]);
            $('#clearFiltersBtn').prop('disabled', true);
        },
        getURL: function(){
            return "/" + constants.context + "/managed/" + this.data.objectName;
        },
        getCols: function(){
            var prom = $.Deferred();
            
            $.when(managedObjectDelegate.getSchema(this.data.objectName)).then(_.bind(function(schema){
                var cols = [],
                    unorderedCols = [];
                
                cols.push(
                        {
                            "name":"_id",
                            "hidden": true,
                            "key": true
                        }
                );
                    
                if(schema !== "invalidObject"){
                    this.data.validObject = true;
                    if(schema){
                        this.data.pageTitle = schema.title || this.data.objectName;
                        _.each(schema.properties, function(col,colName){
                            if(col.searchable){
                                unorderedCols.push(
                                        {
                                            "name": colName,
                                            "label": col.title || colName,
                                            "formatter": Handlebars.Utils.escapeExpression
                                        }
                                );
                            }
                        });
                        
                        _.each(schema.order,function(prop){
                            var col = _.findWhere(unorderedCols, { name : prop });
                            
                            if(col){
                                cols.push(col);
                            }
                        });
                        prom.resolve(cols);
                    } else {
                        this.data.pageTitle = this.data.objectName;
                        $.get(this.getURL() + '?_queryFilter=_id sw ""&_pageSize=1').then(function(qry){
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
                } else {
                    this.data.validObject = false;
                    prom.resolve(cols);
                }
            },this));
            
            return prom;
        },
        getTotal: function(){
            var prom = $.Deferred();
            
            $.get(this.getURL() + '?_queryFilter=_id sw ""&_fields=none').then(
                function(qry){
                    prom.resolve(qry);
                },
                function(){
                    prom.resolve({ resultCount: 0 });
                }
            );
            
            return prom;
        },
        
        render: function(args) {
            this.data.objectName = args[0];
            this.data.grid_id = args[0] + "ViewTable";
            this.grid_id_selector = "#" + this.data.grid_id;

            $.when(this.getCols(), this.getTotal()).then(_.bind(function(cols, total){
                this.data.hasData = false;
                if(total.resultCount){
                    this.data.hasData = true;
                    this.parentRender(function() {
                        var _this = this,
                        grid_id = this.grid_id_selector,
                        pager_id = grid_id + '_pager',
                        rowNum = sessionStorage.getItem(this.data.objectName + "ViewGridRows");
                        
                        uiUtils.buildJQGrid(this, this.data.grid_id, {
                            url: this.getURL(),
                            width: 920,
                            shrinkToFit: cols.length <= 6 || false,
                            rowList: [10,20,50],
                            rowNum: (rowNum) ? parseInt(rowNum, 10) : 10,
                            sortname: cols[1].name,
                            sortorder: 'asc',
                            colModel: cols,
                            pager: pager_id,
                            onCellSelect: function(rowid,iCol,val,e){
                                var posted_data = $(grid_id).jqGrid('getGridParam','postData');
                                sessionStorage.setItem(_this.data.objectName + "ViewGridParams", JSON.stringify(posted_data));
                                
                                if(_this.data.posted_data_preTranslation){
                                    sessionStorage.setItem(_this.data.objectName + "ViewGridParams_preTranslation", JSON.stringify(_this.data.posted_data_preTranslation));
                                }
                                
                                _this.showObject(rowid);
                            },
                            jsonReader : {
                                repeatitems: false,
                                root: function(obj){ return obj.result; },
                                id: "_id",
                                page: function(obj){ return _this.gridPage || 1; },
                                total: function(obj){ return Math.ceil(total.resultCount / ((rowNum) ? rowNum : 10)); },
                                records: function(obj){ return total.resultCount; }
                            },
                            loadComplete: function(data){
                               var params = sessionStorage.getItem(_this.data.objectName + "ViewGridParams_preTranslation");
                               if(params){
                                   params = JSON.parse(params);
                                   _.each(cols, function(col){
                                       $('#gs_' + col.name).val(params[col.name]);
                                   });
                                   $(grid_id).jqGrid("sortGrid", params.sidx, false, params.sord);
                                   $('#clearFiltersBtn').prop('disabled', false);
                               }
                               if(!hasFilters()){
                                   $('#clearFiltersBtn').prop('disabled', true);
                               } 

                               
                               sessionStorage.removeItem(_this.data.objectName + "ViewGridParams_preTranslation");
                               
                               sessionStorage.removeItem(_this.data.objectName + "ViewGridParams");
                            },
                            beforeRequest: function(){
                                var posted_data = $(grid_id).jqGrid('getGridParam','postData');
                                if(posted_data._queryFilter) {
                                    _this.data.posted_data_preTranslation = _.clone(posted_data);
                                    sessionStorage.setItem(_this.data.objectName + "ViewGridParams_preTranslation", JSON.stringify(posted_data));
                                }
                                _this.gridPage = posted_data.page;
                            },
                            onPaging: function(btn){
                                if(btn === "records"){
                                    var rows = $('.ui-pg-selbox').val();
                                    sessionStorage.setItem(_this.data.objectName + "ViewGridRows", rows);
                                }
                            }
                        }, 
                        { 
                            search: true,
                            searchOperator: "sw",
                            suppressColumnChooser: true,
                            storageKey: this.data.objectName,
                            serializeGridData: function(view, posted_data){
                                var cachedParams = sessionStorage.getItem(_this.data.objectName + "ViewGridParams");
                                if(cachedParams && JSON.parse(cachedParams)._queryFilter){
                                    return JSON.parse(cachedParams)._queryFilter;
                                } else {
                                    return '_id sw ""';
                                } 
                            },
                            columnChooserOptions: { height: "auto", width: "auto" }
                        });
                    });
                } else {
                    this.parentRender();
                }
                
            },this));
        }   
    }); 
    
    return new ListManagedObjectView();
});



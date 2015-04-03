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
define("org/forgerock/openidm/ui/common/resource/ListResourceView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate"
], function(AbstractView, eventManager, constants, cookieHelper, uiUtils, resourceDelegate) {
    var ListResourceView = AbstractView.extend({
        template: "templates/admin/resource/ListResourceViewTemplate.html",
        
        events: {
            "click #reloadGridBtn": "reloadGrid",
            "click #clearFiltersBtn": "clearFilters"
        },
        
        hasFilters: function(){
            var search = false;
            $.each($('.ui-search-toolbar').find('input,select'),function(){
                if($(this).val().length > 0){
                    search = true;
                }
            });
            return search;
        },
        
        select: function(event) {
            event.stopPropagation();
        },
        
        reloadGrid: function(event){
            event.preventDefault();
            $(this.grid_id_selector).trigger('reloadGrid');
        },
        
        showObject: function(objectId) {
            var args = this.data.args,
                routeName = (this.data.objectType === "managed") ? "adminEditManagedObjectView" : "adminEditSystemObjectView";
            
            args.push(objectId);
            
            if(objectId) {
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: routeName, args: args});
            }
        },
        
        clearFilters: function(event){
            var grid_id = this.grid_id_selector,
                post_data = sessionStorage.getItem(this.data.objectName.replace("/","_") + "ViewGridParams_preTranslation");
            
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

            this.render(this.data.args);
            this.$el.find('#clearFiltersBtn').prop('disabled', true);
        },
        getURL: function(){
            return "/" + constants.context + "/" + this.data.objectType + "/" + this.data.objectName;
        },
        getCols: function(){
            var prom = $.Deferred();
            
            $.when(resourceDelegate.getSchema(this.data.args)).then(_.bind(function(schema){
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
                        this.data.pageTitle = this.data.objectName;
                        if (schema.title && this.data.objectType === "managed") {
                            this.data.pageTitle = schema.title;
                        }
                        
                        _.each(schema.properties, _.bind(function(col,colName){
                            if(col.searchable || this.data.objectType === "system"){
                                unorderedCols.push(
                                        {
                                            "name": colName,
                                            "label": col.title || colName,
                                            "formatter": Handlebars.Utils.escapeExpression
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
        
        render: function(args) {
            this.data.args = args;
            this.data.addLinkHref = "#resource/" + args[0] + "/" + args[1] + "/add/";
            this.data.objectType = args[0];
            this.data.objectName = args[1];
            this.data.grid_id = args[0] + "ViewTable";
            this.grid_id_selector = "#" + this.data.grid_id;
            
            if (this.data.objectType === "system") {
                this.data.objectName += "/" + args[2];
                this.data.addLinkHref = "#resource/" + args[0] + "/" + args[1] + "/" + args[2] + "/add/";
            }

            $.when(this.getCols(), this.getTotal()).then(_.bind(function(cols, total){
                this.data.hasData = false;
                if(total.resultCount){
                    this.data.hasData = true;
                    this.parentRender(function() {
                        var _this = this,
                        grid_id = this.grid_id_selector,
                        pager_id = grid_id + '_pager',
                        rowNum = sessionStorage.getItem(this.data.objectName.replace("/","_") + "ViewGridRows");
                        
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
                                sessionStorage.setItem(_this.data.objectName.replace("/","_") + "ViewGridParams", JSON.stringify(posted_data));
                                
                                if(_this.data.posted_data_preTranslation){
                                    sessionStorage.setItem(_this.data.objectName.replace("/","_") + "ViewGridParams_preTranslation", JSON.stringify(_this.data.posted_data_preTranslation));
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
                            loadComplete: _.bind(function(data){
                               var params = sessionStorage.getItem(_this.data.objectName.replace("/","_") + "ViewGridParams_preTranslation");
                               if(params){
                                   params = JSON.parse(params);
                                   _.each(cols, function(col){
                                       $('#gs_' + col.name).val(params[col.name]);
                                   });
                                   $(grid_id).jqGrid("sortGrid", params.sidx, false, params.sord);
                                   $('#clearFiltersBtn').prop('disabled', false);
                               }
                               if(!this.hasFilters()){
                                   $('#clearFiltersBtn').prop('disabled', true);
                               } 

                               
                               sessionStorage.removeItem(_this.data.objectName.replace("/","_") + "ViewGridParams_preTranslation");
                               
                               sessionStorage.removeItem(_this.data.objectName.replace("/","_") + "ViewGridParams");
                            }, this),
                            beforeRequest: function(){
                                var posted_data = $(grid_id).jqGrid('getGridParam','postData');
                                if(posted_data._queryFilter) {
                                    _this.data.posted_data_preTranslation = _.clone(posted_data);
                                    sessionStorage.setItem(_this.data.objectName.replace("/","_") + "ViewGridParams_preTranslation", JSON.stringify(posted_data));
                                }
                                _this.gridPage = posted_data.page;
                            },
                            onPaging: function(btn){
                                if(btn === "records"){
                                    var rows = $('.ui-pg-selbox').val();
                                    sessionStorage.setItem(_this.data.objectName.replace("/","_") + "ViewGridRows", rows);
                                }
                            }
                        }, 
                        { 
                            search: true,
                            searchOperator: "sw",
                            suppressColumnChooser: true,
                            storageKey: this.data.objectName.replace("/","_"),
                            serializeGridData: function(view, posted_data){
                                var cachedParams = sessionStorage.getItem(_this.data.objectName.replace("/","_") + "ViewGridParams");
                                if(cachedParams && JSON.parse(cachedParams)._queryFilter){
                                    return JSON.parse(cachedParams)._queryFilter;
                                } else {
                                    if(_this.data.objectType === "system") {
                                        return cols[1].name + ' sw ""';
                                    } else {
                                        return '_id sw ""';
                                    }
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
    
    return new ListResourceView();
});



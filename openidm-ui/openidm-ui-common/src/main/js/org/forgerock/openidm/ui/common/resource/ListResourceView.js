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
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/commons/ui/common/components/Messages"
], function(AbstractView, eventManager, constants, cookieHelper, uiUtils, resourceDelegate, messagesManager) {
    var ListResourceView = AbstractView.extend({
        template: "templates/admin/resource/ListResourceViewTemplate.html",
        
        events: {
            "click #reloadGridBtn": "reloadGrid",
            "click #clearFiltersBtn": "clearFilters",
            "click #deleteSelected": "deleteSelected"
        },
        
        hasFilters: function(){
            var search = false;
            $.each(this.$el.find('.ui-search-toolbar').find('input,select'),function(){
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
            if(event) {
                event.preventDefault();
            }
            $(this.grid_id_selector).trigger('reloadGrid');
        },
        
        showObject: function(objectId) {
            var args = this.data.args,
                routeName = (!this.isSystemResource) ? "adminEditManagedObjectView" : "adminEditSystemObjectView";
            
            args.push(objectId);
            
            if(objectId) {
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: routeName, args: args});
            }
        },
        
        clearFilters: function(event){
            var grid_id = this.grid_id_selector,
                post_data = sessionStorage.getItem(this.objectNameClean() + "ViewGridParams_preTranslation");
            
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
            var prom = $.Deferred(),
                setCols;
            
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
                        if (schema.title && !this.isSystemResource) {
                            this.data.pageTitle = schema.title;
                        }
                        
                        setCols = _.bind(function(properties, parentProp) {
                            _.each(properties, _.bind(function(col,colName){
                                if(col.type === "object") {
                                    setCols(col.properties, colName);
                                } else {
                                    if(col.searchable || this.isSystemResource){
                                        //if _id is in the schema properties and is searchable get rid of the default
                                        //_id col and replace it with a visible one
                                        if(colName === "_id") {
                                            cols.splice(0,1); 
                                            
                                            unorderedCols.push(
                                                    {
                                                        "name":"_id",
                                                        "key": true,
                                                        "label": col.title || colName,
                                                        "formatter": Handlebars.Utils.escapeExpression
                                                    }
                                            );
                                        } else {
                                            if(parentProp) {
                                                colName = parentProp + "." + colName;
                                            }
                                            unorderedCols.push(
                                                    {
                                                        "name": colName,
                                                        "label": col.title || colName,
                                                        "formatter": Handlebars.Utils.escapeExpression
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
        objectNameClean: function() {
            return this.data.objectName.replace("/","_");
        },
        selectedRows: function() {
            return this.$el.find(this.grid_id_selector).jqGrid('getGridParam','selarrrow');
        },
        toggleDeleteSelected: function() {
            if(this.selectedRows().length === 0) {
                this.$el.find('#deleteSelected').prop('disabled',true);
            } else {
                this.$el.find('#deleteSelected').prop('disabled',false);
            }
        },
        deleteSelected: function(e) {
            e.preventDefault();
            
            uiUtils.jqConfirm($.t("templates.admin.ResourceEdit.confirmDeleteSelected",{ objectTitle: this.data.objectName }), _.bind(function(){
                var promArr = [];
                _.each(this.selectedRows(), _.bind(function(objectId) {
                    promArr.push(resourceDelegate.deleteResource(this.data.serviceUrl, objectId, null, _.bind(function() {
                        this.reloadGrid();
                    }, this)));
                }, this));
                $.when.apply($,promArr).then(_.bind(function(proms){
                    this.render(this.data.args, _.bind(function() {
                        messagesManager.messages.addMessage({"message": $.t("templates.admin.ResourceEdit.deleteSelectedSuccess",{ objectTitle: this.data.objectName })});
                    },this));
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
            this.isSystemResource = false;
            this.data.serviceUrl = resourceDelegate.getServiceUrl(args);
            
            if (this.data.objectType === "system") {
                this.isSystemResource = true;
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
                        rowNum = sessionStorage.getItem(this.objectNameClean() + "ViewGridRows");
                        
                        uiUtils.buildJQGrid(this, this.data.grid_id, {
                            url: this.getURL(),
                            width: 920,
                            shrinkToFit: cols.length <= 6 || false,
                            rowList: [10,20,50],
                            rowNum: (rowNum) ? parseInt(rowNum, 10) : 10,
                            sortname: (cols[1] && !this.isSystemResource) ? cols[1].name : cols[0].name,
                            sortorder: 'asc',
                            colModel: cols,
                            multiselect: true,
                            pager: pager_id,
                            onCellSelect: _.bind(function(rowid,iCol,val,e){
                                if(iCol !== 0) {
                                    var posted_data = $(grid_id).jqGrid('getGridParam','postData');
                                    sessionStorage.setItem(_this.objectNameClean() + "ViewGridParams", JSON.stringify(posted_data));
                                    
                                    if(_this.data.posted_data_preTranslation){
                                        sessionStorage.setItem(_this.objectNameClean() + "ViewGridParams_preTranslation", JSON.stringify(_this.data.posted_data_preTranslation));
                                    }
                                    
                                    _this.showObject(rowid);
                                }
                            }, this),
                            jsonReader : {
                                repeatitems: false,
                                root: function(obj){ return obj.result; },
                                id: "_id",
                                page: function(obj){ return _this.gridPage || 1; },
                                total: function(obj){ return Math.ceil(total.resultCount / ((rowNum) ? rowNum : 10)); },
                                records: function(obj){ return total.resultCount; }
                            },
                            loadComplete: _.bind(function(data){
                               var params = sessionStorage.getItem(_this.objectNameClean() + "ViewGridParams_preTranslation");
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

                               
                               sessionStorage.removeItem(_this.objectNameClean() + "ViewGridParams_preTranslation");
                               
                               sessionStorage.removeItem(_this.objectNameClean() + "ViewGridParams");
                            }, this),
                            beforeRequest: function(){
                                var posted_data = $(grid_id).jqGrid('getGridParam','postData');
                                if(posted_data._queryFilter) {
                                    _this.data.posted_data_preTranslation = _.clone(posted_data);
                                    sessionStorage.setItem(_this.objectNameClean() + "ViewGridParams_preTranslation", JSON.stringify(posted_data));
                                }
                                _this.gridPage = posted_data.page;
                            },
                            onPaging: function(btn){
                                if(btn === "records"){
                                    var rows = $('.ui-pg-selbox').val();
                                    sessionStorage.setItem(_this.objectNameClean() + "ViewGridRows", rows);
                                }
                            },
                            onSelectRow: _.bind(function() {
                                this.toggleDeleteSelected();
                            }, this),
                            onSelectAll: _.bind(function() {
                                this.toggleDeleteSelected();
                            }, this)
                        }, 
                        { 
                            search: true,
                            searchOperator: "sw",
                            suppressColumnChooser: true,
                            storageKey: this.objectNameClean(),
                            serializeGridData: function(posted_data){
                                var cachedParams = sessionStorage.getItem(_this.objectNameClean() + "ViewGridParams"),
                                    omittedFields = ["_pageSize","_pagedResultsOffset","_queryFilter","_sortKeys","page","sord"],
                                    searchFields = _.omit(posted_data,omittedFields),
                                    filterArray = [];
                                
                                //convert sortKeys to json pointer
                                posted_data._sortKeys = posted_data._sortKeys.replace(".","/");
                                
                                if(cachedParams && JSON.parse(cachedParams)._queryFilter){
                                    return JSON.parse(cachedParams)._queryFilter;
                                } else {
                                    if(!_.isEmpty(searchFields)) {
                                        _.each(posted_data, function(val, key){
                                            if(_.contains(_.keys(searchFields),key)) {
                                                //convert the field name to json pointer
                                                filterArray.push('/' + key.replace(".","/") + ' sw "' + val + '"');
                                                //remove the old dot notation version
                                                delete posted_data[key];
                                            }
                                        });
                                        
                                        return filterArray.join(" AND ");
                                    } else {
                                        if(_this.isSystemResource) {
                                            return "/" + cols[1].name + ' sw ""';
                                        } else {
                                            return '/_id sw ""';
                                        }
                                    }
                                } 
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
    
    return new ListResourceView();
});



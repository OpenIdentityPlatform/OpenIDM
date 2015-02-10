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

/*global define, $, form2js, _, Handlebars */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/admin/users/UsersView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "UserDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/i18nManager",
    "org/forgerock/commons/ui/common/util/CookieHelper"
], function(AbstractView, userDelegate, eventManager, constants, i18nManager, cookieHelper) {
    var hasFilters = function(){
            var search = false;
            $.each($('.ui-search-toolbar').find('input,select'),function(){
                if($(this).val().length > 0){
                    search = true;
                }
            });
            return search;
        },
        UsersView = AbstractView.extend({
        template: "templates/admin/NewUsersTemplate.html",
        
        events: {
            "click #reloadGridBtn": "reloadGrid",
            "click #clearFiltersBtn": "clearFilters"
        },
        
        select: function(event) {
            console.log("user selected");
            event.stopPropagation();
        },
        
        reloadGrid: function(event){
            event.preventDefault();
            $('#usersTable').trigger('reloadGrid');
        },
        
        showProfile: function(userName) {
            if(userName) {
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "adminUserProfile", args: [userName]});
            }
        },
        
        clearFilters: function(event){
            var grid_id = '#usersTable',
                post_data = $(grid_id).jqGrid('getGridParam','postData');
            
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
            
            $(grid_id).trigger("reloadGrid", [{ page: 1}]);
            $('#clearFiltersBtn').prop('disabled', true);
        },
        
        render: function() {
            this.parentRender(function() {
                var _this = this,
                grid_id = 'usersTable',
                pager_id = grid_id + '_pager',
                cols = [
                        {
                            "name": "_id",
                            "hidden": true,
                            "formatter": Handlebars.Utils.escapeExpression
                        },
                        {
                            "name": "version",
                            "hidden": true,
                            "formatter": Handlebars.Utils.escapeExpression
                        },
                        {
                            "name": "userName",
                            "label": $.t("common.user.username"),
                            "key": true
                        },
                        {
                            "name": "givenName",
                            "label": $.t("common.user.givenName"),
                            "formatter": Handlebars.Utils.escapeExpression
                        },
                        {
                            "name": "sn",
                            "label": $.t("common.user.familyName"),
                            "formatter": Handlebars.Utils.escapeExpression
                        },
                        {
                            "name": "mail",
                            "label": $.t("common.user.email"),
                            "formatter": Handlebars.Utils.escapeExpression
                        },
                        {
                            "name": "accountStatus",
                            "label": $.t("common.user.status"),
                            "align": "center",
                            "stype": "select", 
                            "edittype": "select",
                            "editoptions": { "value": ":All;active:Active;inactive:Inactive"},
                            "formatter": function(val, options, row_data){
                                var val_escaped = Handlebars.Utils.escapeExpression(val),
                                    img_src = (val_escaped === 'active') ? 'fa-check' : 'fa-times',
                                    html = '<i style="padding-top:5px;cursor:pointer;" class="fa ' + img_src + '" title="' + val_escaped + '"></i>';
                                return (val_escaped && val_escaped.length > 0) ? html : '';
                            }
                        }
                ],
                rowNum = cookieHelper.getCookie("userGridRows");

                $('#' + grid_id).jqGrid('GridUnload');
                $('#' + pager_id).remove();
                
                $('#' + grid_id).after('<div id="' + pager_id + '"></div>');
                $('#' + grid_id).jqGrid( {
                    jsonReader : {
                        repeatitems: false,
                        root: function(obj){ 
                            return _.map(obj.result[0].rows, function (r) {
                                r.userName = Handlebars.Utils.escapeExpression(r.userName);
                                return r;
                            });
                        },
                        id: "userName",
                        page: function(obj){ return obj.result[0].page; },
                        total: function(obj){ return obj.result[0].total; },
                        records: function(obj){ return obj.result[0].records; }
                    },
                    prmNames: {
                        search: "search"
                    },
                    url: '/openidm/endpoint/jqgrid?resource=managed/user&_queryId=get-managed-users&formatted=false',
                    datatype: "json",
                    height: 'auto',
                    width: 920,
                    rowNum: (rowNum) ? parseInt(rowNum, 10) : 10,
                    rowList: [10,20,50],
                    pager: pager_id,
                    viewrecords: true,
                    hidegrid: false,
                    sortname: 'userName',
                    sortorder: 'asc',
                    colModel: cols,
                    loadComplete: function(data){
                        var params = cookieHelper.getCookie("userGridParams");
                        if (data.result[0].records === 0) {
                            $('#' + grid_id).addRowData("blankRow", {"userName":"No Data Found", "name":"", "email":"", "status":""});
                            $('.ui-paging-info').text('');
                       }
                       if(params){
                           params = JSON.parse(params);
                           $('#gs_userName').val(params.userName);
                           $('#gs_givenName').val(params.givenName);
                           $('#gs_sn').val(params.sn);
                           $('#gs_mail').val(params.mail);
                           $('#gs_accountStatus').val(params.accountStatus);
                           $('#' + grid_id).jqGrid("sortGrid", params.sidx, false, params.sord);
                           cookieHelper.deleteCookie("userGridParams");
                           $('#clearFiltersBtn').prop('disabled', false);
                       }
                       if(!hasFilters()){
                           $('#clearFiltersBtn').prop('disabled', true);
                       }
                    },
                    onCellSelect: function(rowid,iCol,val,e){
                        var status,
                            rowdata,
                            img_src,
                            new_status,
                            posted_data;
                        if(rowid !== 'blankRow'){
                            if(iCol === 6 && e.target.nodeName.toLowerCase() === "img"){
                                status = $(val).attr('title');
                                rowdata = $('#' + grid_id).getRowData(rowid);
                                img_src = (status === 'active') ? 'images/span_error.png' : 'images/span_ok.png';
                                new_status = (status === 'active') ? 'inactive' : 'active';
                                
                                userDelegate.patchUserDifferences({_id: rowdata._id, _rev: rowdata.version, accountStatus: status}, {_id: rowdata._id, _rev: rowdata.version, accountStatus: new_status}, function(d) {
                                    $(e.target).prop('src',img_src).attr('title',new_status);
                                    $('#' + grid_id).jqGrid('setCell', rowid, 'version', d._rev);
                                    $(e.target).closest('tr').removeClass('ui-state-highlight');
                                });
                            }
                            else{
                                posted_data = $('#' + grid_id).jqGrid('getGridParam','postData');
                                _this.showProfile(rowid);
                                cookieHelper.setCookie("userGridParams", JSON.stringify(posted_data));
                            }
                        }
                    },
                    onSortCol: function(){
                        if(hasFilters()){
                            $('#' + grid_id).setGridParam({ search: true });
                        }
                    },
                    onPaging: function(btn){
                        if(btn === "records"){
                            var rows = $('.ui-pg-selbox').val();
                            cookieHelper.setCookie("userGridRows", rows);
                        }
                    },
                    beforeRequest: function(){
                        var params = cookieHelper.getCookie("userGridParams");
                        if(params){
                            $('#' + grid_id).setGridParam({ postData: JSON.parse(params) });
                        }
                    }
                });
                
                $('#' + grid_id).jqGrid("filterToolbar", {
                    searchOnEnter: false,
                    beforeSearch: function() {
                        var posted_data = $('#' + grid_id).jqGrid('getGridParam','postData');
                        if(!posted_data.userName){
                            _.extend(posted_data,{userName:''});
                        }
                        if(!posted_data.givenName){
                            _.extend(posted_data,{givenName:''});
                        }
                        if(!posted_data.sn){
                            _.extend(posted_data,{sn:''});
                        }
                        if(!posted_data.mail){
                            _.extend(posted_data,{mail:''});
                        }
                        if(!posted_data.accountStatus){
                            _.extend(posted_data,{accountStatus:''});
                        }
                        $('#' + grid_id).setGridParam({ postData: posted_data });
                    },
                    afterSearch: function(){
                        $('#clearFiltersBtn').prop('disabled', false);
                    }
                });
            });
            
        }   
    }); 
    
    return new UsersView();
});



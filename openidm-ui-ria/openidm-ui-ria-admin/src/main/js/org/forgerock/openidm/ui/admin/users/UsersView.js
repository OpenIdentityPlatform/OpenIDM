/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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

/*global define, $, form2js, _ */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/admin/users/UsersView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "UserDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "dataTable",
    "org/forgerock/commons/ui/common/main/i18nManager"
], function(AbstractView, userDelegate, eventManager, constants, dataTable, i18nManager) {
    var UsersView = AbstractView.extend({
        template: "templates/admin/NewUsersTemplate.html",
        
        events: {
            "click tr": "showProfile",
            "click checkbox": "select"
        },
        
        select: function(event) {
            console.log("user selected");
            event.stopPropagation();
        },
        
        showProfile: function(event) {
            var userName = $(event.target).parent().find(".userName").text();
            
            if(userName) {
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "adminUserProfile", args: [userName]});
            }
        },
        
        render: function() {
            this.parentRender(function() {
                $('#usersTable').dataTable( {
                    "bProcessing": true,
                    "sAjaxSource": "",
                    "fnServerData": function(sUrl, aoData, fnCallback, oSettings) {
                        userDelegate.getAllUsers(function(users) {
                            var data = {aaData: users}, i;
                            
                            for(i = 0; i < data.aaData.length; i++) {
                                data.aaData[i].selector = '<input type="checkbox" />';
                                data.aaData[i].name = $('<span class="name" />').text(users[i].givenName + ' ' + users[i].familyName).wrap('<p>').parent().html();
                                data.aaData[i].userName = $('<span class="userName" />').text(users[i].userName).wrap('<p>').parent().html();
                            }

                            fnCallback(data);
                        });
                    },
                    "aoColumns": [
                        {
                            "mData": "selector",
                            "bSortable": false
                        },
                        {
                            "mData": "userName"
                        },
                        { 
                            "mData": "name" 
                        },
                        { 
                            "mData": "accountStatus",
                            "sClass": "highlight"
                        },
                        { 
                            "mData": "email" 
                        } 
                    ],
                    "oLanguage": {
                        "sUrl": "locales/" + i18nManager.language + "/datatable_translation.json"
                    },
                    "sDom": 'l<"addButton">f<"clear">rt<"clear">ip<"clear">',
                    "sPaginationType": "full_numbers",
                    "fnInitComplete": function(oSettings, json) {
                        $(".addButton").html('<a href="#users/add/" class="button active" style="margin-left: 15px; float: left;">' + $.t("common.form.addUser") + '</a>');
                    }
                });
            });
            
        }   
    }); 
    
    return new UsersView();
});



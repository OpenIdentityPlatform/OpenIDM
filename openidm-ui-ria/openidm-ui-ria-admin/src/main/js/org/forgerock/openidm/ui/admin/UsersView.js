/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
define("org/forgerock/openidm/ui/admin/UsersView", [
    "org/forgerock/openidm/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/user/delegates/UserDelegate",
    "org/forgerock/openidm/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/util/Constants",
    "dataTable"
], function(AbstractView, userDelegate, eventManager, constants, dataTable) {
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
            var email = $(event.target).parent().find("td:last").html();
            
            if(email) {
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "adminUserProfile", args: [email]});
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
                                data.aaData[i].name = '<span class="name">' + users[i].givenName + ' ' + users[i].familyName + '</span>'; 
                            }
                            
                            fnCallback(data);
                        }, function() {
                            
                        });
                    },
                    "aoColumns": [
                        {
                            "mData": "selector",
                            "bSortable": false
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
                        "sLengthMenu": "Display _MENU_ per page"
                    },
                    "sDom": 'l<"addButton">f<"clear">rt<"clear">ip<"clear">',
                    "sPaginationType": "full_numbers",
                    "fnInitComplete": function(oSettings, json) {
                        $(".addButton").html('<a href="#users/add/" class="buttonOrange" style="margin-left: 15px; float: left;">Add user</a>');
                    }
                });
            });
            
        }   
    }); 
    
    return new UsersView();
});



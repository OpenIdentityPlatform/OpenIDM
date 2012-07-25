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

/*global define */ 

/**
 * @author mbilski
 */

define("app/comp/common/navigation/NavigationCtrl",
        ["app/comp/common/navigation/NavigationView",
         "app/comp/main/Configuration",
         "app/comp/common/eventmanager/EventManager", 
         "app/util/Constants",
         "app/util/UIUtils"], 
         function(view, configuration, eventManager, constants, UIUtils) {
    var obj = {};

    obj.view = view;	

    obj.init = function(mode) {
        obj.mode = mode;
        view.show(function() {
            obj.addLinks();
            obj.view.getHomeLink().off().on('click', function(event) {
                event.preventDefault();
                eventManager.sendEvent(constants.EVENT_NAVIGATION_HOME_REQUEST);
            });
        });
    };

    obj.addLinks = function() {
        if( !obj.mode || obj.mode !== constants.MODE_ADMIN ) {
            obj.view.addOuterLink('http://forgerock.com/openam.html', 'OpenAM');
            obj.view.addOuterLink('http://forgerock.com/opendj.html', 'OpenDJ');
            obj.view.addOuterLink('http://forgerock.com/openidm.html', 'OpenIDM');	
        } else {
            obj.view.addLink('userManagementNavigationLink','User Management').bind('click', function(event) {
                event.preventDefault();
                eventManager.sendEvent(constants.EVENT_SWITCH_VIEW_REQUEST, { viewId: "app/comp/admin/usermanagement/UsersCtrl"});
            });
            obj.view.addLink('groupManagementNavigationLink','Group Management');
        }
    };

    return obj;

});


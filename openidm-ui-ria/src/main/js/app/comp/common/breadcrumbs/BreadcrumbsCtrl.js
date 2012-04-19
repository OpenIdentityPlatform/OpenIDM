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

define("app/comp/common/breadcrumbs/BreadcrumbsCtrl",
        ["app/comp/common/breadcrumbs/BreadcrumbsView", 
         "app/comp/common/eventmanager/EventManager",
         "app/util/Constants"], 
         function(view, eventManager,constants) {
    var obj = {};

    obj.view = view;

    obj.numberOfPaths = 0;

    obj.set = function(pageName) {
        console.info('setting page name');
        obj.view.setCrumb(pageName);
    };

    obj.goBack = function() {
        obj.view.top().trigger('click');
    };

    obj.addPath = function(pathName, callback) {
        obj.numberOfPaths++;
        var result = view.pushPath(pathName);
        if(callback) {
            result.on('click', callback);
        }
        return ;
    };

    obj.removePath = function() {
        if( obj.numberOfPaths > 0 ) {
            view.popPath();
            obj.numberOfPaths--;
        }		
    };

    obj.clearPath = function() {
        while(obj.numberOfPaths > 0) {
            obj.removePath();
        }
    };

    obj.init = function() {
        obj.view.getHomeButton().on('click', function(event) {
            event.preventDefault();
            eventManager.sendEvent(constants.EVENT_BREADCRUMBS_HOME_CLICKED);

            obj.set("Home");
            obj.clearPath();			
        });


    };

    return obj;

});


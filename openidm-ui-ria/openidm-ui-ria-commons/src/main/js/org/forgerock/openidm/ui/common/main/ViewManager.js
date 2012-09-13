/*
 * @license DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
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

/*global require, define, _, $ */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/common/main/ViewManager", [
    "org/forgerock/openidm/ui/common/util/UIUtils"    
], function(uiUtils) {
    var obj = {};
    
    obj.currentView = "null";
    obj.currentDialog = "null";
    
    /**
     * Initializes view if it is not equal to current view.
     * Changes URL without triggering event.
     */
    obj.changeView = function(viewPath, args, callback) {        
        var view;
        
        if(obj.currentView !== viewPath) {
            view = require(viewPath);
            
            if(view.init) {
                view.init();
            } else {
                view.render(args, callback);
            }
        } else {
            view = require(obj.currentView);
            view.rebind();
            
            if(callback) {
                callback();
            }
        }
        
        obj.currentView = viewPath;
        obj.currentDialog = "null";
    };
    
    obj.showDialog = function(dialogPath, args) {
        if(obj.currentDialog !== dialogPath) {
            require(dialogPath).render(args);
        }
        
        obj.currentDialog = dialogPath;
    };

    return obj;

});    


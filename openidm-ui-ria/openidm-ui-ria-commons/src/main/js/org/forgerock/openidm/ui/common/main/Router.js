/*
 * @license DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
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

/*global define, Backbone, _, window */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/common/main/Router", [
    "org/forgerock/openidm/ui/common/main/EventManager", 
    "org/forgerock/openidm/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/main/AbstractConfigurationAware"
], function(eventManager, constants, conf, AbstractConfigurationAware) {
    var obj = new AbstractConfigurationAware();
    
    obj.bindedRoutes = {};
    
    obj.init = function() {
        console.debug("Router init");
        
        var Router = Backbone.Router.extend({
            initialize: function(routes) {
                var route, url;
                
                for(route in routes) {                     
                    url = routes[route].url;
                    this.route(url, route, _.bind(this.processRoute, {key: route}));
                    obj.bindedRoutes[route] = _.bind(this.processRoute, {key: route});
                }
            },
            processRoute : function(args) {
                var route = obj.configuration.routes[this.key], baseView;
                
                if(route.excludedRole) {
                    if(conf.loggedUser && conf.loggedUser.roles.indexOf(route.excludedRole) !== -1) {
                        eventManager.sendEvent(constants.EVENT_UNAUTHORIZED);
                        return;
                    }
                }                
                
                if(route.role) {
                    if(!conf.loggedUser || conf.loggedUser.roles.indexOf(route.role) === -1) {
                        eventManager.sendEvent(constants.EVENT_UNAUTHORIZED);
                        return;
                    }
                }
                
                if(route.event) {
                    eventManager.sendEvent(route.event, args);
                } else if(route.dialog) {
                    route.baseView = obj.configuration.routes[route.base];
                    
                    eventManager.sendEvent(constants.EVENT_SHOW_DIALOG, {route: route, args: [args], base: route.base});
                } else if(route.view) {
                    eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: route, args: [args]});
                }
            }
        });        
        
        obj.router = new Router(obj.configuration.routes);
        Backbone.history.start();
    };
    
    obj.routeTo = function(routeName, params) {
        var link;
        
        if(params && params.args) {
            link = obj.getLink(obj.configuration.routes[routeName], params.args);
        } else {
            link = obj.configuration.routes[routeName].url;
        }
        
        if(window.location.hash === link) {
            console.log("Hard route");
            obj.bindedRoutes[routeName]();
        }
        
        obj.router.navigate(link, params);
    };
    
    obj.execRouteHandler = function(routeName) {
        obj.bindedRoutes[routeName]();
    };
    
    obj.navigate = function(link, params) {
        obj.router.navigate(link, params);
    };
    
    obj.getLink = function(route, args) {
        var i, pattern;
        
        if(!_.isRegExp(route.url)) {
            pattern = route.url.replace(/:[A-Za-z@.]+/, "?");
        } else {
            pattern = route.pattern;
        }

        if(args) {
            for(i = 0; i < args.length; i++) {
                pattern = pattern.replace("?", args[i]);
            }
        }
        
        return pattern;
    };

    return obj;

});    

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

/*global define, $, _, Backbone, window */

/**
 * @author mbilski
 */

define("org/forgerock/openidm/ui/common/components/Navigation", [
    "underscore",
    "backbone",
    "org/forgerock/openidm/ui/common/main/AbstractConfigurationAware",
    "org/forgerock/openidm/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/util/Constants"
], function(_, Backbone, AbstractConfigurationAware, AbstractView, conf, constants) {
    var obj = new AbstractConfigurationAware();
    
    obj.init = function() {
        var Navigation = AbstractView.extend({
            
            element: "#menu",
            template: "templates/common/NavigationTemplate.html",
            noBaseTemplate: true,
            
            render: function() {
                this.parentRender(_.bind(function() {
                    this.reload();
                }, this));
            },
            
            addLinks: function(linkName) {
                var url, urlName, subUrl, subUrlName;
                
                for(urlName in obj.configuration.links[linkName].urls) {
                    url = obj.configuration.links[linkName].urls[urlName];
                    
                    if (this.isCurrent(url.url) || this.isCurrent(url.baseUrl)) {
                        this.addLink(url.name, url.url, true);
                        
                        if (url.urls) {
                            this.$el.append('<div id="submenu"><ul>');
                            for(subUrlName in url.urls) {
                                subUrl = url.urls[subUrlName];
                                this.addSubLink(subUrl.name, subUrl.url, this.isCurrent(subUrl.url));
                            }
                            this.$el.append('</ul></div>');
                        }
                        
                    } else {
                        this.addLink(url.name, url.url, false);
                    }
                    
                }
                
                if (this.isOnDashboard()) {
                    this.$el.find("ul:first").find("li:first").addClass('active');
                } else {
                    this.$el.find("ul:first").find("li:first").removeClass('active');
                }
            },
            
            addLink: function(name, url, isActive) {
                var newLink = this.$el.find("ul:first").append('<li><a href="'+url+'">'+ name +'</a></li>');
                if (isActive) {
                    $(newLink).find("li:last").addClass('active');
                }
            },
            
            addSubLink: function(name, url, isActive) {
                var newSubLink = this.$el.find("ul:last").append('<li><a href="'+url+'">'+ name +'</a></li>');
                if (isActive) {
                    $(newSubLink).find("li:last").addClass('active');
                }
            },
            
            isOnDashboard: function() {
                var afterHash = window.location.href.split('#')[1];
                return afterHash === '' || afterHash === "/";
            },
            
            isCurrent: function(urlName) {
                var fromHash = "#" + window.location.href.split('#')[1];
                return fromHash.indexOf(urlName) !== -1;
            },
            
            clear: function() {
                $("#menu li").not(':first').remove();
                $("#submenu").remove();
            },
            
            reload: function() {
                this.clear();
                
                var link, linkName;
                
                for(linkName in obj.configuration.links) {
                    link = obj.configuration.links[linkName];
                    
                    if(link.role && conf.loggedUser && conf.loggedUser.roles.indexOf(link.role) !== -1) {
                        this.addLinks(linkName);
                        return;
                    } else if(!link.role) {
                        this.addLinks(linkName);
                        return;
                    }
                }
            }
        });
                
        obj.navigation = new Navigation();
        obj.navigation.render();
    };
    
    obj.reload = function() {
        if(obj.navigation) {
            obj.navigation.reload();
        }
    };

    return obj;
});
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

/*global define, $, _, Backbone, window, document */

/**
 * @author mbilski
 */

define("org/forgerock/openidm/ui/common/components/Breadcrumbs", [
    "underscore",
    "backbone"
], function(_, Backbone) {
    var Breadcrumbs= Backbone.View.extend({

        size: 0,
        element: "#nav-content",
        
        /**
         * Registers listeners and creates links using URL
         */
        init: function() {
            $(window).on('hashchange', _.bind(this.buildByUrl, this));
            this.baseTitle = document.title;
            this.buildByUrl();
        },
        
        /**
         * Creates links using URL
         */
        buildByUrl: function() {
            var path, parts, url, i, humanized;
            
            path = window.location.href.match(/#([a-zA-Z\/_.@]+)/);
            
            if(path === null) {
                path = 'Dashboard';
            } else {
                path = path[1];
            }
            
            parts = _.compact(path.split('/'));
            humanized = this.getHumanizedUrls(parts);
            
            url = "#";     
            
            this.clear();            
            for(i = 0; i < parts.length - 1; i++) {
                url += parts[i] + "/";
                this.push(humanized[i], url);
            }
            this.set(humanized[humanized.length-1]);
            
            document.title = this.baseTitle + " - " + humanized.join(" - ");
        },
        
        /**
         * Replaces '_' to ' ' and capitalize first letter in array of strings
         */
        getHumanizedUrls: function(urls) {
            var humanized = [], i, emailPattern = /^\w+@[a-zA-Z_]+?\.[a-zA-Z]{2,4}$/;
            
            for(i = 0; i < urls.length; i++) {
                humanized[i] = urls[i].split("_").join(" ");
                
                if(!emailPattern.test(humanized[i])) { 
                    humanized[i] = humanized[i].capitalize();
                }
            }
            
            return humanized;
        },
        
        clear: function() {
            while(this.size > 0) {
                this.pop();
            }
        },
        
        /**
         * Sets the name of last breadcrumb item
         */
        set: function(name) {
            $(this.element).find("span:last").html(name);
        },
        
        /**
         * Appends link to the breadcrumbs list and an arrow after it.
         */
        push: function(name, url) {        
            $(this.element).find("a:last").after(' <a href="'+url+'" class="orange">' + name + '</a>');
            $(this.element).find("a:last").before('<img src="images/navi-next.png" width="3" height="5" alt="" align="absmiddle" class="navi-next" /><span></span>');

            this.size++;
        },

        pop: function() {
            if($("#nav-content").find("a").length > 1) {
                $(this.element).find("a:last").remove();
                $(this.element).find("img:last").remove();
                $(this.element).find("span:last").remove();
            }
            
            this.size--;
        }
    });

    return new Breadcrumbs();
});
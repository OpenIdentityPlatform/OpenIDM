"use strict";

/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2011-2016 ForgeRock AS.
 */

define(["jquery", "underscore", "backbone"], function ($, _, Backbone) {
    /**
     * @exports org/forgerock/commons/ui/common/components/Breadcrumbs
     */
    var Breadcrumbs = Backbone.View.extend({

        size: 0,
        element: "#nav-content",

        /**
         * Registers listeners and creates links using URL
         */
        init: function init() {
            $(window).on('hashchange', _.bind(this.buildByUrl, this));
            this.baseTitle = document.title;
            this.buildByUrl();
        },

        /**
         * Creates links using URL
         */
        buildByUrl: function buildByUrl() {
            var path, parts, url, i, humanized;

            path = window.location.href.match(/#([a-zA-Z\/_.@]+)/);

            if (path === null) {
                document.title = this.baseTitle;
            } else {
                path = path[1];

                parts = _.compact(path.split('/'));
                humanized = this.getHumanizedUrls(parts);

                url = "#";

                this.clear();
                for (i = 0; i < parts.length - 1; i++) {
                    url += parts[i] + "/";
                    this.push(humanized[i], url);
                }
                this.set(humanized[humanized.length - 1]);

                document.title = this.baseTitle + " - " + humanized.join(" - ");
            }
        },

        /**
         * Replaces '_' to ' ' and capitalize first letter in array of strings
         */
        getHumanizedUrls: function getHumanizedUrls(urls) {
            return _.map(urls, function (url) {
                return url.split("_").join(" ").replace(new RegExp("^(.)(.*)"), function (all, first, rest) {
                    return first.toUpperCase() + rest;
                });
            });
        },

        clear: function clear() {
            while (this.size > 0) {
                this.pop();
            }
        },

        /**
         * Sets the name of last breadcrumb item
         */
        set: function set(name) {
            $(this.element).find("span:last").html(name);
        },

        /**
         * Appends link to the breadcrumbs list and an arrow after it.
         */
        push: function push(name, url) {
            $(this.element).find("a:last").after(' <a href="' + url + '" class="active">' + name + '</a>');
            $(this.element).find("a:last").before('<img src="images/navi-next.png" width="3" height="5"' + ' alt="" align="absmiddle" class="navi-next" /><span></span>');

            this.size++;
        },

        pop: function pop() {
            if ($("#nav-content").find("a").length > 1) {
                $(this.element).find("a:last").remove();
                $(this.element).find("img:last").remove();
                $(this.element).find("span:last").remove();
            }

            this.size--;
        }
    });

    return new Breadcrumbs();
});

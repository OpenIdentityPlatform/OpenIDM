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
 * Copyright 2011-2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/mapping/util/MappingUtils", [
        "jquery",
        "underscore",
        "handlebars",
        "org/forgerock/openidm/ui/common/delegates/SearchDelegate",
        "selectize"
    ],
    function($, _,
             Handlebars,
             searchDelegate,
             selectize) {

        var obj = {};

        obj.buildObjectRepresentation = function(objToRep, props){
            var propVals = [];

            _.each(props, _.bind(function(prop, i){
                var objRepEl = $("<span>"),
                    wrapper = $("<div>");
                if(objToRep[prop]){
                    objRepEl.text(Handlebars.Utils.escapeExpression(objToRep[prop])).attr("title", prop);
                }
                if(i === 0){
                    objRepEl.addClass("objectRepresentationHeader");
                } else {
                    objRepEl.addClass("objectRepresentation");
                }
                wrapper.append(objRepEl);
                propVals.push(wrapper.html());
            }, this));

            return propVals.join("<br/>");
        };

        obj.setupSampleSearch = function(el, mapping, autocompleteProps, selectSuccessCallback){
            var searchList,
                selectedItem;

            el.selectize({
                valueField: autocompleteProps[0],
                searchField: autocompleteProps,
                maxOptions: 10,
                create: false,
                onChange: function() {
                    selectSuccessCallback(selectedItem);
                },
                render: {
                    option: function(item, selectizeEscape) {
                        var fields = _.pick(item, autocompleteProps),
                            element = $('<div class="fr-search-option"></div>'),
                            counter = 0;

                        _.forIn(fields, function(value, key) {
                            if(counter === 0) {
                                $(element).append('<div class="fr-search-primary">' +selectizeEscape(value) +'</div>');
                            } else {
                                $(element).append('<div class="fr-search-secondary text-muted">' +selectizeEscape(value) +'</div>');
                            }

                            counter++;
                        }, this);

                        return element.prop('outerHTML');
                    },
                    item: function(item, escape) {
                        selectedItem = item;

                        return "<div>" +escape(item[autocompleteProps[0]]) +"</div>";
                    }
                },
                load: function(query, callback) {
                    if (!query.length || query.length < 2 || !autocompleteProps.length) {
                        return callback();
                    }

                    searchDelegate.searchResults(mapping.source, autocompleteProps, query).then(function(response) {
                        if(response) {
                            searchList = response;
                            callback([response]);
                        } else {
                            searchList = [];

                            callback();
                        }
                    });
                }
            });
        };

        obj.readOnlySituationalPolicy = function(policies){
            return _.reduce(policies, function(memo, val){
                return memo && val.action === "ASYNC";
            }, true);
        };

        return obj;
    });

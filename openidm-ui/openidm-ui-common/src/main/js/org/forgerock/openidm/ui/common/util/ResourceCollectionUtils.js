/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All rights reserved.
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
/*jslint evil:true */

define("org/forgerock/openidm/ui/common/util/ResourceCollectionUtils", [
    "jquery",
    "underscore",
    "handlebars",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/delegates/SearchDelegate"
], function ($, _, Handlebars, constants, eventManager, searchDelegate) {
    var obj = {};

    obj.resourceCollectionCache = {};

    obj.displayTextDelimiter = ", ";

    obj.getDisplayText = function(prop, item){
        var pathToResource = (prop.items) ? prop.items.resourceCollection.path : prop.resourceCollection.path,
            resourceKey = pathToResource + "/" + item._id,
            validDisplayProps = _.reject(obj.autocompleteProps(prop),function(p){
                return (p && !p.length) || !eval("item." + p);
            }),
            txt = _.map(validDisplayProps, function(p){
                return eval("item." + p);
            }).join(obj.displayTextDelimiter);

        if(!obj.resourceCollectionCache[resourceKey]) {
            obj.resourceCollectionCache[resourceKey] = txt;
        }

        return txt;
    };

    obj.autocompleteProps = function(prop, showRaw) {
        var fields = (prop.items) ? prop.items.resourceCollection.query.fields : prop.resourceCollection.query.fields;

        if(showRaw) {
            return fields;
        } else {
            return _.map(fields, function(field) {
                return field.replace("/",".");
            });
        }
    };

    obj.setupAutocompleteField = function(autocompleteField, prop, opts) {
        var pathToResource = (prop.items) ? prop.items.resourceCollection.path : prop.resourceCollection.path,
            defaultOpts = {
                valueField: '_id',
                searchField: obj.autocompleteProps(prop),
                create: false,
                preload: true,
                placeholder: $.t("templates.admin.ResourceEdit.search",{ objectTitle: prop.title || prop.name }),
                render: {
                    item: function(item, escape) {
                        var txt = obj.getDisplayText(prop, item);
                        return "<div>" + txt + "</div>";
                    },
                    option: function(item, escape) {
                        var txt = obj.getDisplayText(prop, item);
                        return "<div>" + txt + "</div>";
                    }
                },
                load: _.bind(function(query, callback) {
                    searchDelegate.searchResults(pathToResource, obj.autocompleteProps(prop, true), query).then(function(result) {
                            var convertNestedProps = function(item) {
                                    _.each(obj.autocompleteProps(prop), function(propName) {
                                        if(propName.indexOf(".") > -1) {
                                            item[propName] = eval("item." + propName);
                                        }
                                    });
                                    return item;
                                },
                                modifiedResult = _.map(result, function(item){
                                    return convertNestedProps(item);
                                });

                            callback(modifiedResult);
                        },
                        function(){
                            callback();
                        }
                    );
                }, this)
            };

        autocompleteField.selectize(_.extend({}, defaultOpts, opts || {}));
    };

    obj.getHeaderValues = function(fields, schema) {
        return _.map(fields, function(field) {
            var propField = function() {
                return eval("schema." + field.replace("/",".properties."));
            };

            if(schema && propField() && propField().title && propField().title.length) {
                return propField().title;
            } else {
                return field;
            }
        });
    };

    obj.showResource = function(resourcePath) {
        var args = resourcePath.split("/"),
            routeName = (args[0] !== "system") ? "adminEditManagedObjectView" : "adminEditSystemObjectView";

        if(args.length >= 3) {
            eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: routeName, args: args});
        }
    };

    Handlebars.registerHelper('nestedLookup', function(property,key) {
        return property[key];
    });

    return obj;
});

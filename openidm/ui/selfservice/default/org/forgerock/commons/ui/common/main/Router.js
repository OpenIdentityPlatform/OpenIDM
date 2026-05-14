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

define(["underscore", "backbone", "org/forgerock/commons/ui/common/main/EventManager", "org/forgerock/commons/ui/common/util/Constants", "org/forgerock/commons/ui/common/main/Configuration", "org/forgerock/commons/ui/common/main/AbstractConfigurationAware", "org/forgerock/commons/ui/common/util/URIUtils"], function (_, Backbone, EventManager, constants, conf, AbstractConfigurationAware, URIUtils) {
    /**
     * @exports org/forgerock/commons/ui/common/main/Router
     */

    var obj = new AbstractConfigurationAware();

    obj.currentRoute = {};

    /**
     * @deprecated
     * @see Use {@link module:org/forgerock/commons/ui/common/util/URIUtils.getCurrentUrl}
     */
    obj.getUrl = URIUtils.getCurrentUrl;

    /**
     * @deprecated
     * @see Use {@link module:org/forgerock/commons/ui/common/util/URIUtils.getCurrentOrigin}
     */
    obj.getCurrentUrlBasePart = URIUtils.getCurrentOrigin;

    /**
     * @deprecated
     * @see Use {@link module:org/forgerock/commons/ui/common/util/URIUtils.getCurrentFragmentQueryString}
     */
    obj.getURIFragmentQueryString = URIUtils.getCurrentFragmentQueryString;

    /**
     * @deprecated
     * @see Use {@link module:org/forgerock/commons/ui/common/util/URIUtils.getCurrentQueryString}
     */
    obj.getURIQueryString = URIUtils.getCurrentQueryString;

    /**
     * @deprecated
     * @see Use {@link module:org/forgerock/commons/ui/common/util/URIUtils.getCurrentFragment}
     */
    obj.getCurrentHash = function () {
        if (obj.getUrl().indexOf('#') === -1) {
            return "";
        } else {
            // cannot use window.location.hash due to FF which de-encodes this parameter.
            return obj.getUrl().substring(obj.getUrl().indexOf('#') + 1);
        }
    };

    /**
     * @deprecated
     * @see Use {@link module:org/forgerock/commons/ui/common/util/URIUtils.getCurrentFragment}
     */
    obj.getURIFragment = URIUtils.getCurrentFragment;

    /**
     * @deprecated
     * @see Use {@link module:org/forgerock/commons/ui/common/util/URIUtils.getCurrentCompositeQueryString}
     */
    obj.getCurrentUrlQueryParameters = function () {
        var hash = obj.getCurrentHash(),
            queries = obj.getURIQueryString();
        // location.search will only return a value if there are queries before the hash.
        if (hash && hash.indexOf('&') > -1) {
            queries = hash.substring(hash.indexOf('&') + 1);
        }
        return queries;
    };

    /**
     * @deprecated
     * @see Use {@link module:org/forgerock/commons/ui/common/util/URIUtils.getCurrentCompositeQueryString}
     */
    obj.getCompositeQueryString = URIUtils.getCurrentCompositeQueryString;

    /**
     * @deprecated
     * @see Use {@link module:org/forgerock/commons/ui/common/util/URIUtils.getCurrentPathName}
     */
    obj.getCurrentPathName = URIUtils.getCurrentPathName;

    obj.setUrl = function (url) {
        window.location.href = url;
    };

    obj.normalizeSubPath = function (subPath) {
        return subPath.replace(/\/$/, '');
    };

    obj.convertCurrentUrlToJSON = function () {
        var result = {};

        result.url = obj.getCurrentUrlBasePart();
        result.pathName = obj.normalizeSubPath(obj.getCurrentPathName());

        result.params = obj.convertQueryParametersToJSON(obj.getCurrentUrlQueryParameters());
        return result;
    };

    /**
     * @deprecated
     * @see Use {@link module:org/forgerock/commons/ui/common/util/URIUtils.parseQueryString}
     */
    obj.convertQueryParametersToJSON = URIUtils.parseQueryString;

    /**
     * @deprecated
     * @see Use {@link module:org/forgerock/commons/ui/common/util/URIUtils.getCurrentQueryParam}
     */
    obj.getParamByName = URIUtils.getCurrentQueryParam;

    // returns undecoded route parameters for the provided hash
    obj.extractParameters = function (route, hash) {
        if (_.isRegExp(route.url)) {
            return route.url.exec(hash).slice(1);
        } else {
            return null;
        }
    };

    /**
     * Given a route and a set of current parameters, will return a new parameter
     * list with whichever missing default values were available.
     */
    obj.applyDefaultParameters = function (route, originalParameters) {
        var populatedParameters = _.clone(originalParameters),
            maxArgLength,
            i;

        if (_.isObject(route.defaults)) {
            if (_.isArray(originalParameters) && originalParameters.length) {
                maxArgLength = originalParameters.length >= route.defaults.length ? originalParameters.length : route.defaults.length;
                for (i = 0; i < maxArgLength; i++) {
                    if (!_.isString(originalParameters[i]) && !_.isUndefined(route.defaults[i])) {
                        populatedParameters[i] = _.clone(route.defaults[i]);
                    }
                }
            } else {
                populatedParameters = _.clone(route.defaults);
            }
        }
        return populatedParameters;
    };

    obj.checkRole = function (route) {
        if (route.role) {
            if (!conf.loggedUser) {
                EventManager.sendEvent(constants.EVENT_UNAUTHENTICATED, { fromRouter: true });
                return false;
            } else if (!_.find(route.role.split(','), function (role) {
                return conf.loggedUser.uiroles.indexOf(role) !== -1;
            })) {
                EventManager.sendEvent(constants.EVENT_UNAUTHORIZED, { fromRouter: true });
                return false;
            }
        }

        if (route.excludedRole) {
            if (conf.loggedUser && conf.loggedUser.uiroles.indexOf(route.excludedRole) !== -1) {
                EventManager.sendEvent(constants.EVENT_UNAUTHORIZED, { fromRouter: true });
                return false;
            }
        }
        return true;
    };

    obj.init = function () {
        var Router = Backbone.Router.extend({
            initialize: function initialize(routes) {
                _.each(routes, function (route, key) {
                    this.route(route.url, key, _.partial(this.routeCallback, route));
                }, this);
            },
            routeCallback: function routeCallback(route) {
                if (!obj.checkRole(route)) {
                    if (!conf.loggedUser) {
                        return;
                    }
                    route = {
                        view: "org/forgerock/commons/ui/common/UnauthorizedView",
                        url: ""
                    };
                }

                /**
                 * we don't actually use any of the backbone-provided arguments to this function,
                 * as they are decoded and that results in the loss of important context.
                 * instead we parse the parameters out of the hash ourselves:
                 */
                var args = obj.applyDefaultParameters(route, obj.extractParameters(route, obj.getURIFragment()));

                obj.currentRoute = route;

                if (route.event) {
                    EventManager.sendEvent(route.event, { route: route, args: args });
                } else if (route.dialog) {
                    route.baseView = obj.configuration.routes[route.base];

                    EventManager.sendEvent(constants.EVENT_SHOW_DIALOG, { route: route, args: args, base: route.base });
                } else if (route.view) {
                    EventManager.sendEvent(constants.EVENT_CHANGE_VIEW, { route: route, args: args, fromRouter: true });
                }
            }
        });

        obj.router = new Router(obj.configuration.routes);
        Backbone.history.start();
    };

    obj.routeTo = function (route, params) {
        var link;

        if (params && params.args) {
            link = obj.getLink(route, params.args);
        } else if (_.isArray(route.defaults) && route.defaults.length) {
            link = obj.getLink(route, route.defaults);
        } else {
            link = route.url;
        }

        params.replace = false;
        obj.currentRoute = route;
        obj.router.navigate(link, params);
    };

    obj.navigate = function (link, params) {
        obj.router.navigate(link, params);
    };

    obj.getLink = function (route, rawParameters) {
        var pattern,
            args = obj.applyDefaultParameters(route, rawParameters),
            i = 0;

        if (!_.isRegExp(route.url)) {
            pattern = route.url.replace(/:[A-Za-z@.]+/, "?");
        } else {
            pattern = route.pattern;
        }

        if (args) {
            // Breaks the pattern up into groups, based on ? placeholders
            // Each ? found will be replaced by the corresponding argument
            // The final result will recompose the groups into a single string value
            pattern = _.map(pattern.match(/([^\?]+|\?)/g), function (part) {
                if (part === "?") {
                    return typeof args[i] === "string" ? args[i++] : "";
                } else {
                    return part;
                }
            }).join('');
        }

        return pattern;
    };

    return obj;
});

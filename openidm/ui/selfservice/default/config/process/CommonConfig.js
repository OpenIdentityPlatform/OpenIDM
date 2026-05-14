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
 * Portions copyright 2025 3A Systems LLC.
 */

define(["jquery", "lodash", "org/forgerock/commons/ui/common/util/Constants", "org/forgerock/commons/ui/common/main/EventManager", "org/forgerock/commons/ui/common/main/SpinnerManager", "org/forgerock/commons/ui/common/main/ErrorsHandler"], function ($, _, Constants, EventManager, spinner, errorsHandler) {
    var obj = [{
        startEvent: Constants.EVENT_APP_INITIALIZED,
        description: "Starting basic components",
        dependencies: ["org/forgerock/commons/ui/common/main/Router", "org/forgerock/commons/ui/common/main/Configuration", "org/forgerock/commons/ui/common/util/UIUtils", "org/forgerock/commons/ui/common/util/CookieHelper", "org/forgerock/commons/ui/common/main/SessionManager", "org/forgerock/commons/ui/common/main/i18nManager"],
        processDescription: function processDescription(event, Router, Configuration, UIUtils, CookieHelper, SessionManager, i18nManager) {
            var postSessionCheck = function postSessionCheck() {
                UIUtils.preloadInitialTemplates();
                UIUtils.preloadInitialPartials();
                Router.init();
            };

            i18nManager.init({
                serverLang: Configuration.globalData.lang,
                paramLang: Router.convertCurrentUrlToJSON().params,
                defaultLang: Constants.DEFAULT_LANGUAGE
            }).then(function () {
                SessionManager.getLoggedUser(function (user) {
                    Configuration.setProperty('loggedUser', user);
                    // WARNING - do not use the promise returned from sendEvent as an example for using this system
                    // TODO - replace with simplified event system as per CUI-110
                    EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: false }).then(postSessionCheck);
                }, function () {
                    if (!CookieHelper.cookiesEnabled()) {
                        location.href = "#enableCookies/";
                    }
                    EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true }).then(postSessionCheck);
                });
            });
        }
    }, {
        startEvent: Constants.EVENT_CHANGE_BASE_VIEW,
        description: "",
        dependencies: ["org/forgerock/commons/ui/common/components/Navigation", "org/forgerock/commons/ui/common/components/LoginHeader", "Footer"],
        processDescription: function processDescription(event, Navigation, LoginHeader, Footer) {
            LoginHeader.render();
            Navigation.init();
            Footer.render();
        }
    }, {
        startEvent: Constants.EVENT_AUTHENTICATION_DATA_CHANGED,
        description: "",
        dependencies: ["org/forgerock/commons/ui/common/main/Configuration", "org/forgerock/commons/ui/common/components/Navigation"],
        processDescription: function processDescription(event, Configuration, Navigation) {
            var serviceInvokerModuleName, serviceInvokerConfig;
            serviceInvokerModuleName = "org/forgerock/commons/ui/common/main/ServiceInvoker";
            serviceInvokerConfig = Configuration.getModuleConfiguration(serviceInvokerModuleName);
            if (!event.anonymousMode) {
                delete Configuration.globalData.authorizationFailurePending;
                delete serviceInvokerConfig.defaultHeaders[Constants.HEADER_PARAM_PASSWORD];
                delete serviceInvokerConfig.defaultHeaders[Constants.HEADER_PARAM_USERNAME];
                delete serviceInvokerConfig.defaultHeaders[Constants.HEADER_PARAM_NO_SESSION];

                EventManager.sendEvent(Constants.EVENT_AUTHENTICATED);
            } else {
                serviceInvokerConfig.defaultHeaders[Constants.HEADER_PARAM_PASSWORD] = Constants.ANONYMOUS_PASSWORD;
                serviceInvokerConfig.defaultHeaders[Constants.HEADER_PARAM_USERNAME] = Constants.ANONYMOUS_USERNAME;
                serviceInvokerConfig.defaultHeaders[Constants.HEADER_PARAM_NO_SESSION] = true;

                Configuration.setProperty("loggedUser", null);
                Navigation.reload();
            }
            Configuration.sendSingleModuleConfigurationChangeInfo(serviceInvokerModuleName);
        }
    }, {
        startEvent: Constants.EVENT_UNAUTHORIZED,
        description: "",
        dependencies: [],
        processDescription: function processDescription() {
            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unauthorized");
        }
    }, {
        startEvent: Constants.EVENT_UNAUTHENTICATED,
        description: "",
        dependencies: ["org/forgerock/commons/ui/common/main/Router", "org/forgerock/commons/ui/common/main/Configuration"],
        processDescription: function processDescription(error, Router, Configuration) {
            var hash = Router.getCurrentHash();
            if (!Configuration.gotoURL && !hash.match(Router.configuration.routes.login.url)) {
                Configuration.setProperty("gotoURL", "#" + hash);
            }
            EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, {
                anonymousMode: true
            });
            EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                route: Router.configuration.routes.login
            });
        }
    }, {
        startEvent: Constants.EVENT_DIALOG_CLOSE,
        description: "",
        dependencies: ["org/forgerock/commons/ui/common/main/Configuration", "org/forgerock/commons/ui/common/util/ModuleLoader", "org/forgerock/commons/ui/common/components/Navigation", "org/forgerock/commons/ui/common/main/Router", "org/forgerock/commons/ui/common/main/ViewManager"],
        processDescription: function processDescription(event, Configuration, ModuleLoader, Navigation, Router, ViewManager) {
            ViewManager.currentDialog = null;
            if (Configuration.baseView) {
                ModuleLoader.load(Router.configuration.routes[Configuration.baseView].view).then(function (view) {
                    view.rebind();
                    Router.navigate(Router.getLink(Router.configuration.routes[Configuration.baseView], Configuration.baseViewArgs));
                    Navigation.reload();
                });
            }
        }
    }, {
        startEvent: Constants.EVENT_REST_CALL_ERROR,
        description: "",
        processDescription: function processDescription(event) {
            errorsHandler.handleError(event.data, event.errorsHandlers);
            spinner.hideSpinner();
        }
    }, {
        startEvent: Constants.EVENT_START_REST_CALL,
        description: "",
        processDescription: function processDescription(event) {
            if (!event.suppressSpinner) {
                spinner.showSpinner();
            }
        }
    }, {
        startEvent: Constants.EVENT_END_REST_CALL,
        description: "",
        processDescription: function processDescription() {
            spinner.hideSpinner();
        }
    }, {
        startEvent: Constants.EVENT_CHANGE_VIEW,
        description: "",
        dependencies: ["org/forgerock/commons/ui/common/main/Configuration", "org/forgerock/commons/ui/common/util/ModuleLoader", "org/forgerock/commons/ui/common/components/Navigation", "org/forgerock/commons/ui/common/main/Router", "org/forgerock/commons/ui/common/SiteConfigurator", "org/forgerock/commons/ui/common/main/SpinnerManager", "org/forgerock/commons/ui/common/main/ViewManager"],
        processDescription: function processDescription(event, Configuration, ModuleLoader, Navigation, Router, SiteConfigurator, SpinnerManager, ViewManager) {
            var route = event.route,
                params = event.args,
                callback = event.callback,
                fromRouter = event.fromRouter;

            if (!Router.checkRole(route)) {
                route = {
                    view: "org/forgerock/commons/ui/common/UnauthorizedView",
                    url: "",
                    fromRouter: true
                };
            }

            if (Configuration.backgroundLogin) {
                return;
            }

            return ModuleLoader.load(route.view).then(function (view) {
                view.route = route;

                params = params || route.defaults;
                Configuration.setProperty("baseView", "");
                Configuration.setProperty("baseViewArgs", "");

                return SiteConfigurator.configurePage(route, params).then(function () {
                    var promise = $.Deferred();
                    SpinnerManager.hideSpinner(10);
                    if (!fromRouter) {
                        Router.routeTo(route, { trigger: true, args: params });
                    }

                    ViewManager.changeView(route.view, params, function () {
                        if (callback) {
                            callback();
                        }
                        promise.resolve(view);
                    }, route.forceUpdate);

                    Navigation.reload();
                    return promise;
                });
            });
        }
    }, {
        startEvent: Constants.EVENT_SHOW_DIALOG,
        description: "",
        dependencies: ["org/forgerock/commons/ui/common/main/ViewManager", "org/forgerock/commons/ui/common/main/Router", "org/forgerock/commons/ui/common/main/Configuration", "org/forgerock/commons/ui/common/components/Navigation"],
        processDescription: function processDescription(args, viewManager, router, conf, navigation) {
            var route = args.route,
                params = args.args,
                callback = args.callback,
                baseViewArgs;

            if (!router.checkRole(route)) {
                return;
            }

            if (viewManager.currentViewArgs === null) {
                baseViewArgs = params;
            } else {
                baseViewArgs = viewManager.currentViewArgs;
            }

            conf.setProperty("baseView", args.base);
            conf.setProperty("baseViewArgs", baseViewArgs);

            navigation.init();

            if (!_.has(route, "baseView") && _.has(route, "base")) {
                viewManager.changeView(router.configuration.routes[route.base].view, baseViewArgs, function () {
                    viewManager.showDialog(route.dialog, params);
                    router.navigate(router.getLink(route, params));
                    if (callback) {
                        callback();
                    }
                });
            } else {
                /*
                 * There is an expectation that the base view uses some subset of the same
                 * params that the dialog uses, and that they are in the same order.
                 * The base might have a url like myView/foo, where '/foo' is the first param.
                 * The dialog should be constructed so that its own arguments follow, like so:
                 * myViewDialog/foo/bar - the params being '/foo' and '/bar'. Because '/foo'
                 * is still in the first position, it is reasonable to pass to the base view
                 * (along with '/bar', which will presumably be ignored)
                 */

                viewManager.changeView(route.baseView.view, baseViewArgs, function () {
                    viewManager.showDialog(route.dialog, params);
                    router.navigate(router.getLink(route, params));
                    if (callback) {
                        callback();
                    }
                });
            }
        }
    }, {
        startEvent: Constants.EVENT_SERVICE_UNAVAILABLE,
        description: "",
        dependencies: ["org/forgerock/commons/ui/common/main/Router"],
        processDescription: function processDescription() {
            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "serviceUnavailable");
        }
    }, {
        startEvent: Constants.ROUTE_REQUEST,
        description: "",
        dependencies: ["org/forgerock/commons/ui/common/components/Navigation", "org/forgerock/commons/ui/common/main/Router", "org/forgerock/commons/ui/common/main/ViewManager"],
        processDescription: function processDescription(event, Navigation, Router, ViewManager) {
            var route = Router.configuration.routes[event.routeName];

            // trigger defaults to true
            if (event.trigger === undefined) {
                event.trigger = true;
            } else if (event.trigger === false) {
                ViewManager.currentView = route.view;
                ViewManager.currentViewArgs = event.args;
            }

            Router.routeTo(route, event);
            Navigation.reload();
        }
    }, {
        startEvent: Constants.EVENT_DISPLAY_MESSAGE_REQUEST,
        description: "",
        dependencies: ["org/forgerock/commons/ui/common/components/Messages"],
        processDescription: function processDescription(event, messagesManager) {
            messagesManager.messages.displayMessageFromConfig(event);
        }
    }, {
        startEvent: Constants.EVENT_LOGIN_REQUEST,
        description: "",
        dependencies: ["org/forgerock/commons/ui/common/main/Configuration", "org/forgerock/commons/ui/common/util/ModuleLoader", "org/forgerock/commons/ui/common/main/Router", "org/forgerock/commons/ui/common/main/SessionManager", "org/forgerock/commons/ui/common/main/ViewManager"],
        processDescription: function processDescription(event, Configuration, ModuleLoader, Router, SessionManager, ViewManager) {
            var promise = $.Deferred(),
                submitContent = event.submitContent || event;

            SessionManager.login(submitContent, function (user) {
                Configuration.setProperty('loggedUser', user);

                EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: false });

                if (!Configuration.backgroundLogin) {
                    if (_.has(Configuration, "globalData.auth.urlParams.goto")) {
                        window.location.href = decodeURIComponent(Configuration.globalData.auth.urlParams.goto);
                        return false;
                    }

                    if (Configuration.gotoURL && _.indexOf(["#", "", "#/", "/#"], Configuration.gotoURL) === -1) {
                        Router.navigate(Configuration.gotoURL, { trigger: true });
                        delete Configuration.gotoURL;
                    } else if (Router.checkRole(Router.configuration.routes["default"])) {
                        EventManager.sendEvent(Constants.ROUTE_REQUEST, { routeName: "default", args: [] });
                    } else {
                        EventManager.sendEvent(Constants.EVENT_UNAUTHORIZED);
                        return;
                    }
                } else if (ViewManager.currentDialog !== null) {
                    ModuleLoader.load(ViewManager.currentDialog).then(function (dialog) {
                        dialog.close();
                    });
                } else if (typeof $.prototype.modal === "function") {
                    $(".modal.in").modal("hide");
                    // There are some cases, when user is presented with login modal panel,
                    // rather than a normal login view. backgroundLogin property is used to
                    // indicate such cases. It should be deleted afterwards for correct
                    // display of the login view later
                    delete Configuration.backgroundLogin;
                }

                promise.resolve(user);
            }, function (reason) {

                if (_.has(Configuration, "globalData.auth.urlParams.gotoOnFail")) {
                    window.location.href = Configuration.globalData.auth.urlParams.gotoOnFail;
                    return false;
                }

                reason = reason ? reason : "authenticationFailed";
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, reason);

                if (event.failureCallback) {
                    event.failureCallback(reason);
                }

                promise.reject(reason);
            });

            return promise;
        }
    }, {
        startEvent: Constants.EVENT_SHOW_LOGIN_DIALOG,
        description: "",
        dependencies: ["org/forgerock/commons/ui/common/main/Configuration", "LoginDialog", "org/forgerock/commons/ui/common/util/Queue"],
        processDescription: function processDescription(event, Configuration, LoginDialog, Queue) {
            var queueName = "loginDialogAuthCallbacks";
            if (!Configuration.globalData[queueName]) {
                Configuration.globalData[queueName] = new Queue();
            }
            // only render the LoginDialog if it has an empty callback queue
            if (!Configuration.globalData[queueName].peek()) {
                LoginDialog.render({
                    authenticatedCallback: function authenticatedCallback() {
                        var callback = Configuration.globalData[queueName].remove();
                        while (callback) {
                            callback();
                            callback = Configuration.globalData[queueName].remove();
                        }
                    }
                });
            }
            if (event.authenticatedCallback) {
                Configuration.globalData[queueName].add(event.authenticatedCallback);
            }
        }
    }, {
        startEvent: Constants.EVENT_LOGOUT,
        description: "",
        dependencies: ["org/forgerock/commons/ui/common/main/Router", "org/forgerock/commons/ui/common/main/Configuration", "org/forgerock/commons/ui/common/main/SessionManager"],
        processDescription: function processDescription(event, router, conf, sessionManager) {
            return sessionManager.logout(function () {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "loggedOut");
                EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true });
                return EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                    route: router.configuration.routes.login
                });
            });
        }
    }, {
        startEvent: Constants.EVENT_SELECT_KBA_QUESTION,
        description: "",
        dependencies: ["org/forgerock/commons/ui/user/anonymousProcess/KBAView"],
        processDescription: function processDescription(event, KBAView) {
            KBAView.changeQuestion();
        }
    }, {
        startEvent: Constants.EVENT_DELETE_KBA_QUESTION,
        description: "",
        dependencies: ["org/forgerock/commons/ui/user/anonymousProcess/KBAView"],
        processDescription: function processDescription(event, KBAView) {
            KBAView.deleteQuestion(event.viewId);
        }
    }];
    return obj;
});

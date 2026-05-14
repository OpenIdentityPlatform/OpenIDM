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
 * Copyright 2012-2016 ForgeRock AS.
 */

define(["jquery", "underscore", "org/forgerock/commons/ui/common/components/Messages", "org/forgerock/commons/ui/common/main/AbstractView", "org/forgerock/commons/ui/common/util/ModuleLoader"], function ($, _, msg, AbstractView, ModuleLoader) {
    var obj = {},
        decodeArgs = function decodeArgs(args) {
        return _.map(args, function (a) {
            return a && decodeURIComponent(a) || "";
        });
    },
        isBackboneView = function isBackboneView(view) {
        return view.render && !_.isFunction(view);
    },
        isReactView = function isReactView(view) {
        return !view.render && _.isFunction(view);
    };

    obj.currentView = null;
    obj.currentDialog = null;
    obj.currentViewArgs = null;
    obj.currentDialogArgs = null;

    /**
     * Initializes view if it is not equal to current view.
     * Changes URL without triggering event.
     */
    obj.changeView = function (viewPath, args, callback, forceUpdate) {
        var decodedArgs = decodeArgs(args);

        if (obj.currentView !== viewPath || forceUpdate || !_.isEqual(obj.currentViewArgs, args)) {
            if (obj.currentDialog !== null) {
                ModuleLoader.load(obj.currentDialog).then(function (dialog) {
                    dialog.close();
                });
            }

            //close all existing dialogs
            if (typeof $.prototype.modal === "function") {
                $('.modal.in').modal('hide');
            }

            obj.currentDialog = null;

            msg.messages.hideMessages();
            ModuleLoader.load(viewPath).then(function (view) {
                // For ES6 modules, we require that the view is the default export.
                if (view.__esModule) {
                    view = view.default;
                }

                // TODO: Investigate whether this is required anymore
                if (view.init) {
                    view.init();
                }

                if (isBackboneView(view)) {
                    view.render(decodedArgs, callback);
                } else if (isReactView(view)) {
                    // ReactAdapterView (and thus React and React-DOM) are only loaded when a React view is encountered
                    require(["org/forgerock/commons/ui/common/main/ReactAdapterView"], function (ReactAdapterView) {
                        new ReactAdapterView({ reactView: view }).render();
                    });
                } else {
                    throw new Error("[ViewManager] Unable to determine view type (Backbone or React).");
                }
            });
        } else {
            ModuleLoader.load(obj.currentView).then(function (view) {
                view.rebind();

                if (callback) {
                    callback();
                }
            });
        }

        obj.currentViewArgs = args;
        obj.currentView = viewPath;
    };

    obj.showDialog = function (dialogPath, args, callback) {
        var decodedArgs = decodeArgs(args);

        if (obj.currentDialog !== dialogPath || !_.isEqual(obj.currentDialogArgs, decodedArgs)) {
            msg.messages.hideMessages();
            ModuleLoader.load(dialogPath).then(function (dialog) {
                dialog.render(decodedArgs, callback);
            });
        }

        if (obj.currentDialog !== null) {
            ModuleLoader.load(obj.currentDialog).then(function (dialog) {
                dialog.close();
            });
        }

        obj.currentDialog = dialogPath;
        obj.currentDialogArgs = decodedArgs;
    };

    obj.refresh = function () {
        var cDialog = obj.currentDialog,
            cDialogArgs = obj.currentDialogArgs;

        obj.changeView(obj.currentView, obj.currentViewArgs, function () {}, true);
        if (cDialog && cDialog !== null) {
            obj.showDialog(cDialog, cDialogArgs);
        }
    };

    return obj;
});

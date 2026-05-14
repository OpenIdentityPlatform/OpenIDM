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

define(["jquery", "underscore", "backbone", "org/forgerock/commons/ui/common/main/Configuration", "org/forgerock/commons/ui/common/util/Constants", "org/forgerock/commons/ui/common/main/EventManager", "org/forgerock/commons/ui/common/util/ModuleLoader", "org/forgerock/commons/ui/common/main/Router", "ThemeManager", "org/forgerock/commons/ui/common/util/UIUtils", "org/forgerock/commons/ui/common/main/ValidatorsManager"], function ($, _, Backbone, Configuration, Constants, EventManager, ModuleLoader, Router, ThemeManager, UIUtils, ValidatorsManager) {
    /**
     * @exports org/forgerock/commons/ui/common/main/AbstractView
     */

    /**
      Internal helper method shared by the default implementations of
      validationSuccessful and validationFailed
    */
    function validationStarted(event) {
        if (!event || !event.target) {
            return $.Deferred().reject();
        }
        // necessary to load bootstrap for popover support
        // (which isn't always necessary for AbstractView)
        return ModuleLoader.load("bootstrap").then(function () {
            return $(event.target);
        });
    }

    /**
      Sets the enabled state of the submit button based on the validation status of the provided form
    */
    function validationCompleted(formElement) {
        var button = formElement.find("input[type=submit]");

        if (!button.length) {
            button = formElement.find("#submit");
        }
        if (button.length) {
            button.prop('disabled', !ValidatorsManager.formValidated(formElement));
        }
    }

    return Backbone.View.extend({

        /**
         * This params should be passed when creating new object, for example:
         * new View({el: "#someId", template: "templates/main.html"});
         */
        element: "#content",

        baseTemplate: "templates/common/DefaultBaseTemplate.html",

        /**
         * View mode: replace or append
         */
        mode: "replace",
        defaultEvents: {
            "validationSuccessful :input": "validationSuccessful",
            "validationReset :input": "validationSuccessful",
            "validationFailed :input": "validationFailed"
        },
        initialize: function initialize() {
            this.data = this.data || {};
            _.extend(this.events, this.defaultEvents);
            this.delegateEvents();
        },

        /**
         * Change content of 'el' element with 'viewTpl',
         * which is compiled using 'data' attributes.
         */
        parentRender: function parentRender(callback) {
            this.callback = callback;

            var _this = this,
                needsNewBaseTemplate = function needsNewBaseTemplate() {
                return Configuration.baseTemplate !== _this.baseTemplate && !_this.noBaseTemplate;
            };
            EventManager.registerListener(Constants.EVENT_REQUEST_RESEND_REQUIRED, function () {
                _this.unlock();
            });

            ThemeManager.getTheme().then(function (theme) {
                _this.data.theme = theme;

                if (needsNewBaseTemplate()) {
                    UIUtils.renderTemplate(_this.baseTemplate, $("#wrapper"), _.extend({}, Configuration.globalData, _this.data), _.bind(_this.loadTemplate, _this), "replace", needsNewBaseTemplate);
                } else {
                    _this.loadTemplate();
                }
            });
        },

        loadTemplate: function loadTemplate() {
            var self = this,
                validateCurrent = function validateCurrent() {
                if (!_.has(self, "route")) {
                    return true;
                } else if (!self.route.url.length && Router.getCurrentHash().replace(/^#/, '') === "") {
                    return true;
                } else if (self.route === Router.configuration.routes.login) {
                    /**
                     * Determines if the current route is a login route, in which case allow the route  to execute.
                     * This is due to OpenAM's requirement for two views rendering being rendered at the same time
                     * (an arbitrary view and a session expiry login dialog view layered above) where the route and
                     * the hash don't match.
                     */
                    return true;
                } else {
                    return Router.getCurrentHash().replace(/^#/, '').match(self.route.url);
                }
            };

            this.setElement($(this.element));
            this.$el.unbind();
            this.delegateEvents();

            if (Configuration.baseTemplate !== this.baseTemplate && !this.noBaseTemplate) {
                Configuration.setProperty("baseTemplate", this.baseTemplate);
                EventManager.sendEvent(Constants.EVENT_CHANGE_BASE_VIEW);
            }

            // Ensure all partials are (pre)loaded before rendering the template
            $.when.apply($, _.map(this.partials, UIUtils.preloadPartial)).then(function () {
                UIUtils.renderTemplate(self.template, self.$el, _.extend({}, Configuration.globalData, self.data), self.callback ? _.bind(self.callback, self) : _.noop(), self.mode, validateCurrent);
            });
        },

        rebind: function rebind() {
            this.setElement($(this.element));
            this.$el.unbind();
            this.delegateEvents();
        },

        render: function render(args, callback) {
            this.parentRender(callback);
        },

        /**
         * This is the default implementation of the function used to reflect that
         * a given field has passed validation. It is invoked via a the event system,
         * and can be overridden per-view as needed.
         */
        validationSuccessful: function validationSuccessful(event) {
            validationStarted(event).then(function (input) {
                if (input.data()["bs.popover"]) {
                    input.popover('destroy');
                }
                input.parents(".form-group").removeClass('has-feedback has-error');
                return input.closest("form");
            }).then(validationCompleted);
        },

        /**
         * This is the default implementation of the function used to reflect that
         * a given field has failed validation. It is invoked via a the event system,
         * and can be overridden per-view as needed.
         *
         * @param {object} details - "failures" entry lists all messages (localized) associated with this validation
         *                           failure
         */
        validationFailed: function validationFailed(event, details) {
            validationStarted(event).then(function (input) {
                input.parents(".form-group").addClass('has-feedback has-error');
                if (input.data()["bs.popover"]) {
                    input.data('bs.popover').options.content = '<i class="fa fa-exclamation-circle"></i> ' + details.failures.join('<br><i class="fa fa-exclamation-circle"></i> ');
                } else {
                    input.popover({
                        validationMessage: details.failures,
                        animation: false,
                        content: '<i class="fa fa-exclamation-circle"></i> ' + details.failures.join('<br><i class="fa fa-exclamation-circle"></i> '),
                        trigger: 'focus hover',
                        placement: 'top',
                        html: 'true',
                        template: '<div class="popover popover-error help-block" role="tooltip">' + '<div class="arrow"></div><h3 class="popover-title"></h3>' + '<div class="popover-content"></div></div>'
                    });
                }
                if (input.is(":focus")) {
                    input.popover("show");
                }
                return input.closest("form");
            }).then(validationCompleted);
        },

        // legacy; needed here to prevent breakage of views which have an event registered for this function
        onValidate: function onValidate() {
            console.warn("Deprecated use of onValidate method; Change to validationSuccessful / validationFailed");
        }
    });
});

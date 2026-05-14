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

define(["jquery", "underscore", "org/forgerock/commons/ui/common/main/AbstractView", "org/forgerock/commons/ui/common/util/UIUtils", "org/forgerock/commons/ui/common/util/Constants", "org/forgerock/commons/ui/common/main/EventManager", "org/forgerock/commons/ui/common/main/Configuration"], function ($, _, AbstractView, UIUtils, Constants, EventManager, Configuration) {
    /**
     * @exports org/forgerock/commons/ui/common/components/Dialog
     */
    return AbstractView.extend({
        template: "templates/common/DialogTemplate.html",
        element: "#dialogs",

        data: {},

        mode: "append",

        events: {
            "click .dialogCloseCross": "close",
            "click input[name='close']": "close",
            "click .dialogContainer": "stop"
        },

        actions: [{
            "type": "button",
            "name": "close"
        }],

        stop: function stop(event) {
            event.stopPropagation();
        },

        /**
         * Creates new dialog in #dialogs div. Fills it with dialog template.
         * Then creates actions buttons and bind events. If actions map is empty, default
         * close action is added.
         */
        show: function show(callback) {

            this.data.actions = _.map(this.actions, function (a) {
                if (a.type === "submit") {
                    a.buttonClass = "btn-primary";
                } else {
                    a.buttonClass = "btn-default";
                }
                return a;
            });

            this.setElement($("#dialogs"));
            this.parentRender(_.bind(function () {

                this.$el.addClass('show');
                this.setElement(this.$el.find(".dialogContainer:last"));

                $("#dialog-background").addClass('show');
                this.$el.off('click').on('click', _.bind(this.close, this));

                this.loadContent(callback);
                this.delegateEvents();
            }, this));
        },

        /**
         * Loads template from 'contentTemplate'
         */
        loadContent: function loadContent(callback) {
            UIUtils.renderTemplate(this.contentTemplate, this.$el.find(".dialogContent"), _.extend({}, Configuration.globalData, this.data), callback ? _.bind(callback, this) : _.noop(), "append");
        },

        render: function render() {
            this.show();
        },

        close: function close(e) {
            if (e) {
                e.preventDefault();
            }

            if ($(".dialogContainer").length < 2) {
                $("#dialog-background").removeClass('show');
                $("#dialogs").removeClass('show');
                $("#dialogs").hide();
            }

            EventManager.sendEvent(Constants.EVENT_DIALOG_CLOSE);

            this.$el.remove();
        },

        addAction: function addAction(name, type) {
            if (!this.getAction(name)) {
                this.actions.push({
                    "name": name,
                    "type": type
                });
            }
        },

        addTitle: function addTitle(title) {
            this.data.dialogTitle = title;
        },

        getAction: function getAction(name) {
            return _.find(this.actions, function (a) {
                return a.name === name;
            });
        }
    });
});

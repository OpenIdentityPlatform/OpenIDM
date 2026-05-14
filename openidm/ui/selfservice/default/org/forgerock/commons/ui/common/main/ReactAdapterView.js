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
 * Copyright 2016 ForgeRock AS.
 */

define(["lodash", "backbone", "react-dom", "react", "org/forgerock/commons/ui/common/main/Configuration", "org/forgerock/commons/ui/common/main/EventManager", "org/forgerock/commons/ui/common/util/Constants", "org/forgerock/commons/ui/common/util/UIUtils"], function (_, Backbone, ReactDOM, React, Configuration, EventManager, Constants, UIUtils) {
    var BASE_TEMPLATE = "templates/common/DefaultBaseTemplate.html";

    function throwOnNoInitializationOptions(options) {
        if (!options) {
            throw new Error("[ReactAdapterView] No initialization options found.");
        }
    }

    function throwOnNoReactView(options) {
        if (!options.reactView) {
            throw new Error("[ReactAdapterView] No \"reactView\" option found on initialization options.");
        }
    }

    return Backbone.View.extend({
        initialize: function initialize(options) {
            throwOnNoInitializationOptions(options);
            throwOnNoReactView(options);

            this.options = options;

            _.defaults(this.options, {
                reactProps: {},
                needsBaseTemplate: true
            });
        },

        setBaseTemplate: function setBaseTemplate() {
            Configuration.setProperty("baseTemplate", BASE_TEMPLATE);
            EventManager.sendEvent(Constants.EVENT_CHANGE_BASE_VIEW);
        },

        renderReactComponent: function renderReactComponent() {
            ReactDOM.render(React.createElement(this.options.reactView, this.options.reactProps), this.el);
        },

        render: function render() {
            var view = this;

            if (this.options.needsBaseTemplate) {
                this.setBaseTemplate();

                UIUtils.compileTemplate(BASE_TEMPLATE).then(function (template) {
                    document.getElementById("wrapper").innerHTML = template;
                    view.setElement("#content");
                    view.renderReactComponent();
                });
            } else {
                view.renderReactComponent();
            }
        }
    });
});

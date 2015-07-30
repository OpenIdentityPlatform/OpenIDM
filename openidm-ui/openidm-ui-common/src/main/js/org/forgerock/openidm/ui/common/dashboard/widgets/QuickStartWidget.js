/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

define("org/forgerock/openidm/ui/common/dashboard/widgets/QuickStartWidget", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration"
], function($, _, AbstractView, eventManager, constants, conf) {
    var widgetInstance = {},
        Widget = AbstractView.extend({
            noBaseTemplate: true,
            template: "templates/dashboard/widget/QuickStartWidgetTemplate.html",
            model: {
            },
            events: {

            },
            data: {

            },
            render: function(args, callback) {
                this.element = args.element;
                this.model.menu = args.menu;
                this.data.cards = args.widget.cards;

                _.each(this.data.cards, function(card){
                    card.name = $.t(card.name);
                });

                this.parentRender(_.bind(function(){
                    if(callback) {
                        callback();
                    }
                }, this));
            }
        });

    widgetInstance.generateWidget = function(loadingObject, callback) {
        var widget = {};

        $.extend(true, widget, new Widget());

        widget.render(loadingObject, callback);

        return widget;
    };

    return widgetInstance;
});

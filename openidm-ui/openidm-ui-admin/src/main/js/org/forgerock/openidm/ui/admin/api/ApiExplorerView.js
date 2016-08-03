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

define([
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractView"
], function($, AbstractView) {
    var ApiExplorerView = AbstractView.extend({
        template: "templates/admin/util/IframeViewTemplate.html",
        render: function(args,callback) {
            this.data.iframeSrc = "/api";
            this.data.iframeClass = "api-explorer-iframe";
            this.parentRender(function () {
                var iframe = this.$el.find("." + this.data.iframeClass);

                iframe[0].style.height = iframe[0].contentWindow.document.body.scrollHeight + 'px';

                if (callback) {
                    callback();
                }
            });
        }
    });

    return new ApiExplorerView();
});

/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

define("org/forgerock/openidm/ui/admin/util/ReconDetailsView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "moment"
], function($, _,
            AbstractView,
            moment) {

    var ReconDetailsView = AbstractView.extend({
        template: "templates/admin/util/ReconDetailsTemplate.html",
        element: "#syncStatusDetails",
        noBaseTemplate: true,
        events: {

        },

        render: function (syncDetails, callback) {
            this.data.syncDetails = syncDetails;

            if(syncDetails) {
                this.data.timeDisplay = moment.utc(syncDetails.duration).format("HH:mm:ss:SSS");
            }

            this.parentRender(_.bind(function () {
                this.$el.find(".fa-info-circle").popover({
                    content: function () { return $(this).attr("data-title");},
                    trigger:'hover click',
                    container: 'body',
                    placement:'top',
                    html: 'true',
                    title: ''
                });

                if(callback) {
                    callback();
                }

            }, this));
        }

    });

    return new ReconDetailsView();
});

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
 * Copyright 2014-2015 ForgeRock AS.
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

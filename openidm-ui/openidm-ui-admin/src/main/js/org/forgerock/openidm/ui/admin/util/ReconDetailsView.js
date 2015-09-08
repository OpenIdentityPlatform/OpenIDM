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
    "org/forgerock/commons/ui/common/main/AbstractView"
], function($, _, AbstractView) {

    var ReconDetailsView = AbstractView.extend({
        template: "templates/admin/util/ReconDetailsTemplate.html",
        element: "#syncStatusDetails",
        noBaseTemplate: true,
        events: {

        },

        render: function (syncDetails, callback) {
            this.data.syncDetails = syncDetails;

            if(syncDetails) {
                this.data.timeDisplay = this.millisecondsToTime(syncDetails.duration);
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
        },

        millisecondsToTime: function(milli) {
            var milliseconds = 0,
                seconds = 0,
                minutes = 0,
                hours = 0;

            if(milli !== -1) {
                milliseconds = parseInt((milli%1000), 10);
                seconds = parseInt((milli/1000)%60, 10);
                minutes = parseInt((milli/(1000*60))%60, 10);
                hours = parseInt((milli/(1000*60*60))%24, 10);

                hours = (hours < 10) ? "0" + hours : hours;
                minutes = (minutes < 10) ? "0" + minutes : minutes;
                seconds = (seconds < 10) ? "0" + seconds : seconds;

                if(milliseconds < 10) {
                    milliseconds = milliseconds + "00";
                } else if (milliseconds < 100) {
                    milliseconds = milliseconds + "0";
                }

            } else {
                milliseconds = "000";
                seconds = "00";
                minutes = "00";
                hours = "00";
            }

            return hours +":" +minutes + ":" + seconds + "." + milliseconds;
        }

    });

    return new ReconDetailsView();
});

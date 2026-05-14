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

define(["jquery"], function ($) {

    var obj = {};

    obj.init = function () {
        $("#popup").on('mouseleave', function () {
            obj.hide();
        });
    };

    obj.setContent = function (content) {
        $("#popup-content").html(content);
    };

    obj.setPositionBy = function (element) {
        var ph,
            left = $(element).position().left,
            top = $(element).position().top,
            h = $(element).height();

        $("#popup").css('left', left);
        $("#popup").css('top', top);

        $("#popup").css('height', h);
        $("#popup-content").css("margin-left", 20);

        ph = $("#popup-content").height();
        $("#popup-content").css("margin-top", -ph * 1.2);
    };

    obj.show = function () {
        $("#popup").show();
    };

    obj.hide = function () {
        $("#popup").hide();
    };

    return obj;
});

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

define(["jquery", "spin"], function ($, Spinner) {

    var obj = {};

    obj.showSpinner = function (priority) {
        if (obj.spinner) {
            obj.hideSpinner();
        }

        obj.spinner = new Spinner().spin(document.getElementById('wrapper'));
        $(".spinner").position({
            of: $(window),
            my: "center center",
            at: "center center"
        });

        if (priority && (!obj.priority || priority > obj.priority)) {
            obj.priority = priority;
        }

        $("#wrapper").attr("aria-busy", true);
    };

    obj.hideSpinner = function (priority) {
        if (obj.spinner && (!obj.priority || priority && priority >= obj.priority)) {
            obj.spinner.stop();
            delete obj.priority;
        }

        $("#wrapper").attr("aria-busy", false);
    };

    return obj;
});

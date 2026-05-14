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

define(["underscore", "handlebars", "xdate", "moment"], function (_, Handlebars, XDate, moment) {

    var obj = {};

    //format ISO8601; example: 2012-10-29T10:49:49.419+01:00
    Handlebars.registerHelper('date', function (unformattedDate, datePattern) {
        var date = obj.parseDateString(unformattedDate),
            formattedDate;

        if (!obj.isDateValid(date)) {
            return "";
        }

        if (datePattern && _.isString(datePattern)) {
            formattedDate = obj.formatDate(date, datePattern);
        } else {
            formattedDate = obj.formatDate(date);
        }

        return new Handlebars.SafeString(formattedDate);
    });

    obj.defaultDateFormat = "MMMM dd, yyyy";

    obj.formatDate = function (date, datePattern) {
        if (datePattern) {
            return new XDate(date).toString(datePattern);
        } else {
            return new XDate(date).toString(obj.defaultDateFormat);
        }
    };

    obj.isDateValid = function (date) {
        if (Object.prototype.toString.call(date) !== "[object Date]") {
            return false;
        }
        return !isNaN(date.getTime());
    };

    obj.isDateStringValid = function (dateString, datePattern) {
        return dateString.length === datePattern.length && moment(dateString, datePattern).isValid();
    };

    obj.parseStringValid = function (dateString, datePattern) {
        return dateString.length === datePattern.length && moment(dateString, datePattern).isValid();
    };

    obj.getDateFromEpochString = function (stringWithMilisFromEpoch) {
        return new XDate(parseInt(stringWithMilisFromEpoch, 10)).toDate();
    };

    obj.currentDate = function () {
        return new XDate().toDate();
    };

    obj.parseDateString = function (dateString, datePattern) {
        if (datePattern) {
            datePattern = datePattern.replace(/d/g, 'D');
            datePattern = datePattern.replace(/y/g, 'Y');
            return moment(dateString, datePattern).toDate();
        } else {
            return new XDate(dateString).toDate();
        }
    };

    return obj;
});

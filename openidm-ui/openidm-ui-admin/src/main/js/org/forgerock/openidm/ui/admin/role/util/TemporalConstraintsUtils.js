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
    "underscore",
    "handlebars",
    "moment",
    "moment-timezone"
], function ($, _, Handlebars, moment, momentTimezone) {
    var obj = {},
        format = 'MM/DD/YYYY h:mm A';

    /*
    * This function takes in an interval string (ex. "2016-04-25T07:00:00.000Z/2016-04-30T07:00:00.000Z"),
    * splits it into two parts (start and end), converts the two ISO formatted date strings into a human readable format,
    * and returns an object
    * ex:
    *    { start: "04/25/2016 12:00 AM", end: "04/30/2016 12:00 AM"}
    *
    * @param {string} intervalString - a string containing two interval parts separated by "/"
    * @param {number} timezoneOffset - an integer representing the timezonOffset for a timezone
    * @returns {object} - and object formatted like that in the example
    */
    obj.convertFromIntervalString = function (intervalString, timezoneOffset) {
        var defaultTimezone = localStorage.getItem("temporalConstraintsDefaultTimezone"),
            intervalStart = "",
            intervalEnd = "",
            start = "",
            end = "",
            returnValue;

        if (intervalString.split("/").length === 2) {
            intervalStart = intervalString.split("/")[0];
            intervalEnd = intervalString.split("/")[1];
        }

        if (timezoneOffset == null && defaultTimezone) {
            timezoneOffset = this.getTimezoneOffset(defaultTimezone, new Date(intervalStart));
        } else if (timezoneOffset == null) {
            timezoneOffset = new Date().getTimezoneOffset();
        }

        if (intervalStart.length) {
            start = moment(intervalStart).zone(timezoneOffset);
        }

        if (intervalEnd.length) {
            end = moment(intervalEnd).zone(timezoneOffset);
        }

        returnValue = {
            start: start.format(format),
            end: end.format(format)
        };

        return returnValue;
    };

    /*
    * This function takes in an two human readable dates
    * ex: obj.convertToIntervalString("04/25/2016 12:00 AM", "04/30/2016 12:00 AM"),
    * converts them into ISO formatted date strings,
    * and puts them together into an interval string format
    * ex:
    *    "2016-04-25T07:00:00.000Z/2016-04-30T07:00:00.000Z"
    *
    * @param {string} intervalStart - human readable startDate
    * @param {string} intervalEnd - human readable endDate
    * @param {number} timezoneOffset - an integer representing the timezonOffset for a timezone
    * @returns {string} - a string formatted like that in the example
    */
    obj.convertToIntervalString = function (intervalStart, intervalEnd, timezoneOffset) {
        var defaultTimezone = localStorage.getItem("temporalConstraintsDefaultTimezone"),
            start = new Date(),
            end = new Date(),
            intervalString = "";

        if (timezoneOffset == null && defaultTimezone) {
            timezoneOffset = this.getTimezoneOffset(defaultTimezone, new Date(intervalStart));
        } else if (timezoneOffset == null) {
            timezoneOffset = new Date().getTimezoneOffset();
        }

        if (intervalStart.length) {
            start = moment.utc(intervalStart, format).add(timezoneOffset, 'minutes');
        }

        if (intervalEnd.length) {
            end = moment.utc(intervalEnd, format).add(timezoneOffset, 'minutes');
        }

        intervalString = start.toISOString() + "/" + end.toISOString();

        return intervalString;
    };

    /*
    * This function takes a jquery object representing a temporal constraints form
    * and returns an array of temporal constraint objects
    *
    * @param {obj} el - jquery object
    * @param {boolean} usePreviousTimezone - boolean that tells this function what timezone to use for it's calculation
    * @returns {array} - an array of temporal constraints
    */
    obj.getTemporalConstraintsValue = function (el, usePreviousTimezone) {
        var constraints = el.find(".temporalConstraint");

        return _.map(constraints, (constraint) => {
            var startDate = $(constraint).find(".temporalConstraintStartDate").val(),
                endDate = $(constraint).find(".temporalConstraintEndDate").val(),
                timezone = $(constraint).find(".temporalConstraintTimezone").val(),
                previousTimezone = $(constraint).find(".temporalConstraintTimezone").attr("previousTimezone"),
                timezoneOffset,
                intervalString;

            if (usePreviousTimezone) {
                timezoneOffset = this.getTimezoneOffset(previousTimezone);
            } else {
                timezoneOffset = this.getTimezoneOffset(timezone);
            }

            intervalString = this.convertToIntervalString(startDate, endDate, timezoneOffset);

            return { duration: intervalString };
        });
    };

    /*
    * @returns {array} - an array of temporal timezone strings
    */
    obj.getTimezones = function () {
        return moment.tz.names();
    };

    /*
    * @returns {string} - a string representing the timezone in which the browser resides or the defaultTimezone saved in localStorage
    */
    obj.getDefaultTimezone = function () {
        var defaultTimezone = localStorage.getItem("temporalConstraintsDefaultTimezone");

        if (!defaultTimezone) {
            defaultTimezone = moment.tz.guess();
        }

        return defaultTimezone;
    };

    /*
    * @param {string} timezone - string representing a timezone
    * @param {date} timestamp - date object representing the datetime to be used for calculating a timezone
    * @returns {number} - an integer representing a timezone offset in minutes
    */
    obj.getTimezoneOffset = function (timezone, timestamp) {
        if (!timestamp) {
            timestamp = new Date();
        }

        return moment.tz.zone(timezone).offset(timestamp);
    };

    return obj;
});

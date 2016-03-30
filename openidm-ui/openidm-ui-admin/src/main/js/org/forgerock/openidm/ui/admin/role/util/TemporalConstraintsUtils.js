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

/*global define */

define("org/forgerock/openidm/ui/admin/role/util/TemporalConstraintsUtils", [
    "jquery",
    "underscore",
    "handlebars",
    "moment"
], function ($, _, Handlebars, moment) {
    var obj = {};

    /*
    * This function takes in an interval string (ex. "2016-04-25T07:00:00.000Z/2016-04-30T07:00:00.000Z"),
    * splits it into two parts (start and end), converts the two ISO formatted date strings into a human readable format,
    * and returns an object
    * ex:
    *    { start: "04/25/2016 12:00:00 AM", end: "04/30/2016 12:00:00 AM"}
    *
    * @param {string} intervalString - a string containing two interval parts separated by "/"
    * @returns {object} - and object formatted like that in the example
    */
    obj.convertFromIntervalString = function (intervalString) {
          var intervalStart = "",
              intervalEnd = "",
              start = "",
              end = "";

          if (intervalString.split("/").length === 2) {
              intervalStart = intervalString.split("/")[0];
              intervalEnd = intervalString.split("/")[1];
          }

          if (intervalStart.length) {
              start = moment(intervalStart);
          }

          if (intervalEnd.length) {
              end = moment(intervalEnd);
          }

          return {
              start: start.format('MM/DD/YYYY h:mm:ss A'),
              end: end.format('MM/DD/YYYY h:mm:ss A')
          };
    };

    /*
    * This function takes in an two human readable dates
    * ex: obj.convertToIntervalString("04/25/2016 12:00:00 AM", "04/30/2016 12:00:00 AM"),
    * converts them into ISO formatted date strings,
    * and puts them together into an interval string format
    * ex:
    *    "2016-04-25T07:00:00.000Z/2016-04-30T07:00:00.000Z"
    *
    * @param {string} intervalStart - human readable startDate
    * @param {string} intervalEnd - human readable endDate
    * @returns {string} - a string formatted like that in the example
    */
    obj.convertToIntervalString = function (intervalStart, intervalEnd) {
         var start = new Date(),
            end = new Date(),
            intervalString = "";

         if (intervalStart.length) {
            start = new Date(intervalStart);
         }

         if (intervalEnd.length) {
            end = new Date(intervalEnd);
         }

         intervalString = start.toISOString() + "/" + end.toISOString();

         return intervalString;
    };

    /*
    * This function takes a jquery object representing a temporal constraints form
    * and returns an array of temporal constraint objects
    *
    * @param {obj} el - jquery object
    * @returns {array} - an array of temporal constraints
    */
    obj.getTemporalConstraintsValue = function (el) {
        var constraints = el.find(".temporalConstraint");

        return _.map(constraints, function (constraint) {
            var startDate = $(constraint).find(".temporalConstraintStartDate").val(),
                endDate = $(constraint).find(".temporalConstraintEndDate").val(),
                intervalString = obj.convertToIntervalString(startDate, endDate);

            return { duration: intervalString };
        });
    };

    return obj;
});

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
    "underscore"
], function ($, _) {
    var obj = {};

    /*
    * This function takes in a table row click event and returns the index of the clicked row
    *
    * @param {object} event - a click event
    * @returns {number} - the index of the clicked table row
    */
    obj.getClickedRowIndex = function (e) {
        var index;

        _.each($(e.currentTarget).closest("table tbody").find("tr"), function(tr, i) {
            if (tr === $(e.currentTarget).closest("tr")[0]) {
                index = i;
            }
        });

        return index;
    };
    /*
    * This function takes a schema properties object, looks for nullable properties,
    * sets each property's type attribute to something like example => ["relationship", "null"]
    * in the case where nullable === true, then removes the "nullable" attribute because it is
    * not needed when it is saved in managed.json.
    *
    * @param {object} properties - schema properties object each possibly having the "nullable" attribute
    * @returns {object} - adjusted schema properties object
    */
    obj.setNullableProperties = function(properties) {
        //check for nullable properties and add "null" to an array of types
        _.each(properties, (property) => {
            if (property.nullable) {
                property.type = [property.type, "null"];
            }

            delete property.nullable;
        });

        return properties;
    };
    /*
    * This function takes a schema properties object, looks for properties which have the type attribute
    * set to an array with "null" being one of the values in the array, then sets those properties to
    * nullable = true and type = $(theFirstNotNullValueInTheTypeArray).
    *
    * @param {object} properties - schema properties object each possibly having the "nullable" attribute
    * @returns {object} - adjusted schema properties object
    */
    obj.getNullableProperties = function(properties) {
        _.each(properties, (property) => {
            if (_.isArray(property.type) && _.indexOf(property.type,"null") > -1) {
                property.nullable = true;
                property.type = _.pull(property.type, "null")[0];
            }
        });

        return properties;
    };

    return obj;
});

/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All rights reserved.
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

define("org/forgerock/openidm/ui/util/delegates/CountryStateDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager"
], function(constants, AbstractDelegate, configuration, eventManager) {

    var obj = new AbstractDelegate(constants.host + "/openidm/config/ui/countries");

    obj.getAllCountries = function(successCallback, errorCallback) {
        var i;

        if (obj.pureCountries) {
            successCallback(obj.pureCountries);
        } else {

            obj.serviceCall({url: "", success: function(data) {
                if(successCallback) {
                    obj.pureCountries = data.countries;
                    obj.countries = {};
                    for (i = 0; i < data.countries.length; i++) {
                        obj.countries[data.countries[i].key] = data.countries[i];
                    }

                    successCallback(data.countries);
                }
            }, error: errorCallback} );
        }
    };

    obj.getAllStatesForCountry = function(countryKey, successCallback, errorCallback) {
        var i;

        if (obj.countries) {
            successCallback(obj.countries[countryKey].states);
        } else {
            obj.getAllCountries( function(countries) {
                successCallback(obj.countries[countryKey].states);
            });
        }
    };

    return obj;
});

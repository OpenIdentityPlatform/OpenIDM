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
 * Copyright 2014-2016 ForgeRock AS.
 */

define("org/forgerock/commons/ui/common/main/i18nManager", ["jquery", "lodash", "require", "handlebars", "i18next", "module", "org/forgerock/commons/ui/common/util/CookieHelper"], function ($, _, require, Handlebars, i18next, Module, CookieHelper) {

    var obj = {};

    /**
     * Initialises the i18next module.
     *
     * Takes the following options: serverLang, paramLang, defaultLang, and nameSpace.
     * i18nManger with i18next will try to detect the user language and load the corresponding translation in the
     * following order:
     * 1) paramLang which is a query string parameter (&locale=fr).
     * 2) serverLang, a 2-5 character long language or locale code passed in from server. The value can be "en" or
     * "en-US" for example.
     * 3) defaultLang will be the default language set inside the Constants.DEFAULT_LANGUAGE.
     *
     * Note that the "load" field controls how the localization files are resolved:
     * 1) current: always use the value that was passed in as "lang" (may be just "en", or "en-US")
     * 2) unspecific: always use the non country-specific locale (so "en" in case lang was "en-US")
     * 3) not set/other value: country-specific first, then non-country specific
     *
     * @param {object} options
     * @param {string} options.paramLang which is a query string parameter, optionally space separated, (&locale=zh fr).
     * @param {string} options.serverLang, a 2 digit language code passed in from server.
     * @param {string} options.defaultLang will be the default language set inside the Constants.DEFAULT_LANGUAGE.
     * @param {string} [options.nameSpace] The nameSpace is optional and will default to "translation"
     */
    obj.init = function (options) {

        var locales = [],
            opts = {},
            overrideLang = {},
            nameSpace = options.nameSpace ? options.nameSpace : "translation";

        overrideLang.locale = options.paramLang.locale || CookieHelper.getCookie("i18next");

        if (overrideLang.locale) {
            locales = overrideLang.locale.split(" ");
            options.serverLang = locales.shift();
        }
        if (options.defaultLang) {
            locales.push(options.defaultLang);
        }

        // return if the stored lang matches the new one.
        if (obj.lang && obj.lang === options.serverLang) {
            return $.Deferred().resolve();
        }
        obj.lang = options.serverLang;

        opts = {
            fallbackLng: locales,
            detectLngQS: "locale",
            getAsync: false,
            useCookie: true,
            lng: options.serverLang,
            load: Module.config().i18nLoad || "current",
            ns: nameSpace,
            nsseparator: ":::",
            resGetPath: require.toUrl("locales/__lng__/__ns__.json")
        };

        /**
         * Handlebars parameterized translation helper
         * @param {Object|String} [options] Object or String to pass to this function.
         * @param {Boolean} [options.hash.safeString] If set to false the returned string will be html character encoded
         * @returns {Object|String} returns a translation object or string if safeString was set to false.
         * @example
         * 1) In translation file define a value under "key.to.my.translation.string" key,
         *    e.g. "Display __foo__ and __bar__"
         * 2) call helper function with key value pairs: {{t "key.to.my.translation.string" foo="test1" bar="test2"}}
         * 3) Resulting string will be "Display test1 and test2"
         */
        Handlebars.registerHelper("t", function (key, options) {
            options = options || {};
            if (_.get(options.hash, "safeString") !== false) {
                return new Handlebars.SafeString(i18next.t(key, options.hash));
            } else {
                // TODO: OPENAM-9618 The safeString check must remain until we use
                // triple handlebars syntax for safe strings.
                return i18next.t(key, options.hash);
            }
        });

        /**
         * @param {object} map Each key in the map is a locale, each value is a string in that locale
         * @example
            {{mapTranslate map}} where map is an object like so:
            {
                "en_GB": "What's your favorite colour?",
                "fr": "Quelle est votre couleur préférée?",
                "en": "What's your favorite color?"
            }
        */
        Handlebars.registerHelper("mapTranslate", function (map) {
            var fallback;
            if (_.has(map, i18next.options.lng)) {
                return new Handlebars.SafeString(map[i18next.options.lng]);
            } else {
                fallback = _.find(i18next.options.fallbackLng, function (lng) {
                    return _.has(map, lng);
                });
                return new Handlebars.SafeString(map[fallback]);
            }
        });

        return i18next.init(opts);
    };

    return obj;
});

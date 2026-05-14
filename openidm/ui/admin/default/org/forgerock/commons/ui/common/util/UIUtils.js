"use strict";

var _typeof = typeof Symbol === "function" && typeof Symbol.iterator === "symbol" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; };

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

define(["jquery", "underscore", "require", "handlebars", "i18next", "ThemeManager", "org/forgerock/commons/ui/common/main/AbstractConfigurationAware", "org/forgerock/commons/ui/common/util/ModuleLoader", "org/forgerock/commons/ui/common/main/Router"], function ($, _, require, Handlebars, i18next, ThemeManager, AbstractConfigurationAware, ModuleLoader, Router) {

    /**
     * @exports org/forgerock/commons/ui/common/util/UIUtils
     */
    var obj = new AbstractConfigurationAware();

    obj.templates = {};

    function fetchTemplate(url) {
        return $.ajax({ type: "GET", url: require.toUrl(url), dataType: "html" });
    }

    function fetchAndSaveTemplate(urlToFetch, urlToSave) {
        return fetchTemplate(urlToFetch).done(function (template) {
            obj.templates[urlToSave] = template;
        });
    }

    function fetchAndCompileTemplate(urlToFetch, urlToSave, data) {
        return fetchTemplate(urlToFetch).then(function (template) {
            if (data !== "unknown" && data !== null) {
                obj.templates[urlToSave] = template;
                template = Handlebars.compile(template)(data);
            }
            return template;
        });
    }

    function registerPartial(name, url) {
        return fetchTemplate(url).then(function (data) {
            Handlebars.registerPartial(name, Handlebars.compile(data));
        });
    }

    /**
     * Renders the template.
     * @param {String} templateUrl - template url.
     * @param {JQuery} el - element, in which the template should be rendered.
     * @param {Object} data - template will be compiled with this data.
     * @param {Function} callback - callback to be called after template is rendered.
     * @param {String} mode - "append" means the template will be appended, provide any other value for
     *                        replacing current contents of the element.
     * @param {Function} validation - validation function.
     */
    obj.renderTemplate = function (templateUrl, el, data, callback, mode, validation) {
        return obj.compileTemplate(templateUrl, data).then(function validateAndRender(template) {
            if (validation && !validation()) {
                return false;
            }

            if (mode === "append") {
                el.append(template);
            } else {
                el.html(template);
            }

            if (callback) {
                callback();
            }
        });
    };

    /**
     * @deprecated
     * @see Use {@link module:org/forgerock/commons/ui/common/util/UIUtils.compileTemplate}
     */
    obj.fillTemplateWithData = function (templateUrl, data, compileCallback) {
        return obj.compileTemplate(templateUrl, data).then(compileCallback);
    };

    /**
     * Compiles template and returns the result of compilation.
     * @param {String} templateUrl - template url.
     * @param {Object} data - template will be compiled with this data.
     * @returns {Promise} compiled template wrapped in a promise
     */
    obj.compileTemplate = function (templateUrl, data) {
        if (templateUrl) {
            return ThemeManager.getTheme().then(function (theme) {
                var templateUrlWithPath = theme.path + templateUrl,
                    templateSavedPath = theme.path ? templateUrlWithPath : templateUrl;

                if (obj.templates[templateSavedPath]) {
                    return Handlebars.compile(obj.templates[templateSavedPath])(data);
                } else if (theme.path) {
                    return fetchAndCompileTemplate(templateUrlWithPath, templateUrlWithPath, data).then(null, function fallBackToDefaultPath() {
                        console.log(templateUrlWithPath + " was not found. Trying " + templateUrl);
                        return fetchAndCompileTemplate(templateUrl, templateUrlWithPath, data);
                    });
                } else {
                    return fetchAndCompileTemplate(templateUrl, templateUrl, data);
                }
            });
        } else {
            return $.Deferred().resolve("");
        }
    };

    /**
    * Preloads templates for their later usage.
    * @param {(String|String[])} urls - Urls to be preloaded, can be either a string or an array.
    */
    obj.preloadTemplates = function (urls) {
        if (typeof urls === "string") {
            urls = [urls];
        }

        return ThemeManager.getTheme().then(function (theme) {
            var promises = [];

            if (theme.path) {
                _.each(urls, function (templateUrl) {
                    var urlWithPath = theme.path + templateUrl;
                    promises.push(fetchAndSaveTemplate(urlWithPath, urlWithPath).then(null, function fallBackToDefaultPath() {
                        console.log(urlWithPath + " was not found. Trying " + templateUrl);
                        promises.push(fetchAndSaveTemplate(templateUrl, urlWithPath));
                    }));
                });
            } else {
                _.each(urls, function (templateUrl) {
                    promises.push(fetchAndSaveTemplate(templateUrl, templateUrl));
                });
            }

            return $.when.apply($, promises);
        });
    };

    /**
     * Loads all the templates defined in the "templateUrls" attribute of this module's configuration.
     */
    obj.preloadInitialTemplates = function () {
        obj.preloadTemplates(obj.configuration.templateUrls);
    };

    /**
     * Loads a Handlebars partial.
     * <p>
     * The registered name for the partial is inferred from the URL specified. e.g.
     * "partials/headers/_Title.html" => "headers/_Title"
     * <p>
     * Will not reload and register partials that are already loaded and registered
     * @param {String} url URL of partial to load in the format "partials/<path_to_partial>.html"
     * @return {Promise.<Object>} Load promise
     */
    obj.preloadPartial = function (url) {
        var name = url.replace(/(^partials\/)|(\.html$)/g, "");

        return ThemeManager.getTheme().then(function (theme) {
            if (Handlebars.partials[name]) {
                return;
            } else if (theme.path) {
                return registerPartial(name, theme.path + url).then(null, function fallBackToDefaultPath() {
                    console.log(theme.path + url + " was not found. Trying " + url);
                    return registerPartial(name, url);
                });
            } else {
                return registerPartial(name, url);
            }
        });
    };

    /**
     * Loads all the Handlebars partials defined in the "partialUrls" attribute of this module's configuration
     */
    obj.preloadInitialPartials = function () {
        _.each(obj.configuration.partialUrls, function (url) {
            obj.preloadPartial(url);
        });
    };

    $.fn.emptySelect = function () {
        return this.each(function () {
            if (this.tagName === "SELECT") {
                this.options.length = 0;
            }
        });
    };

    $.fn.loadSelect = function (optionsDataArray) {
        return this.emptySelect().each(function () {
            if (this.tagName === "SELECT") {
                var i,
                    option,
                    selectElement = this;
                for (i = 0; i < optionsDataArray.length; i++) {
                    option = new Option(optionsDataArray[i].value, optionsDataArray[i].key);
                    selectElement.options[selectElement.options.length] = option;
                }
            }
        });
    };

    $.event.special.delayedkeyup = {
        setup: function setup() {
            $(this).bind("keyup", $.event.special.delayedkeyup.handler);
        },

        teardown: function teardown() {
            $(this).unbind("keyup", $.event.special.delayedkeyup.handler);
        },

        handler: function handler(event) {
            var self = this,
                args = arguments;

            event.type = "delayedkeyup";

            $.doTimeout("delayedkeyup", 250, function () {
                $.event.handle.apply(self, args);
            });
        }
    };

    //map should have format key : value
    Handlebars.registerHelper("selectm", function (map, elementName, selectedKey, selectedValue, multiple, height) {
        var result,
            prePart,
            postPart,
            content = "",
            isSelected,
            entityName;

        prePart = "<select";

        if (elementName && _.isString(elementName)) {
            prePart += ' name="' + elementName + '"';
        }

        if (multiple) {
            prePart += ' multiple="multiple"';
        }

        if (height) {
            prePart += ' style="height: ' + height + 'px"';
        }

        prePart += '>';

        postPart = '</select> ';

        for (entityName in map) {
            isSelected = false;
            if (selectedValue && _.isString(selectedValue)) {
                if (selectedValue === map[entityName]) {
                    isSelected = true;
                }
            } else if (selectedKey && selectedKey === entityName) {
                isSelected = true;
            }

            if (isSelected) {
                content += '<option value="' + entityName + '" selected="true">' + $.t(map[entityName]) + '</option>';
            } else {
                content += '<option value="' + entityName + '">' + $.t(map[entityName]) + '</option>';
            }
        }

        result = prePart + content + postPart;
        return new Handlebars.SafeString(result);
    });

    /**
     * Use this helper around a basic select to automatically
     * mark the option corresponding to the provided value as selected.
     *
     * @example JS
     *  this.data.mimeType = "text/html";
     *
     * @example HTML
     *  <select>
     *      {{#staticSelect mimeType}}
     *      <option value="text/html">text/html</option>
     *      <option value="text/plain">text/plain</option>
     *      {{/staticSelect}}
     *  </select>
     */
    Handlebars.registerHelper("staticSelect", function (value, options) {
        var selected = $("<select />").html(options.fn(this));
        if (typeof value !== "undefined" && value !== null) {
            selected.find("[value=\'" + value.toString().replace("'", "\\'") + "\']").attr({ "selected": "selected" });
        }
        return selected.html();
    });

    Handlebars.registerHelper('select', function (map, elementName, selectedKey, selectedValue, additionalParams) {
        var result,
            prePart,
            postPart,
            content = "",
            isSelected,
            entityName,
            entityKey;

        if (map && _.isString(map)) {
            map = JSON.parse(map);
        }

        if (elementName && _.isString(elementName)) {
            prePart = '<select name="' + elementName + '" ' + additionalParams + '>';
        } else {
            prePart = '<select>';
        }

        postPart = '</select> ';

        for (entityName in map) {
            isSelected = false;
            if (selectedValue && _.isString(selectedValue) && selectedValue !== '') {
                if (selectedValue === map[entityName]) {
                    isSelected = true;
                }
            } else if (selectedKey && selectedKey !== '' && selectedKey === entityName) {
                isSelected = true;
            }

            if (entityName === '__null') {
                entityKey = '';
            } else {
                entityKey = entityName;
            }

            if (isSelected) {
                content += '<option value="' + entityKey + '" selected="true">' + $.t(map[entityName]) + '</option>';
            } else {
                content += '<option value="' + entityKey + '">' + $.t(map[entityName]) + '</option>';
            }
        }

        result = prePart + content + postPart;
        return new Handlebars.SafeString(result);
    });

    Handlebars.registerHelper('p', function (countValue, options) {
        var params, result;
        params = { count: countValue };
        result = i18next.t(options.hash.key, params);
        return new Handlebars.SafeString(result);
    });

    /**
     * @description A handlebars helper checking the equality of two provided parameters, if
     *      the parameters are not equal and there is an else block, the else block will be rendered.
     *
     * @example:
     *
     * {{#equals "testParam" "testParam"}}
     *      <span>Equals Block!</span>
     * {{else}}
     *      <span> Not Equals Block!</span>
     * {{/equals}}
     */
    Handlebars.registerHelper('equals', function (val, val2, options) {
        if (val === val2) {
            return options.fn(this);
        } else {
            return options.inverse(this);
        }
    });

    Handlebars.registerHelper('checkbox', function (map, name) {
        var ret = "<div class='checkboxList' id='" + name + "'><ol>",
            idx,
            sortedMap = _.chain(map).pairs().sortBy(function (arr) {
            return arr[1];
        }).value();

        for (idx = 0; idx < sortedMap.length; idx++) {
            ret += '<li><input type="checkbox" name="' + name + '" value="' + sortedMap[idx][0] + '" id="' + name + '_' + encodeURIComponent(sortedMap[idx][0]) + '"><label for="' + name + '_' + encodeURIComponent(sortedMap[idx][0]) + '">' + sortedMap[idx][1] + '</label></li>';
        }

        ret += "</ol></div>";

        return new Handlebars.SafeString(ret);
    });

    Handlebars.registerHelper('siteImages', function (images) {
        var ret = "",
            i;

        for (i = 0; i < images.length; i++) {
            ret += '<img class="item" src="' + encodeURI(images[i]) + '" data-site-image="' + encodeURI(images[i]) + '" />';
        }

        return new Handlebars.SafeString(ret);
    });

    Handlebars.registerHelper("each_with_index", function (array, fn) {
        var buffer = "",
            item,
            k = 0,
            i = 0,
            j = 0;

        for (i = 0, j = array.length; i < j; i++) {
            if (array[i]) {
                item = {};
                item.value = array[i];

                // stick an index property onto the item, starting with 0
                item.index = k;

                item.first = k === 0;
                item.last = k === array.length;

                // show the inside of the block
                buffer += fn.fn(item);

                k++;
            }
        }

        // return the finished buffer
        return buffer;
    });

    Handlebars.registerHelper('camelCaseToTitle', function (string) {
        var newString = string.replace(/([a-z])([A-Z])/g, '$1 $2');
        return new Handlebars.SafeString(newString[0].toUpperCase() + newString.slice(1));
    });

    Handlebars.registerHelper('stringify', function (string, spaces) {
        spaces = spaces ? spaces : 0;
        var newString = JSON.stringify(string, null, spaces);
        return newString;
    });

    Handlebars.registerHelper('ifObject', function (item, options) {
        if ((typeof item === "undefined" ? "undefined" : _typeof(item)) === 'object') {
            return options.fn(this);
        } else {
            return options.inverse(this);
        }
    });

    /**
     * Handlebars 'urlTo' helper
     * @returns {String} fragment from a given route key after applying arguments or default values.
     *
     * @example {{urlTo 'login'}} will return "#login"
     */
    Handlebars.registerHelper("urlTo", function (routeKey) {
        var args = [].concat(arguments[1]);
        // Don't return a safe string to prevent XSS.
        return "#" + Router.getLink(Router.configuration.routes[routeKey], args);
    });

    /**
     * Handlebars 'routeTo' helper
     * Creates a routing hash will all arguments passed through #encodeURIComponent
     */
    Handlebars.registerHelper('routeTo', function (routeKey) {
        var result = '#',
            args = _.toArray(arguments).slice(1, -1);
        args = _.map(args, function (arg) {
            return encodeURIComponent(arg);
        });

        result += Router.getLink(Router.configuration.routes[routeKey], args);

        return new Handlebars.SafeString(result);
    });

    /**
     * Handlebars "partial" helper
     * @example
     * {{partial this.partialName this}}
     */
    Handlebars.registerHelper("partial", function (name, context) {
        var partial = Handlebars.partials[name];

        if (!partial) {
            console.error("Handlebars \"partial\" helper unable to find partial \"" + name + "\"");
        } else {
            return new Handlebars.SafeString(partial(context));
        }
    });

    obj.loadSelectOptions = function (data, el, empty, callback) {
        if (empty === undefined || empty === true) {
            data = [{
                "key": "",
                "value": $.t("common.form.pleaseSelect")
            }].concat(data);
        }

        el.loadSelect(data);

        if (callback) {
            callback(data);
        }
    };

    //This function exists to catch any legacy jqConfirms.
    //Once completly updated across the applications this function can be removed.
    obj.jqConfirm = function (message, confirmCallback) {
        this.confirmDialog(message, "default", confirmCallback);
    };

    /**
     * @param {string} message The text provided in the main body of the dialog
     * @param {string} type The type of dialog to display
     * @param {Function} confirmCallback Fired when the confirm button is clicked
     * default
     * info
     * primary
     * success
     * warning
     * danger
     *
     * @example
     *  UIUtils.confirmDialog($.t("templates.admin.ResourceEdit.confirmDelete"), "danger",s _.bind(function(){
     *      //Useful stuff here
     *  }, this));
     */
    obj.confirmDialog = function (message, type, confirmCallback) {
        ModuleLoader.load("bootstrap-dialog").then(function (BootstrapDialog) {
            var btnType = "btn-" + type;

            if (type === "default") {
                btnType = "btn-primary";
            }

            BootstrapDialog.show({
                title: $.t('common.form.confirm'),
                type: "type-" + type,
                message: message,
                id: "frConfirmationDialog",
                buttons: [{
                    label: $.t('common.form.cancel'),
                    id: "frConfirmationDialogBtnClose",
                    action: function action(dialog) {
                        dialog.close();
                    }
                }, {
                    label: $.t('common.form.ok'),
                    cssClass: btnType,
                    id: "frConfirmationDialogBtnOk",
                    action: function action(dialog) {
                        if (confirmCallback) {
                            confirmCallback();
                        }
                        dialog.close();
                    }
                }]
            });
        });
    };

    obj.responseMessageMatch = function (error, string) {
        var responseMessage = JSON.parse(error).message;
        return responseMessage.indexOf(string) > -1;
    };

    // Registering global mixins

    _.mixin({

        /**
         * findByValues takes a collection and returns a subset made up of objects where the given property name
         * matches a value in the list.
         * @returns {Array} subset of made up of {Object} where there is no match between the given property name and
         *                  the values in the list.
         * @example
         *
         *    var collections = [
         *        {id: 1, stack: 'am'},
         *        {id: 2, stack: 'dj'},
         *        {id: 3, stack: 'idm'},
         *        {id: 4, stack: 'api'},
         *        {id: 5, stack: 'rest'}
         *    ];
         *
         *    var filtered = _.findByValues(collections, "id", [1,3,4]);
         *
         *    filtered = [
         *        {id: 1, stack: 'am'},
         *        {id: 3, stack: 'idm'},
         *        {id: 4, stack: 'api'}
         *    ]
         *
         */
        "findByValues": function findByValues(collection, property, values) {
            return _.filter(collection, function (item) {
                return _.contains(values, item[property]);
            });
        },

        /**
         * Returns subset array from a collection
         * @returns {Array} subset of made up of {Object} where there is no match between the given property name and
         *                  the values in the list.
         * @example
         *
         *    var filtered = _.removeByValues(collections, "id", [1,3,4]);
         *
         *    filtered = [
         *        {id: 2, stack: 'dj'},
         *        {id: 5, stack: 'rest'}
         *    ]
         *
         */
        "removeByValues": function removeByValues(collection, property, values) {
            return _.reject(collection, function (item) {
                return _.contains(values, item[property]);
            });
        },

        /**
         * isUrl checks to see if string is a valid URL
         * @returns {Boolean}
         */
        "isUrl": function isUrl(string) {
            var regexp = /(http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/;
            return regexp.test(string);
        }

    });

    return obj;
});

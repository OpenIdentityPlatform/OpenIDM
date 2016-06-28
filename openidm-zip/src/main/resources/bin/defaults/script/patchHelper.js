/*
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

/**
 * Used to perform transform operations (add/remove) against the objects array.
 * @module patchHelper
 * @see managed
 */

(function () {
    function FluentObjectArrayPatcher(array) {
        this.content = array;

        this.matchField = function(field, value) {
            this._matchField = field;
            this._matchValue = value;

            return this;
        }

        const self = this;
        var matches = function(item) {
            if (self._matchField) {
                return item[self._matchField] === self._matchValue;
            } else {
                true;
            }
        }

        this.replace = function(field, value) {
            for (var i = 0; i < this.content.length; i++) {
                var item = this.content[i];

                if (matches(item)) {
                    index(item, field, value);
                }
            }

            return this.content;
        }

        this.add = function(field, value) {
            return this.replace(field, value);
        }

        this.remove = function(field) {
            for (var i = 0; i < this.content.length; i++) {
                var item = this.content[i];

                if (matches(item)) {
                    var nodes = field.split(".");
                    var obj = item;
                    for (var n = 0; n < nodes.length - 1; n++) {
                        obj = obj[nodes[n]];
                    }
                    delete obj[nodes[nodes.length - 1]];

//                    delete item.content[field];
                }
            }

            return this.content;
        }
    }

    /**
     * Patch an array of objects
     */
    exports.objectArray = function(array) {
        return new FluentObjectArrayPatcher(array);
    }

    // Convert JavaScript string in dot notation into an object reference
    function index(obj, is, value) {
        if (typeof is == 'string')
            return index(obj, is.split('.'), value);
        else if (is.length == 1 && value !== undefined)
            return obj[is[0]] = value;
        else if (is.length == 0)
            return obj;
        else
            return index(obj[is[0]], is.slice(1), value);
    }
}());

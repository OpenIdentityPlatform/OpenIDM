/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS.
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

/*global, define, _*/

define("org/forgerock/openidm/ui/admin/util/FilterEvaluator", [
], function () {
    return {
        getValueFromJSONPointer: function (pointer, object) {
            var parts = pointer.split('/');
            if (parts[0] === "") {
                parts = parts.splice(1);
            }
            return _.reduce(parts, function (entry, key) {
                return entry[key];
            }, object);
        },
        evaluate: function (filter, object) {
            switch (filter.op) {
                case "none":
                    // no filter means everything evaluates to true
                    return true;
                case "and":
                    return _.reduce(filter.children, function (currentResult, child) {
                        if (currentResult) { // since this is "and" we can short-circuit evaluation by only continuing to evaluate if we haven't yet hit a false result
                            return this.evaluate(child, object);
                        } else {
                            return currentResult;
                        }
                    }, true, this);
                case "or":
                    return _.reduce(filter.children, function (currentResult, child) {
                        if (!currentResult) { // since this is "or" we can short-circuit evaluation by only continuing to evaluate if we haven't yet hit a true result
                            return this.evaluate(child, object);
                        } else {
                            return currentResult;
                        }
                    }, false, this);
                case "expr":
                    switch (filter.tag) {
                        case "equalityMatch":
                            return this.getValueFromJSONPointer(filter.name, object) === filter.value;
                        case "ne":
                            return this.getValueFromJSONPointer(filter.name, object) !== filter.value;
                        case "approxMatch":
                            return this.getValueFromJSONPointer(filter.name, object).indexOf(filter.value) === 0;
                        case "co":
                            return this.getValueFromJSONPointer(filter.name, object).indexOf(filter.value) !== -1;
                        case "greaterOrEqual":
                            return this.getValueFromJSONPointer(filter.name, object) >= filter.value;
                        case "gt":
                            return this.getValueFromJSONPointer(filter.name, object) > filter.value;
                        case "lessOrEqual":
                            return this.getValueFromJSONPointer(filter.name, object) <= filter.value;
                        case "lt":
                            return this.getValueFromJSONPointer(filter.name, object) < filter.value;
                    }
                    break;
            }
        }
    };
});

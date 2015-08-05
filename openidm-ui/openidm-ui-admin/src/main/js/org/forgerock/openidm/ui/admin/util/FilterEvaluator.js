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

/*global define */

define("org/forgerock/openidm/ui/admin/util/FilterEvaluator", [
    "underscore"
], function (_) {
    return {
        getValueFromJSONPointer: function (pointer, object) {
            var parts = pointer.split('/');
            if (parts[0] === "") {
                parts = parts.splice(1);
            }
            return _.reduce(parts, function (entry, key) {
                return entry ? entry[key] : undefined;
            }, object);
        },
        evaluate: function (filter, object) {
            var value;
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
                    value = this.getValueFromJSONPointer(filter.name, object);
                    switch (filter.tag) {
                        case "equalityMatch":
                            return value === filter.value;
                        case "ne":
                            return value !== filter.value;
                        case "approxMatch":
                            return value.indexOf(filter.value) === 0;
                        case "co":
                            return value.indexOf(filter.value) !== -1;
                        case "greaterOrEqual":
                            return value >= filter.value;
                        case "gt":
                            return value > filter.value;
                        case "lessOrEqual":
                            return value <= filter.value;
                        case "lt":
                            return value < filter.value;
                        case "pr":
                            return value !== null && value !== undefined;
                    }
                    break;
            }
        }
    };
});

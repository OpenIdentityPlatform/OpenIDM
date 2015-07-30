/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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
/**
 *  This code is derivative of the java QueryFilter Class in Commons
 *
 *  Sample Input(VALID):
 *      userName sw "user." AND (/roles/0 eq "openidm-admin" OR /roles/0 eq "openidm-authorized")
 *  QueryFilter -> QueryTree:
 *      [
 *          {field: "userName", not: false, operator: "sw", value: "user."},
 *          "and",
 *          [
 *              {field: "/roles/0", not: false, operator: "eq", value: "openidm-admin"},
 *              "or",
 *              {field: "/roles/0", not: false, operator: "eq", value: "openidm-authorized"}
 *          ]
 *      ]
 *  QueryTree -> QueryFilter:
 *      (userName sw "user." and (/roles/0 eq "openidm-admin" or /roles/0 eq "openidm-authorized"))
 *
 *
 *  Sample Input(INVALID QUERY):
 *      userName sw "user." AND (/roles/0 eq test "openidm-admin" OR /roles/0 eq "openidm-authorized")
 *  QueryFilter -> QueryTree:
 *      {error: true, message: "Bad format."}
 *  QueryTree -> QueryFilter:
 *      Object {error: true, message: "Cannot convert badly structured query filter tree."}
 */
define("org/forgerock/openidm/ui/admin/util/QueryFilterUtils", [
    "underscore"
], function(_) {

    var obj = {},
        VALUE_OF_MAX_DEPTH = 256;

    function ConversionError(message){
        this.message = message;
    }

    ConversionError.prototype = new Error();

    obj.convertTo = function(queryFilterTree) {
        function convertBranch(branch) {
            var filter = "";
            _.each(branch, function(node) {
                if (_.isString(node)) {
                    filter += " " + node + " ";

                } else if (_.isArray(node)) {
                    filter += "(" + convertBranch(node) + ")";

                } else if (_.isObject(node)) {
                    if (node.not) {
                        filter += "! (";
                        filter += node.field + " " + node.operator + " \"" + node.value + "\"";
                        filter += ")";

                    } else {
                        filter += node.field + " " + node.operator + " \"" + node.value + "\"";
                    }
                }
            });
            return filter;
        }

        if (_.has(queryFilterTree, "error")) {
            console.error("Cannot convert badly structured query filter tree.");
            return {"error": true, "message": "Cannot convert badly structured query filter tree."};

        } else {
            var queryFilter = convertBranch(queryFilterTree);
            return queryFilter;
        }
    };

    obj.convertFrom = function(queryFilter) {
        try {
            var tokenizer = new obj.FilterTokenizer(queryFilter),
                filter = obj.valueOfExpr(tokenizer, 0, false, "or");
            return filter;

        } catch (e) {
            console.error("There was an error converting the provided input.");
            return {"error": true, "message": e.message};
        }
    };

    obj.valueOfExpr = function(tokenizer, depth, not, type) {
        try {
            if (obj.checkDepth(tokenizer, depth)) {
                var nextFunction,
                    filter,
                    subFilters = [],
                    temp,
                    tokenTemp,
                    i;

                if (type === "or") {
                    nextFunction = obj.valueOfExpr;
                } else if (type === "and") {
                    nextFunction = obj.valueOfNotExpr;
                } else {
                    throw new ConversionError("Invalid operator.");
                }

                filter = nextFunction(tokenizer, depth + 1, not, "and");

                if (filter.length === 1) {
                    temp = [filter[0]];
                } else {
                    temp = [filter];
                }

                while (tokenizer.hasNext() && tokenizer.peek().toLowerCase() === type) {
                    tokenizer.next();
                    tokenTemp = nextFunction(tokenizer, depth + 1, not, "and");

                    if (_.isArray(tokenTemp) && tokenTemp.length === 1) {
                        tokenTemp = tokenTemp[0];
                    }

                    subFilters.push(tokenTemp);
                }

                if (subFilters.length > 0) {
                    if (subFilters.length === 1) {
                        temp.push(type);
                        temp.push(subFilters[0]);
                    } else {
                        for (i = 0; i < subFilters.length; i++) {
                            temp.push(type);
                            temp.push(subFilters[i]);
                        }
                    }
                }
                return temp;
            }
        } catch(e) {
            throw e;
        }
    };

    obj.valueOfNotExpr = function(tokenizer, depth, not) {
        try {
            if (obj.checkDepth(tokenizer, depth)) {
                if (tokenizer.hasNext() && tokenizer.peek().toLowerCase() === "!") {
                    tokenizer.next();
                    return obj.valueOfPrimaryExpr(tokenizer, depth + 1, true);
                } else {
                    return obj.valueOfPrimaryExpr(tokenizer, depth + 1, not);
                }
            }
        } catch (e) {
            throw e;
        }
    };

    obj.valueOfPrimaryExpr = function(tokenizer, depth, not) {
        var nextToken,
            pointer,
            assertionValue,
            operator,
            filter;

        try {

            if (obj.checkDepth(tokenizer, depth)) {
                if (!tokenizer.hasNext()) {
                    throw new ConversionError("Bad format.");
                }

                nextToken = tokenizer.next();

                if (nextToken === "(") {
                    // Nested expression.
                    filter = obj.valueOfExpr(tokenizer, depth + 1, not, "or");

                    if (!tokenizer.hasNext() || tokenizer.next() !== ")") {
                        throw new ConversionError("Bad format.");
                    }

                    return filter;

                } else if (nextToken === "\"") {
                    throw new ConversionError("Bad format.");

                } else {
                    pointer = nextToken;

                    if (!tokenizer.hasNext()) {
                        throw new ConversionError("Bad format.");
                    }

                    operator = tokenizer.next();
                    nextToken = tokenizer.next();

                    if (nextToken === "\"") {
                        if (!tokenizer.hasNext()) {
                            throw new ConversionError("Bad format.");
                        }

                        assertionValue = tokenizer.next();

                        if (!tokenizer.hasNext() || tokenizer.next() !== "\"") {
                            throw new ConversionError("Bad format.");
                        }

                    } else {
                        assertionValue = nextToken;
                    }

                    try {
                        return obj.comparisonFilter(pointer, operator, assertionValue, not);
                    } catch (error) {
                        throw new ConversionError("The arguments for the filter are bad.");
                    }

                }
            }
        } catch (e) {
            throw e;
        }
    };

    obj.comparisonFilter = function(field, operator, valueAssertion, not) {
        return ({
            field: field,
            operator: operator,
            value: valueAssertion,
            not: not || false
        });
    };

    obj.checkDepth = function(tokenizer, depth) {
        if (depth > VALUE_OF_MAX_DEPTH) {
            this.conversionError(tokenizer, "The query filter '" + tokenizer
                + "' cannot be parsed because it contains more than " + VALUE_OF_MAX_DEPTH
                + " nexted expressions");
        } else {
            return true;
        }
    };

    obj.FilterTokenizer = function(filterString) {
        this.constants = {
            NEED_END_STRING: 2,
            NEED_START_STRING: 1,
            NEED_TOKEN: 0
        };

        this.filterString = filterString;
        this.nextToken = null;
        this.pos = 0;
        this.state = this.constants.NEED_TOKEN;

        this.hasNext = function () {
            return this.nextToken !== null;
        };

        this.next = function () {
            var next = this.peek();
            this.readNextToken();
            return next;
        };

        this.peek = function () {
            if (this.nextToken === null) {
                return false;
            }
            return this.nextToken;
        };

        this.readNextToken = function () {
            var stringStart,
                tokenStart,
                c;

            switch (this.state) {
                case this.constants.NEED_START_STRING:
                    stringStart = this.pos;
                    while (this.pos < this.filterString.length && this.filterString[this.pos] !== '"') {
                        this.pos++;
                    }
                    this.nextToken = this.filterString.substring(stringStart, this.pos);
                    this.state = this.constants.NEED_END_STRING;
                    break;

                case this.constants.NEED_END_STRING:
                    if (this.pos < this.filterString.length) {
                        this.nextToken = this.filterString.substring(this.pos, ++this.pos);
                    } else {
                        this.nextToken = null;
                    }
                    this.state = this.constants.NEED_TOKEN;
                    break;

                default: // NEED_TOKEN:
                    if (!this.skipWhiteSpace()) {
                        this.nextToken = null;
                    } else {
                        tokenStart = this.pos;
                        switch (this.filterString[this.pos++]) {
                            case '(':
                            case ')':
                                break;
                            case '"':
                                this.state = this.constants.NEED_START_STRING;
                                break;
                            default:
                                for (; this.pos < this.filterString.length; this.pos++) {
                                    c = this.filterString[this.pos];

                                    if (c === '(' || c === ')' || c === ' ') {
                                        break;
                                    }
                                }
                                break;
                        }
                        this.nextToken = this.filterString.substring(tokenStart, this.pos);
                    }
            }
        };

        this.skipWhiteSpace = function () {
            while (this.pos < this.filterString.length && this.filterString[this.pos] === " ") {
                this.pos++;
            }
            return this.pos < this.filterString.length;
        };
        this.readNextToken();
    };

    return obj;
});

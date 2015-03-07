/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

 /*global source, linkQualifier, require */
 (function () {

    var syncConfig = openidm.read("config/sync"),
        _ = require('lib/lodash'),
        i = 0,
        expression = "",
        sourceObject = source,
        valueForTargetField = function (mapping, field) {
            var j = 0, p,
                conditionResult,
                source = {},
                returnValue;

            for (j = 0; j < mapping.properties.length; j++) {
                p = mapping.properties[j];
                if (p.target === field) {

                    if (typeof p.condition === "object" && p.condition !== null) {

                        // If this condition uses a linkQualifier to distinguish it, then make sure that the given qualifier
                        // matches the qualifier used to execute this script. Otherwise, skip it.
                        if (p.condition.linkQualifier !== undefined && 
                            typeof linkQualifier !== undefined &&
                            p.condition.linkQualifier !== linkQualifier) {
                            return "";
                        }

                        // If this is a condition script (assuming based on the presence of a "type" property) then
                        // evaluate that condition and decide whether or not to include it based on the result
                        if (typeof p.condition.type !== "undefined") {
                            if (openidm.action("script", "eval", _.extend(p.condition,
                                    {
                                        "globals": {
                                            "object": sourceObject,
                                            "linkQualifier": linkQualifier
                                        }
                                    })
                                ) !== true) {

                                return "";
                            }
                        }
                    }

                    if (typeof p.transform === "object" && p.transform.type === "text/javascript" &&
                            typeof (p.transform.source) === "string") {

                        if (typeof(p.source) !== "undefined" && p.source.length) {
                            source = sourceObject[p.source];
                        } else {
                            source = sourceObject;
                        }

                        // A failure to evaluate the script implies that the source script isn't a valid candidate for syncing;
                        // errors thrown as a result will prevent the sync from occurring.
                        try {
                            returnValue = eval(p.transform.source); // references to "source" variable expected within this string
                        } catch (e) {
                            throw { 
                                "code" : 500, 
                                "message" : "Unable to evaluate transformation script for field " + field,
                                "detail": {
                                    "message": "Unable to evaluate transformation script for field " + field
                                }
                            };
                        }
                        
                    } else if (typeof(p.source) !== "undefined" && p.source.length) {
                        
                        returnValue = sourceObject[p.source];
                        
                    }
                    
                    if (typeof(p["default"]) !== "undefined" && p["default"].length) {
                        
                        if (returnValue === null || returnValue === undefined) {
                            returnValue = p["default"];
                        }

                    }

                    if (returnValue) {
                        return returnValue.replace(/\\/g, "\\\\").replace(/'/g, "\\'");
                    } else {
                        return "";
                    }
                }
            }
            return "";
        },
        expressionParser = function (mapping, node) {
            var getResults = function (container) {
                    var j,tmp,resultArray = [];
                    for (j = 0; j<container.length; j++) {
                        if (typeof container[j] === "string") {
                            resultArray.push(container[j] + " eq \"" + valueForTargetField(mapping, container[j]) + "\"");
                        } else {
                            tmp = expressionParser(mapping, container[j]);
                            if (tmp && tmp.length) {
                                resultArray.push( "(" + tmp + ")");
                            }
                        }
                    }
                    return resultArray;
                };
            
            if (typeof node.any === "object") {
                return getResults(node.any).join(" OR ");
            } else if (typeof node.all === "object") {
                return getResults(node.all).join(" AND ");
            } else {
                return "";
            }

        };

    while (i < syncConfig.mappings.length) {
        if (syncConfig.mappings[i].name === mapping) {
            break;
        }
        i++;
    }

    if (i < syncConfig.mappings.length) {
        if (typeof expressionTree === "object" && expressionTree !== null) {
            expression = expressionParser(syncConfig.mappings[i], expressionTree);
        }
    }


    return {'_queryFilter': expression}
}());

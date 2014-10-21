/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
 */
 (function () {

    var syncConfig = openidm.read("config/sync"),
        i = 0,
        expression = "",
        sourceObject = source,
        valueForTargetField = function (mapping, field) {
            var j = 0, p,
                source = {},
                returnValue;

            for (j = 0; j < mapping.properties.length; j++) {
                p = mapping.properties[j];
                if (p.target === field) {

                    if (typeof(p.transform) === "object" && p.transform.type === "text/javascript" &&
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
                                "openidmCode" : 500, 
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

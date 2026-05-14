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

/**
 * A script for purging recon entries in the repository.
 * 
 * Configuration:
 * mappings: An array of mappings to prune. Each element in the array can either be a String or (optionally) an Object.  
 *     Strings must contain the mapping(s) name and can use "%" as a wild card value that will be used in a LIKE condition
 *     Objects provide the ability to specify mapping(s) to include/exclude and must be of the form:
 *     {
 *         "include" : "mapping1",
 *         "exclude" : "mapping2"
 *     }
 * purgeType: The type of purge to perform. There are two possible values "purgeByNumOfReconsToKeep" and "purgeByExpired"
 *     "purgeByNumOfReconsToKeep" uses the deleteFromAuditReconByNumOf function and the numOfRecons config variable
 *     "purgeByExpired" uses the deleteFromAuditReconByExpired function and the config variables intervalUnit and intervalValue
 * numOfRecons: The number of recon summary records to keep for a given mapping (this includes all child records).
 * intervalUnit: The type of time interval when using "purgeByExpired", acceptable values: "minutes", "hours", "days"
 * intervalValue: The value of the time interval when using "purgeByExpired", should be an integer value    
 */

(function (mappings, purgeType, numOfRecons, intervalUnit, intervalValue, scheduleName) {
    var i, authentication, results = [], obj = {};
    
    obj.getCutoffTimestamp = function() {
        var dateUtil, cur;
        
        // Get a DateUtil instance
        var dateUtil = org.forgerock.openidm.util.DateUtil.getDateUtil("GMT");
        
        // Get the current time
        var cur = dateUtil.currentDateTime();
        
        // Offset with the interval
        if (intervalUnit === 'minutes'){
            return cur.minusMinutes(intervalValue).toString();
        } else if (intervalUnit === 'hours'){
            return cur.minusHours(intervalValue).toString();
        } else if (intervalUnit === 'days'){
            return cur.minusDays(intervalValue).toString();
        } else {
            throw "Unsupported intervalUnit " + intervalUnit;
        }
    };
    
    obj.getReconsToKeep = function(includeMapping, excludeMapping) {
        var result, i, max, idList = [], idMap = {}, key;
        var query = {
                "_queryId": "get-recons",
                "includeMapping" : includeMapping,
                "excludeMapping" : excludeMapping
            };
            
        result = openidm.query("repo/audit/recon", query).result;
        
        for(i = 0; i < result.length; i++){
            if(typeof idMap[result[i].mapping] === "undefined"){
                idMap[result[i].mapping] = [];
            }

            if(idMap[result[i].mapping].length <= numOfRecons){
                idMap[result[i].mapping].push(result[i].reconId);
            }
        }
        
        for(key in idMap){
            max = (idMap[key].length < numOfRecons) ? idMap[key].length : numOfRecons;
            for (i = 0; i < max; i++){
                idList.push(idMap[key][i]);
            }
        }

        return idList.join(",");
    };
    
    obj.deleteFromAuditReconByExpired = function(includeMapping, excludeMapping) {
        var command = {
                "commandId": "purge-by-recon-expired", 
                "timestamp": obj.getCutoffTimestamp(),
                "includeMapping" : includeMapping,
                "excludeMapping" : excludeMapping
            };
        
        var r = openidm.action("repo/audit/recon", "command", {}, command);

        return r;
    };
    
    obj.deleteFromAuditReconByNumOf = function(includeMapping, excludeMapping) {
        var r, code, message, command = {
                "commandId": "purge-by-recon-number-of", 
                "numberOf": numOfRecons,
                "includeMapping" : includeMapping,
                "excludeMapping" : excludeMapping
            };
        
        try {
            // Attempt to purge in one command, passing the number of summaries
            // This may fail if the repo does not support this command
            r = openidm.action("repo/audit/recon", "command", {}, command);
            return r;
        } catch (e) {
            if (e.javaException.getCode() != null && e.javaException.getCode() == 400) {
                // Allow this failure, not all repos support "purge-by-recon-number-of"
                logger.debug("Failed to purge logs by number of recons in one command");   
            } else {
                throw e;
            }
        }

        command = {
                "commandId": "purge-by-recon-ids-to-keep", 
                "reconIds": obj.getReconsToKeep(includeMapping, excludeMapping),
                "includeMapping" : includeMapping,
                "excludeMapping" : excludeMapping
            };

        // If the above command was not supported, attempt to purge by passing a list of reconIds to keep
        r = openidm.action("repo/audit/recon", "command", {}, command);
        return r;
    };
    
    // Modifies the mapping name containing ONLY the wildcard character to support a special a case with OrientDB
    obj.getMapping = function(mapping) {
        if (mapping === "%") {
            return "%%";
        }
        return mapping;
    }
    
    // Set the default value for mappings if not configured
    // "%%" will be used instead of "%" to support a special a case with OrientDB.
    if (typeof mappings === "undefined") {
        mappings = ["%%"];
    }

    for(i = 0;i < mappings.length;i++){
        var includeMapping, excludeMappings, mapping = mappings[i];
        if (typeof mapping === "string") {
            includeMapping = obj.getMapping(mapping);
            excludeMapping = "";
        } else if (typeof mapping === "object") {
            includeMapping = obj.getMapping(mapping.include);
            excludeMapping = obj.getMapping(mapping.exclude);
        } else {
            results = "Mapping must of type string or object";
            logger.warn(results);
            return results;
        }
        if(purgeType == "purgeByNumOfReconsToKeep") {
            results[i] = obj.deleteFromAuditReconByNumOf(includeMapping, excludeMapping);
        } else if (purgeType == "purgeByExpired") {
            results[i] = obj.deleteFromAuditReconByExpired(includeMapping, excludeMapping);
        } else {
            results = "Must choose to either purge by expired or number of recons to keep";
            logger.warn(results);
            return results;
        }
    }

    return results;
}(
        input.mappings,
        input.purgeType,
        input.numOfRecons,
        input.intervalUnit,
        input.intervalValue,
        input.scheduleName
));
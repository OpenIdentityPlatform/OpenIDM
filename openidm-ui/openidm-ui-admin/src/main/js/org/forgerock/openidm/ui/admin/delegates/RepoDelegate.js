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
 * Copyright 2016 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function($, _, Constants, ConfigDelegate) {
    /**
     * Repo-specific extentions to the config delegate
     * @exports org/forgerock/openidm/ui/admin/delegates/RepoDelegate
     */

    var obj = Object.create(ConfigDelegate);

    /**
     * Queries the config store for a single entry which starts with "repo."
     */
    obj.findRepoConfig = function() {
        return obj.serviceCall({
            url: "?_queryFilter=_id sw 'repo.'",
            type: "GET"
        }).then((response) => response.result[0]);
    };

    /**
     * Determines the type of repo based on the config.
     */
    obj.getRepoTypeFromConfig = function(config) {
        return config._id.replace('repo.', '');
    };

    obj.deleteManagedObject = function (config, managedObjectName) {
        if (obj.getRepoTypeFromConfig(config) === "orientdb") {
            config = obj.removeManagedObjectFromOrientClasses(config, managedObjectName);
            return obj.updateEntity(config._id, config);
        } else {
            return $.Deferred().resolve();
        }
    };

    obj.addManagedObjectToOrientClasses = function(config, managedObjectName) {
        var orientClasses = config.dbStructure.orientdbClass;

        if(_.isUndefined(orientClasses["managed_" + managedObjectName])) {
            orientClasses["managed_" + managedObjectName] = {
                "index" : [
                    {
                        "propertyName" : "_openidm_id",
                        "propertyType" : "string",
                        "indexType" : "unique"
                    }
                ]
            };
        }
        return config;
    };

    obj.removeManagedObjectFromOrientClasses = function(config, managedObjectName) {
        var orientClasses = config.dbStructure.orientdbClass;
        delete orientClasses["managed_" + managedObjectName];
        return config;
    };


    /**
     * Returns a reference to the appropriate resource mapping section of the config
     * if one can be found; otherwise, undefined.
     */
    obj.findGenericResourceMappingForRoute = function (config, route) {
        return _.has(config, "resourceMapping.genericMapping") ?
            config.resourceMapping.genericMapping[route] :
            undefined;
    };

    /**
     * Modifies a resource mapping block to reflect the list of searchable properties passed into it
     * @param {Object} resourceConfig - the map of generic object details to modify
     * @param {Array} searchablePropertiesList - complete list of simple strings or JSON Pointers, representing all searchable properties for this object
     */
    obj.syncSearchablePropertiesForGenericResource = function (resourceConfig, searchablePropertiesList) {
        // only need to bother if all the properties are not already searchable by default
        if (resourceConfig && resourceConfig.searchableDefault === false) {
            resourceConfig.properties = _.reduce(searchablePropertiesList, function (result, prop) {
                result[((prop.indexOf("/") !== 0) ? "/" : "") + prop] = {
                    "searchable" : true
                };
                return result;
            }, {});
        }
        return resourceConfig;
    };

    return obj;
});

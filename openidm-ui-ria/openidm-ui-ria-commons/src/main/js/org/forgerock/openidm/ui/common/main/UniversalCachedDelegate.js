/*
 * @license DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
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

/*global $, define*/

/**
 * @author jdabrowski
 */
define("org/forgerock/openidm/ui/common/main/UniversalCachedDelegate", [
    "org/forgerock/openidm/ui/common/main/UniversalDelegate"
], function(universalDelegate) {

    var obj = {};
    
    obj.cache = {};
    
    obj.locks = {};
    
    obj.queue = {};
    
    obj.read = function(serviceUrl, id, successCallback, errorCallback) {
        if (!id) {
            if (errorCallback) {
                errorCallback("Not found for id = undefined");
                console.log("Not found for id = undefined");
            }
            return;
        }
        console.log("reading item");
        obj.get(serviceUrl, "read", "id", id, successCallback, errorCallback);
    };
    
    obj.readByName = function(serviceUrl, name, successCallback, errorCallback) {
        if (!name) {
            if (errorCallback) {
                errorCallback("Not found for name = undefined");
                console.log("Not found for name = undefined");
            }
            return;
        }
        console.log("reading item by name");
        obj.get(serviceUrl, "readByName", "name", name, successCallback, errorCallback);
    };
    
    obj.getAll = function(serviceUrl, successCallback, errorCallback) {
        console.log("reading all items");
        obj.get(serviceUrl, "getAll", null, null, successCallback, errorCallback);
    };
    
    obj.get = function(serviceUrl, methodName, tokenType, token, successCallback, errorCallback) {
        console.log("reading");
        var result = obj.getFromCache(serviceUrl, tokenType, token);
        if (result) {
            console.log("found in cache");
            successCallback(result);
        } else {
            console.log("NOT found in cache");
            
            if (obj.cacheNotEmpty(serviceUrl)) {
                console.log("item not found");
                if (errorCallback){
                    errorCallback("Not found");
                }
                return;
            }
            
            if (obj.isLocked(serviceUrl)) {
                console.log("waiting on lock");
                obj.addRequestToQueue(methodName, serviceUrl, token, successCallback, errorCallback);
            } else {
                obj.createLock(serviceUrl);
                universalDelegate.getAll(serviceUrl, function(data) {
                    console.log("getting all items from server");
                    obj.loadToCache(serviceUrl, data);
                    obj.removeLock(serviceUrl);
                    result = obj.getFromCache(serviceUrl, tokenType, token);
                    if (result) {
                        successCallback(obj.getFromCache(serviceUrl, tokenType, token));
                    } else {
                        console.log("item not found");
                        if (errorCallback){
                            errorCallback("Not found");
                        }
                    }
                    obj.raiseFromQueue(serviceUrl);
                }, function() {
                    obj.removeLock(serviceUrl);
                    if (errorCallback) {
                        errorCallback();
                    }
                    obj.raiseFromQueue(serviceUrl);
                });
            }
        }
    };
    

    
    
    obj.update = function(serviceUrl, objectParam, successCallback, errorCallback) {
        universalDelegate.update(serviceUrl, objectParam, successCallback, errorCallback);
    };
    
    obj.create = function(serviceUrl, object , successCallback, errorCallback) {
        universalDelegate.create(serviceUrl, object , successCallback, errorCallback);
    };

    obj.remove = function(serviceUrl, id, successCallback, errorCallback) {
        universalDelegate.remove(serviceUrl, id, successCallback, errorCallback);
    };

    

    obj.cacheNotEmpty = function(serviceUrl, tokenType, token) {
      if (obj.cache[serviceUrl]) {
          return true;
      }
      return false;
    };
    
    obj.getFromCache = function(serviceUrl, tokenType, token) {
        if (!obj.cache[serviceUrl]) {
            return;
        }
        
        if (!tokenType) {
            return obj.cache[serviceUrl];
        }
        
        if (tokenType === 'id') {
            return obj.cache[serviceUrl][token];
        } else if (tokenType === 'name'){
            return obj.getFromCacheByName(serviceUrl, token);
        }
    };
    
    obj.getFromCacheByName = function(serviceUrl, name) {
        var cacheKey, item;
        for (cacheKey in obj.cache[serviceUrl]) {
            item = obj.cache[serviceUrl][cacheKey];
            if (item.name === name) {
                return item;
            }
        }
    };
    
    obj.loadToCache = function(serviceUrl, items) {
        obj.cache[serviceUrl] = {};
        var i;
        for (i = 0; i < items.length; i++) {
            obj.cache[serviceUrl][items[i]._id] = items[i];
        }
        console.log("items added to cache");
    };
    
    obj.addRequestToQueue = function(methodName, serviceUrl, token, successCallback, errorCallback) {
        if (!obj.queue[serviceUrl]) {
            obj.queue[serviceUrl] = [];
        }
        if (token) {
            obj.queue[serviceUrl].push([methodName, serviceUrl, token, successCallback, errorCallback]);
        } else {
            obj.queue[serviceUrl].push([methodName, serviceUrl, successCallback, errorCallback]);
        }
        
    };
    
    obj.raiseFromQueue = function(queueName) {
        var i, functionData, queueItems = obj.queue[queueName];
        if (queueItems) {
            while (queueItems.length > 0) {
                functionData = queueItems[0];
                queueItems.splice(0,1);
                obj[functionData[0]](queueName, functionData[2],functionData[3],functionData[4]);
            }
        }
    };
    
    obj.createLock = function(sourceName) {
        obj.locks[sourceName] = {};
    };
    
    obj.removeLock = function(sourceName) {
        delete obj.locks[sourceName];
    };
    
    obj.isLocked = function(sourceName) {
        return obj.locks[sourceName];
    };

    return obj;
});
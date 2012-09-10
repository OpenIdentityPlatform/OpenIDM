/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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
*
*/

package org.forgerock.openidm.quartz.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource.Method;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.repo.RepositoryService;

public class MockRepositoryService implements RepositoryService {
    
    private Map<String, Object> map;
    private boolean printMap = true;
    
    /** Standard JSON resource request methods. */
    public enum Method {
        create, read, update, delete, patch, query, action
    }
    
    public MockRepositoryService() {
        map = new HashMap<String, Object>();
    }

    @Override
    public JsonValue handle(JsonValue request) throws JsonResourceException {

            try {
                Method method = request.get("method").asEnum(Method.class);
                String id = request.get("id").asString();
                JsonValue value = request.get("value");
                String rev = request.get("rev").asString();
                switch (method) {
                case create: 
                    return create(id, value.asMap());
                case read: 
                    return new JsonValue(read(id));
                case update: 
                    return update(id, rev, value.asMap());
                case delete: 
                    delete(id, rev);
                    return new JsonValue(null);
                case patch: 
                    patch(id, rev, null);
                    return new JsonValue(null);
                default: 
                    throw new JsonResourceException(JsonResourceException.BAD_REQUEST);
                }
            } catch (JsonValueException jve) {
                throw new JsonResourceException(JsonResourceException.BAD_REQUEST, jve);
            }
    }
    
    public JsonValue create(String id, Map<String, Object> object)
            throws ObjectSetException {
        printMap("create",object);
        map.put(id, object);
        return new JsonValue(map);

    }

    public Map<String, Object> read(String id) throws ObjectSetException, NotFoundException {
        Map<String, Object> object = (Map<String, Object>)map.get(id);
        printMap("read", object);
        if (object == null) {
            throw new NotFoundException("");
        }
        return object;
    }

    public JsonValue update(String id, String rev, Map<String, Object> object)
            throws ObjectSetException {
        printMap("update", object);
        map.put(id, object);
        return new JsonValue(map);
    }

    public void delete(String id, String rev) throws ObjectSetException {
        map.remove(id);
    }

    public void patch(String id, String rev, Patch patch)
            throws ObjectSetException {

    }

    public Map<String, Object> query(String id, Map<String, Object> params)
            throws ObjectSetException {
        return null;
    }

    public Map<String, Object> action(String id, Map<String, Object> params)
            throws ObjectSetException {
        return null;
    }
    
    public void printMap(String method, Map<String, Object> map) {
        if (printMap) {
            if (map != null) {
                Set<String> keys = map.keySet();
                for (String key : keys) {
                    System.out.println(method + " [" + key + ", " + map.get(key) + "]");
                }
            } else {
                System.out.println(method + " null");
            }
        }
    }

}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.salesforce.internal;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class SObjectDescribe {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.getDeserializationConfig().set(
                DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static SObjectDescribe newInstance(Map<String, Object> describe) {
       return mapper.convertValue(describe, SObjectDescribe.class);
    }

    private String name;
    private List<SFieldDescribe> fields;
    private boolean searchable;
    private boolean updateable;
    private boolean createable;
    private boolean deletable;
    private boolean queryable;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SFieldDescribe> getFields() {
        return fields;
    }

    public void setFields(List<SFieldDescribe> fields) {
        this.fields = fields;
    }

    public boolean isSearchable() {
        return searchable;
    }

    public void setSearchable(boolean searchable) {
        this.searchable = searchable;
    }

    public boolean isUpdateable() {
        return updateable;
    }

    public void setUpdateable(boolean updateable) {
        this.updateable = updateable;
    }

    public boolean isCreateable() {
        return createable;
    }

    public void setCreateable(boolean createable) {
        this.createable = createable;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    public boolean isQueryable() {
        return queryable;
    }

    public void setQueryable(boolean queryable) {
        this.queryable = queryable;
    }

    public Map<String, Object> beforeUpdate(JsonValue update) {
        Map<String, Object> response = new HashMap<String, Object>();
        for (SFieldDescribe field: fields) {
             if (field.isUpdateable() && update.isDefined(field.getName())) {
                response.put(field.getName(), update.get(field.getName()).getObject());
             }
        }
        return response;
    }

    public Map<String, Object> beforeCreate(JsonValue update) {
        Map<String, Object> response = new HashMap<String, Object>();
        for (SFieldDescribe field: fields) {
            if (field.isCreateable() && update.isDefined(field.getName())) {
                response.put(field.getName(), update.get(field.getName()).getObject());
            }
        }
        return response;
    }
}

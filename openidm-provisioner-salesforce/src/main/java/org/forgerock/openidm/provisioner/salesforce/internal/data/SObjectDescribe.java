/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openidm.provisioner.salesforce.internal.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.util.JsonUtil;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class SObjectDescribe {

    private static final ObjectMapper mapper = JsonUtil.build();

    static {
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
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
        for (SFieldDescribe field : fields) {
            if (field.isUpdateable() && update.isDefined(field.getName())) {
                response.put(field.getName(), update.get(field.getName()).getObject());
            }
        }
        return response;
    }

    public Map<String, Object> beforeCreate(JsonValue update) {
        Map<String, Object> response = new HashMap<String, Object>();
        for (SFieldDescribe field : fields) {
            if (field.isCreateable() && update.isDefined(field.getName())) {
                response.put(field.getName(), update.get(field.getName()).getObject());
            }
        }
        return response;
    }
}

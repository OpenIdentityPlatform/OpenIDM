/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openidm.provisioner.salesforce.internal.data;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class SFieldDescribe {
    private int length;
    private String name;
    private String type;
    private Object defaultValue;
    private boolean updateable;
    private boolean createable;

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
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
}

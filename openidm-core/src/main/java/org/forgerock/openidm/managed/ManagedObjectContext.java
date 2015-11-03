/*
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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.managed;

import java.util.HashMap;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.services.context.AbstractContext;
import org.forgerock.services.context.Context;

/**
 * A {@link Context} which represents a request coming from (or through) the Managed Object Service.
 * Additionally this context maintains a map of fields which can be used by scripts or components to communicate
 * information about the fields/properties of a managed object.
 */
public class ManagedObjectContext extends AbstractContext {
    
    private static final String ATTR_FIELDS = "fields";
    
    public ManagedObjectContext(final Context parent) {
        super(parent, "managedObject");
        this.data.put(ATTR_FIELDS, new HashMap<String, Object>());
    }

    public ManagedObjectContext(final JsonValue savedContext, final ClassLoader classLoader) 
            throws ResourceException {
        super(savedContext, classLoader);
    }
    
    public void setField(String key, String value) {
        this.data.get(ATTR_FIELDS).put(key, value);
    }
    
    public Object getField(String key) {
        return this.data.get(ATTR_FIELDS).get(key);
    }
}

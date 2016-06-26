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
 * Copyright 2012-2015 ForgeRock AS.
 */
package org.forgerock.openidm.workflow.activiti.impl;

import org.activiti.engine.identity.Group;
import org.forgerock.json.JsonValue;
import java.util.LinkedHashMap;

import static org.forgerock.openidm.workflow.activiti.impl.SharedIdentityService.*;

/**
 * @version $Revision$ $Date$
 */
public class JsonGroup extends JsonValue implements Group {
    static final long serialVersionUID = 1L;

    public JsonGroup(String groupId) {
        super(new LinkedHashMap<String, Object>());
        put(SCIM_ID, groupId);
    }

    public JsonGroup(JsonValue value) {
        super(value);
    }

    public String getId() {
        return get(SCIM_ID).asString();
    }

    public void setId(String id) {
        put(SCIM_ID, id);
    }

    public String getName() {
        return get(SCIM_DISPLAYNAME).asString();
    }

    public void setName(String name) {
        put(SCIM_DISPLAYNAME, name);
    }

    public String getType() {
        return get("type").asString();
    }

    public void setType(String string) {
        put("type", string);
    }
}

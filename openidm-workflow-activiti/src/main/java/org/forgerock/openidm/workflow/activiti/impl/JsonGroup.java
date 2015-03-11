/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.workflow.activiti.impl;

import org.activiti.engine.identity.Group;
import org.forgerock.json.fluent.JsonValue;
import java.util.HashMap;
import static org.forgerock.openidm.workflow.activiti.impl.SharedIdentityService.*;

/**
 * @version $Revision$ $Date$
 */
public class JsonGroup extends JsonValue implements Group {
    static final long serialVersionUID = 1L;

    public JsonGroup(String groupId) {
        super(new HashMap());
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

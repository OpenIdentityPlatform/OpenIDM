/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock Inc. All rights reserved.
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

import java.util.HashMap;
import java.util.Map;
import org.forgerock.json.fluent.JsonValue;

/**
 *
 * @author orsolyamebold
 */
public class ActivitiUtil {
    public static String getActionFromRequest(JsonValue request) {
        return request.get("params").get("_action").required().asString();
    }
    
    public static Map<String, Object> getProcessVariablesFromRequest(JsonValue request) {
        return request.get("value").isNull() ? 
                new HashMap(1) : request.get("value").expect(Map.class).asMap();
    }
}

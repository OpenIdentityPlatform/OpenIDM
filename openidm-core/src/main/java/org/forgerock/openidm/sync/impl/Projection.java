/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.sync.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonPath;
import org.forgerock.json.fluent.JsonNodeException;
import org.forgerock.json.fluent.JsonPathException;

import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.Scripts;
import org.forgerock.openidm.script.ScriptException;

/**
 * Project values by applying transformation scripts on managed objects.
 */
public class Projection {

    final static Logger logger = LoggerFactory.getLogger(Projection.class);

    public Projection() {
    }

    public Map<String, Object> projectValues(Map<String, Object> object, MapEntry mapEntry) throws JsonPathException, JsonNodeException, ScriptException {
        Collection<PropertyEntry> propertyEntries = mapEntry.getPropertyEntries();
        Map<String, Object> transformedObject = new HashMap<String, Object>();
        for (PropertyEntry propertyEntry : propertyEntries) {
            JsonPath sourcePropertyPath = new JsonPath(propertyEntry.getSourcePath());
            JsonPath targetPropertyPath = new JsonPath(propertyEntry.getTargetPath());
            JsonNode sourceObject = new JsonNode(object);
            JsonNode targetObject = new JsonNode(new HashMap<String, Object>());
            JsonNode scriptObject = new JsonNode(propertyEntry.getScript());
            Object sourceProperty = projectValue(sourcePropertyPath.get(sourceObject), scriptObject);
            targetPropertyPath.put(targetObject, targetPropertyPath);
        }
        return transformedObject;
    }

    private Object projectValue(JsonNode propertyValue, JsonNode scriptObject) throws JsonNodeException, ScriptException {
        Script script = Scripts.newInstance(scriptObject);
        Map<String, Object> scopeObject = new HashMap<String, Object>();
        scopeObject.put("sourceValue", propertyValue);
        script.exec(scopeObject);
        Object targetValue = scopeObject.get("targetValue");
        return targetValue;
    }

}
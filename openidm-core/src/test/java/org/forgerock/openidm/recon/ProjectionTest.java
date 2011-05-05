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

package org.forgerock.openidm.recon;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;


import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import static org.fest.assertions.Assertions.assertThat;

import org.forgerock.openidm.sync.impl.MapEntry;
import org.forgerock.openidm.sync.impl.Projection;

import org.forgerock.json.fluent.JsonNodeException;
import org.forgerock.json.fluent.JsonPathException;

import org.forgerock.openidm.script.ScriptException;

/**
 *
 */
public class ProjectionTest {

    MapEntry mapEntry;
    Map<String, Object> sourceObject = new HashMap<String, Object>();
    Map<String, Object> targetObject = new HashMap<String, Object>();
    Projection projector = new Projection();

    @BeforeClass
    private void setup() {
        Map<String, Object> mappings = new HashMap<String, Object>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("name", "");
        map.put("sourceObject", "");
        map.put("targetObject", "");
        map.put("synchrony", "synchronous");
        map.put("qualifier", "");
        map.put("namedQuery", "");

        ArrayList<Map<String, Object>> properties = new ArrayList<Map<String, Object>>();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("sourcePath", "$.property");
        props.put("targetPath", "$.property");

        Map<String, Object> scriptMap = new HashMap<String, Object>();
        scriptMap.put("type", "text/javascript");
        scriptMap.put("source", "targetValue = sourceValue");
        scriptMap.put("sharedScope", true);

        props.put("script", scriptMap);

        properties.add(props);
        map.put("propertyMappings", properties);


        try {
            mapEntry = new MapEntry(map);
        } catch (JsonNodeException e) {
            System.out.println("mapEntry build error: " + e.getLocalizedMessage());
        }
    }

    @Test
    public void simpleProjectionTest() {
        try {
            Map<String, Object> result = projector.projectValues(sourceObject, mapEntry);
            System.out.println(result);
        } catch (JsonPathException e) {
            e.printStackTrace();
        } catch (JsonNodeException e) {
            e.printStackTrace();
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }
}

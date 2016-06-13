/*
 * Copyright 2014 ForgeRock AS.
 *
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
 */

package org.forgerock.openidm.repo.jdbc.impl;

import java.io.IOException;
import java.util.Map;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonParser;

/**
 * Test of GenericTableConfig
 */
public class GenericTableConfigTest {

    @Test
    public void testIsSearchable() throws Exception {

        String cfgStr = 
            "    {" + 
            "        'mainTable' : 'managedobjects'," + 
            "        'propertiesTable' : 'managedobjectproperties'," +
            "        'searchableDefault' : false," + 
            "        'properties' : {" + 
            "            '/userName' : {" + 
            "                'searchable' : true" +
            "            }," + 
            "            '/roles' : {" + 
            "                'searchable' : true" + 
            "            }," + 
            "            '/addresses' : {" + 
            "                'searchable' : true" +
            "            }" + 
            "        }" + 
            "    }";
        
        JsonValue tableConfig = parseJson(cfgStr);
        GenericTableConfig tableCfg = GenericTableConfig.parse(tableConfig);

        // Test not searchable setting
        // simple property
        Assert.assertFalse(tableCfg.isSearchable(new JsonPointer("/arbitrary"))); 
        // map/object property
        Assert.assertFalse(tableCfg.isSearchable(new JsonPointer("/arbitrary2/map/x"))); 
        // list/array property
        Assert.assertFalse(tableCfg.isSearchable(new JsonPointer("/arbitrary3/list/0")));  
        
        // Test searchable setting
        // simple property
        Assert.assertTrue(tableCfg.isSearchable(new JsonPointer("/userName"))); 
        // map/object property
        Assert.assertTrue(tableCfg.isSearchable(new JsonPointer("/addresses/home/street"))); 
        // list/array property
        Assert.assertTrue(tableCfg.isSearchable(new JsonPointer("/roles/3")));

    }

    private JsonValue parseJson(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        return new JsonValue(mapper.readValue(json, Map.class));
    }
}

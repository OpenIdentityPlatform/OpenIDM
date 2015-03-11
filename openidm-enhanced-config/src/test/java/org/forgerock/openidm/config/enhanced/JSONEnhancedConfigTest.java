/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
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

package org.forgerock.openidm.config.enhanced;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.net.URLDecoder;
import java.util.Dictionary;
import java.util.Hashtable;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.core.SystemPropertyAccessor;
import org.testng.annotations.Test;

/**
 * A NAME does ...
 *
 */
public class JSONEnhancedConfigTest {
    @Test
    public void testGetConfiguration() throws Exception {

        // Redirect to a test Boot file
        String bootFile =
                URLDecoder.decode(JSONEnhancedConfigTest.class.getResource(
                        "/" + ServerConstants.DEFAULT_BOOT_FILE_LOCATION).getFile(), "UTF-8");
        System.setProperty(ServerConstants.PROPERTY_BOOT_FILE_LOCATION, bootFile);
        System.setProperty("system.environment", "TEST");

        IdentityServer.initInstance(new SystemPropertyAccessor(null));

        /*
         * {"attr1" : "&{property1}","attr2" : "pre &{property2}-post","attr3" :
         * "&{not-available}","attr4":{"subattr1" :
         * "nested &{env.&{environment}.variable}","subattr2" :
         * "nested &{env.&{system.environment}.variable}"},"attr5" :
         * "&{not-&{property1}","attr6" : "esc\\&{not-&{property1}}","attr7" :
         * "not-&{property1}}","attr8" :
         * "&{&{env-key}.DEMO.&{variable-key}}","attr9" :
         * "&{&{env-key}.PROD.&{variable-key}}","attr10" :
         * "&{&{inst-key}.DEMO.&{variable-key}}"}
         */
        String jsonConfigProperty =
                "{\"attr1\" : \"&{property1}\",\"attr2\" : \"pre &{property2}-post\",\"attr3\" : \"&{not-available}\",\"attr4\":{\"subattr1\" : \"nested &{env.&{environment}.variable}\",\"subattr2\" : \"nested &{env.&{system.environment}.variable}\"},\"attr5\" : \"&{not-&{property1}\",\"attr6\" : \"esc\\\\&{not-&{property1}}\",\"attr7\" : \"not-&{property1}}\",\"attr8\" : \"&{&{env-key}.DEMO.&{variable-key}}\",\"attr9\" : \"&{&{env-key}.PROD.&{variable-key}}\",\"attr10\" : \"&{&{inst-key}.DEMO.&{variable-key}}\"}";

        JSONEnhancedConfig enhancedConfig = new JSONEnhancedConfig();

        Dictionary<String, Object> dict = new Hashtable<String, Object>(1);
        dict.put(JSONEnhancedConfig.JSON_CONFIG_PROPERTY, jsonConfigProperty);

        JsonValue configuration = enhancedConfig.getConfiguration(dict, "pid", false);

        assertEquals(configuration.get("attr1").required().asString(), "value1");
        assertEquals(configuration.get("attr2").required().asString(), "pre value2-post");
        // assertEquals(configuration.get("attr3").required().asString(), "");
        assertEquals(configuration.get("attr3").required().asString(), "&{not-available}");
        JsonValue nested = configuration.get("attr4");
        assertFalse(nested.isNull());

        assertEquals(nested.get("subattr1").required().asString(), "nested DEMO");
        assertEquals(nested.get("subattr2").required().asString(), "nested TEST");

        assertEquals(configuration.get("attr5").required().asString(), "&{not-value1");
        assertEquals(configuration.get("attr6").required().asString(), "esc\\&{not-value1}");
        assertEquals(configuration.get("attr7").required().asString(), "not-value1}");
        assertEquals(configuration.get("attr8").required().asString(), "DEMO");
        assertEquals(configuration.get("attr9").required().asString(), "&{env.PROD.variable}");
        assertEquals(configuration.get("attr10").required().asString(),
                "&{&{inst-key}.DEMO.variable}");
    }
}

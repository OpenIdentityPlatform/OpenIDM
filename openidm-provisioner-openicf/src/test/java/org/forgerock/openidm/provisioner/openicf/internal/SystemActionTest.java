/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.provisioner.openicf.internal;

import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * A NAME does ...
 *
 */
public class SystemActionTest {

    protected JsonValue configuration = null;

    @BeforeClass
    public void BeforeClass() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        configuration =
                new JsonValue(mapper.readValue(SystemActionTest.class
                        .getResourceAsStream("/config/provisioner.openicf-test.json"), Map.class));
    }

    @Test
    public void testGetName() throws Exception {
        for (JsonValue systemActions : configuration.get("systemActions").expect(List.class)) {
            if ("ConnectorScript#5".equals(new SystemAction(systemActions).getName())) {
                return;
            }
        }
        Assert.fail();
    }
}

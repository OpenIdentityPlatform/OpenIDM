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
package org.forgerock.openidm.repo.orientdb.internal;

import java.util.HashMap;
import java.util.Map;
import org.forgerock.openidm.core.PropertyUtil;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class OrientSQLPropertyAccessorTest {

    @Test
    public void testBasicPropertySubstitution() throws Exception {

        String queryExpression = "select * from ${_alma} where ${korte} = :barack";
        Map<String, String> params = new HashMap<String, String>();
        params.put("_alma", "almavalue");
        params.put("korte", "kortevalue");
        params.put("barack", "barackvalue");
        String substVars = (String) PropertyUtil.substVars(queryExpression, new OrientSQLPropertyAccessor(params), PropertyUtil.Delimiter.DOLLAR, true);
        assertEquals(substVars, "select * from almavalue where kortevalue = :barack");
    }
    
    @Test
    public void testDotnotationPropertySubstitution() throws Exception {

        String queryExpression = "select * from ${dotnotation:alma} where ${dotnotation:barack} = :banan";
        Map<String, String> params = new HashMap<String, String>();
        params.put("alma", "alma/value");
        params.put("barack", "/barack/value");
        params.put("banan", "bananvalue");
        String substVars = (String) PropertyUtil.substVars(queryExpression, new OrientSQLPropertyAccessor(params), PropertyUtil.Delimiter.DOLLAR, true);
        assertEquals(substVars, "select * from alma.value where barack.value = :banan");
    }
}

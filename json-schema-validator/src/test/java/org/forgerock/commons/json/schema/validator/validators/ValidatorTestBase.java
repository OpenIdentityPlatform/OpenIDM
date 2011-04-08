/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
 * $Id$
 */
package org.forgerock.commons.json.schema.validator.validators;

import org.forgerock.commons.json.schema.validator.validators.Validator;
import org.forgerock.commons.json.schema.validator.ObjectValidatorFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.testng.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public abstract class ValidatorTestBase {
    protected List<Object[]> getTestJSON(String instanceType, String testFilePath) throws IOException, ParseException {
        InputStream is = ArrayTypeValidatorTest.class.getResourceAsStream(testFilePath);
        Assert.assertNotNull(is);
        JSONParser parser = new JSONParser();
        Object o = parser.parse(new InputStreamReader(is));
        Assert.assertTrue(o instanceof List, "Expect JSON Array");
        List<Object[]> tests = new ArrayList<Object[]>();
        for (Object s : (List) o) {
            Assert.assertTrue(s instanceof Map, "Expect JSON Object");
            Validator v = ObjectValidatorFactory.getTypeValidator((Map<String, Object>) ((Map) s).get("schema"));
            for (Object i : (List) ((Map) s).get(instanceType)) {
                tests.add(new Object[]{v, i});
            }
        }
        return tests;
    }
}

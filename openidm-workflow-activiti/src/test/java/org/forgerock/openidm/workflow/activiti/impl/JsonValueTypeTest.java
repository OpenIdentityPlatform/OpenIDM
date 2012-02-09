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

import org.activiti.engine.impl.persistence.entity.ByteArrayEntity;
import org.activiti.engine.impl.variable.ValueFields;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class JsonValueTypeTest {
    @Test
    public void testValues() throws Exception {
        JsonValueType testable = new JsonValueType();
        ValueFields testField = new TestValueFields();
        testable.setValue(null, testField);
        Assert.assertNull(testable.getValue(testField));

        JsonValue expected = new JsonValue("Test", new JsonPointer("activiti"));
        testable.setValue(expected, testField);
        JsonValue actual = (JsonValue) testable.getValue(testField);
        Assert.assertEquals(actual.getObject(), expected.getObject());
        Assert.assertEquals(actual.getPointer().toString(), expected.getPointer().toString());
    }

    class TestValueFields implements ValueFields {
        private String value = null;

        public String getName() {
            return null;
        }

        public String getTextValue() {
            return value;
        }

        public void setTextValue(String textValue) {
            value = textValue;
        }

        public String getTextValue2() {
            return null;
        }

        public void setTextValue2(String textValue2) {
        }

        public Long getLongValue() {
            return null;
        }

        public void setLongValue(Long longValue) {
        }

        public Double getDoubleValue() {
            return null;
        }

        public void setDoubleValue(Double doubleValue) {
        }

        public String getByteArrayValueId() {
            return null;
        }

        public void setByteArrayValue(ByteArrayEntity byteArrayValue) {
        }

        public ByteArrayEntity getByteArrayValue() {
            return null;
        }

        public Object getCachedValue() {
            return null;
        }

        public void setCachedValue(Object deserializedObject) {
        }
    }
}

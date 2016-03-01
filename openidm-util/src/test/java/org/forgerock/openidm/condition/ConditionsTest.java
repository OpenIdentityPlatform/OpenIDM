/*
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
 *
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.services.context.RootContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test the Conditions class.
 *
 * Currently only the filter visitor is tested.
 */
public class ConditionsTest {
    
    /**
     * A test object used to evaluate conditions against.
     */
    private static JsonValue testObject = json(object(
            field("object", object(
                field("name", "alice"),
                field("age", 1234L),
                field("balance", 3.14159),
                field("isAdmin", false),
                field("nullVal", null))
            ),
            field("linkQualifier", "test")));

    /**
     * Returns an array of filter data.  Each element in the array is an array containing a condition filter string and
     * a boolean representing the result of the filter applied to the test object.
     * 
     * @return an array of filter data.
     */
    @DataProvider
    public Object[][] filterData() {
        return new Object[][] {
                // @formatter:off
                { "/object/name eq \"alice\"", true },
                { "/object/age eq 1234", true },
                { "/object/balance eq 3.14159", true },
                { "/object/isAdmin eq false", true },
                { "/object/age lt 1234", false },
                { "/object/age le 1234", true },
                { "/object/age gt 1234", false },
                { "/object/age ge 1234", true },
                { "/object/name co \"al\"", true },
                { "/object/name sw \"al\"", true },
                { "/object/name pr", true },
                { "/object/missing pr", false },
                { "/object/nullVal pr", false },
                { "/object/age lt 18 or /object/age gt 30", true },
                { "/object/age lt 1000 or /object/age gt 3000", false },
                { "/object/age lt 18 and /object/age gt 30", false },
                { "/object/age gt 1000 and /object/age lt 1300", true },
                { "/object/age ne 1234", false },
                { "/linkQualifier eq \"test\"", true },
                { "/linkQualifier eq \"fail\"", false }
                // @formatter:on
        };
    }

    @Test(dataProvider = "filterData")
    public void testEvaluateCondition(String filter, Boolean state) throws JsonValueException {
        assertThat(Conditions.newCondition(json(filter)).evaluate(testObject, new RootContext())).isEqualTo(state);
    }
}

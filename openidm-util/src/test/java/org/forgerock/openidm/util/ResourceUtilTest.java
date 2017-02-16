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
 * Copyright 2017 ForgeRock AS.
 */
package org.forgerock.openidm.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openidm.util.RelationshipUtil.REFERENCE_PROPERTIES;

import org.forgerock.json.JsonValue;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests {@link ResourceUtil}
 */
public class ResourceUtilTest {

    @DataProvider(name = "equality")
    public Object[][] equalityTestData() {
        return new Object[][]{{
                json(object(
                        field("_id", "id"),
                        field("_rev", "rev"),
                        field("prop", "value"))),
                json(object(
                        field("prop", "value")))
        }, {
                json(object(
                        field("_id", "id"),
                        field("_rev", "rev2"),
                        field("prop", "value"))),
                json(object(
                        field("_id", "X"),
                        field("_rev", "rev3"),
                        field("prop", "value")))
        }, {
                json(object(
                        field("_id", "id"),
                        field("_rev", "rev2"),
                        field("prop", json(object(
                                field("_id", "subid"),
                                field("subprop", "subvalue")))))),
                json(object(
                        field("_id", "X"),
                        field("_rev", "rev3"),
                        field("prop", json(object(
                                field("subprop", "subvalue")))))),
        }, {
                json(object(
                        field("_id", "id"),
                        field("_rev", "rev2"),
                        field(REFERENCE_PROPERTIES, json(object(
                                field("_id", "subid"),
                                field("subprop", "subvalue")))))),
                json(object(
                        field("_id", "X"),
                        field("_rev", "rev3"),
                        field(REFERENCE_PROPERTIES, json(object(
                                field("subprop", "subvalue")))))),
        }, {
                json(object(
                        field("_id", "id"),
                        field("_rev", "rev2"),
                        field(REFERENCE_PROPERTIES, json(object(
                                field("_id", "subid")))))),
                json(object(
                        field("_id", "X"),
                        field("_rev", "rev3"))),
        }};
    }

    @Test(dataProvider = "equality")
    public void testResourceEquality(JsonValue left, JsonValue right) {
        // save a copy before we test equality.
        JsonValue leftCopy = left.copy();
        JsonValue rightCopy = right.copy();

        // test the equality
        assertThat(ResourceUtil.isEqual(left, right)).isTrue();

        // verify that the isEqual doesn't mutate the passed in jsonValues.
        assertThat(leftCopy.isEqualTo(left)).isTrue();
        assertThat(rightCopy.isEqualTo(right)).isTrue();
    }

    @DataProvider(name = "inequality")
    public Object[][] inequalityTestData() {
        return new Object[][]{{
                json(object(
                        field("_id", "id"),
                        field("_rev", "rev"),
                        field("prop", "value"))),
                json(object(
                        field("prop", "valueX")))

        }, {
                json(object()),
                null
        }, {
                json(object(
                        field("_id", "id"),
                        field("_rev", "rev2"),
                        field(REFERENCE_PROPERTIES, json(object(
                                field("_id", "subid"),
                                field("subprop", "subvalue")))))),
                json(object(
                        field("_id", "X"),
                        field("_rev", "rev3"),
                        field(REFERENCE_PROPERTIES, json(object(
                                field("subprop", "different")))))),
        }};
    }

    @Test(dataProvider = "inequality")
    public void testResourceInequality(JsonValue left, JsonValue right) {
        assertThat(ResourceUtil.isEqual(left, right)).isFalse();
    }

}

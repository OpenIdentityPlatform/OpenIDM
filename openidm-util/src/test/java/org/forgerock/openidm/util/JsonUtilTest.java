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

package org.forgerock.openidm.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.SortKey;
import org.testng.annotations.Test;

/**
 * Tests the {@link JsonUtil} class.
 */
public class JsonUtilTest {

    @Test
    public void testJsonComparator() {
        // given
        JsonValue testA = json(object(field("fn", "Alex"), field("on", true), field("desc", "foo")));
        JsonValue testA2 = json(object(field("fn", "Alex"), field("on", true)));
        JsonValue testA3 = json(object(field("fn", "Alex"), field("on", false)));
        JsonValue testA4 = json(object(field("fn", "Alex"), field("on", "string")));
        JsonValue testA5 = json(object(field("fn", "Alex"), field("on", 5)));
        JsonValue testA6 = json(object(field("fn", "Alex"), field("on", 6)));
        JsonValue testZ = json(object(field("fn", "Zebra"), field("on", false)));

        List<SortKey> sortKeys = Arrays.asList(SortKey.ascendingOrder("fn"), SortKey.ascendingOrder("on"));
        List<SortKey> onlyInOneSortKeys = Collections.singletonList(SortKey.ascendingOrder("desc"));
        List<SortKey> notInJsonSortKeys = Collections.singletonList(SortKey.ascendingOrder("notInJson"));

        List<JsonValue> listToSort = Arrays.asList(testA, testZ);

        // when
        Comparator<JsonValue> jsonValueComparator = JsonUtil.getComparator(sortKeys);
        Collections.sort(listToSort, jsonValueComparator);

        // then
        assertThat(jsonValueComparator.compare(testA, testZ)).isLessThan(0);
        assertThat(jsonValueComparator.compare(testZ, testA)).isGreaterThan(0);
        assertThat(jsonValueComparator.compare(testA, testA)).isEqualTo(0);  // tests == condition
        assertThat(jsonValueComparator.compare(testA, testA2)).isEqualTo(0);
        assertThat(jsonValueComparator.compare(testA, testA3)).isGreaterThan(0);
        assertThat(jsonValueComparator.compare(testA, testA4)).isLessThan(0);
        assertThat(jsonValueComparator.compare(testA5, testA6)).isLessThan(0);
        assertThat(jsonValueComparator.compare(json(null), json(null))).isEqualTo(0);
        assertThat(jsonValueComparator.compare(json(object()), json(null))).isGreaterThan(0);
        assertThat(jsonValueComparator.compare(json(null), json(object()))).isLessThan(0);
        assertThat(listToSort.get(0).get("fn").asString()).isEqualTo("Alex");

        // Now test when only one value has the sort key.
        jsonValueComparator = JsonUtil.getComparator(onlyInOneSortKeys);
        assertThat(jsonValueComparator.compare(testA,testZ)).isGreaterThan(0);
        assertThat(jsonValueComparator.compare(testZ,testA)).isLessThan(0);

        // Now given a sortKey that isn't a part of any of the jsonValues, expect equality.
        jsonValueComparator = JsonUtil.getComparator(notInJsonSortKeys);
        assertThat(jsonValueComparator.compare(testA, testA)).isEqualTo(0);
        assertThat(jsonValueComparator.compare(testA, testZ)).isEqualTo(0);
        assertThat(jsonValueComparator.compare(testZ, testA)).isEqualTo(0);
        assertThat(jsonValueComparator.compare(testA, testA2)).isEqualTo(0);
    }

    @Test
    public void testJsonComparatorDescending() {
        // given
        JsonValue testA = json(object(field("fn", "Alex"), field("sn", "Alberto")));
        JsonValue testZ = json(object(field("fn", "Zebra"), field("sn", "Zoolander")));

        List<SortKey> sortKeysDescending = Arrays.asList(SortKey.descendingOrder("fn"), SortKey.descendingOrder("sn"));
        List<JsonValue> listToSort = Arrays.asList(testA, testZ);

        // when
        Comparator<JsonValue> jsonValueComparator = JsonUtil.getComparator(sortKeysDescending);
        Collections.sort(listToSort, jsonValueComparator);

        // then
        assertThat(jsonValueComparator.compare(testA, testZ)).isGreaterThan(0);
        assertThat(jsonValueComparator.compare(testZ, testA)).isLessThan(0);
        assertThat(listToSort.get(0).get("fn").asString()).isEqualTo("Zebra");
    }

}

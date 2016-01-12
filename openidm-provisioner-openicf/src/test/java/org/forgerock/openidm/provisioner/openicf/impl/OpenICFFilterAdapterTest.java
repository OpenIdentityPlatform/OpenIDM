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

package org.forgerock.openidm.provisioner.openicf.impl;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.provisioner.openicf.commons.ObjectClassInfoHelper;
import org.forgerock.openidm.provisioner.openicf.commons.ObjectClassInfoHelperFactory;
import org.forgerock.openidm.util.FileUtil;
import org.forgerock.util.query.QueryFilter;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.CompositeFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.common.objects.filter.NotFilter;
import org.identityconnectors.framework.common.objects.filter.PresenceFilter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.forgerock.util.query.QueryFilter.*;

/**
 * Unit tests for {@link OpenICFFilterAdapter} which verify that all supported CREST
 * {@link QueryFilter}s are converted to the expected ICF {@link Filter}s
 */
public class OpenICFFilterAdapterTest {
    private static final String OBJECT_TYPES = "objectTypes";

    private static final JsonPointer DOES_NOT_EXIST_PTR = new JsonPointer("/doesNotExist");

    private static final String STRING_VALUE = "a@b.com";
    private static final Attribute STRING_ATTR = AttributeBuilder.build("__NAME__", STRING_VALUE);
    private static final JsonPointer STRING_PTR = new JsonPointer("/name");

    private static final Integer NUMBER_VALUE = 123;
    private static final Attribute NUMBER_ATTR = AttributeBuilder.build("sortKey", NUMBER_VALUE);
    private static final JsonPointer NUMBER_PTR = new JsonPointer("/sortKey");

    private final ObjectClassInfoHelper helper;
    private final OpenICFFilterAdapter filterAdapter;

    public OpenICFFilterAdapterTest() throws URISyntaxException, IOException {
        final JsonValue schema = new JsonValue(new HashMap<String, Object>());
        schema.put(OBJECT_TYPES, toJsonValue(FileUtil.readFile(new File(
                new File(OpenICFFilterAdapterTest.class.getResource("/").toURI()),
                "config/queryFilterAdapterTestSchema.json"))));

        helper = ObjectClassInfoHelperFactory.createObjectClassInfoHelper(
                schema.get(OBJECT_TYPES).get(ObjectClass.ACCOUNT_NAME));
        filterAdapter = new OpenICFFilterAdapter();
    }

    /**
     * @return Configuration for the majority of test cases
     */
    @DataProvider
    public Object[][] data() {
        return new Object[][] {
                { contains(STRING_PTR, STRING_VALUE), FilterBuilder.contains(STRING_ATTR) },
                { equalTo(STRING_PTR, STRING_VALUE), FilterBuilder.equalTo(STRING_ATTR) },
                { startsWith(STRING_PTR, STRING_VALUE), FilterBuilder.startsWith(STRING_ATTR) },
                { lessThan(NUMBER_PTR, NUMBER_VALUE), FilterBuilder.lessThan(NUMBER_ATTR) },
                { lessThanOrEqualTo(NUMBER_PTR, NUMBER_VALUE), FilterBuilder.lessThanOrEqualTo(NUMBER_ATTR) },
                { greaterThan(NUMBER_PTR, NUMBER_VALUE), FilterBuilder.greaterThan(NUMBER_ATTR) },
                { greaterThanOrEqualTo(NUMBER_PTR, NUMBER_VALUE), FilterBuilder.greaterThanOrEqualTo(NUMBER_ATTR) },
                { and(equalTo(STRING_PTR, STRING_VALUE), equalTo(STRING_PTR, STRING_VALUE)),
                        FilterBuilder.and(FilterBuilder.equalTo(STRING_ATTR), FilterBuilder.equalTo(STRING_ATTR)) },
                { or(equalTo(STRING_PTR, STRING_VALUE), equalTo(STRING_PTR, STRING_VALUE)),
                        FilterBuilder.or(FilterBuilder.equalTo(STRING_ATTR), FilterBuilder.equalTo(STRING_ATTR)) },
                { not(equalTo(STRING_PTR, STRING_VALUE)), FilterBuilder.not(FilterBuilder.equalTo(STRING_ATTR)) },
                { extendedMatch(STRING_PTR, OpenICFFilterAdapter.EW, STRING_VALUE),
                        FilterBuilder.endsWith(STRING_ATTR) },
                { extendedMatch(STRING_PTR, OpenICFFilterAdapter.CA, STRING_VALUE),
                        FilterBuilder.containsAllValues(STRING_ATTR) },
                { present(STRING_PTR), FilterBuilder.present(STRING_ATTR.getName()) },
                { alwaysTrue(), null }
        };
    }

    /**
     * Performs all tests defined by {@link #data()}
     *
     * @param crestFilter CREST {@link QueryFilter}
     * @param expectedFilter Expected OpenICF {@link Filter}
     */
    @Test(dataProvider = "data")
    public void testFilter(final QueryFilter<JsonPointer> crestFilter, final Filter expectedFilter) {
        final Filter actualFilter = crestFilter.accept(filterAdapter, helper);
        if (expectedFilter == null) {
            // adapter converts to null
            assertThat(actualFilter)
                    .isNull();
            return;
        }

        // class-type equality
        assertThat(actualFilter)
                .isNotNull()
                .isInstanceOf(expectedFilter.getClass());

        // instance equality
        if (expectedFilter instanceof AttributeFilter) {
            assertEquals((AttributeFilter) expectedFilter, (AttributeFilter) actualFilter);
        } else if (expectedFilter instanceof CompositeFilter) {
            final CompositeFilter expectedComposite = (CompositeFilter) expectedFilter;
            final CompositeFilter actualComposite = (CompositeFilter) actualFilter;
            assertEquals((AttributeFilter) expectedComposite.getLeft(), (AttributeFilter) actualComposite.getLeft());
            assertEquals((AttributeFilter) expectedComposite.getRight(), (AttributeFilter) actualComposite.getRight());
        } else if (expectedFilter instanceof NotFilter) {
            final Filter expectedNot = ((NotFilter) expectedFilter).getFilter();
            final Filter actualNot = ((NotFilter) actualFilter).getFilter();
            assertEquals((AttributeFilter) expectedNot, (AttributeFilter) actualNot);
        } else if (expectedFilter instanceof PresenceFilter) {
            assertThat(((PresenceFilter) expectedFilter).getName())
                    .isEqualTo(((PresenceFilter) actualFilter).getName());
        } else {
            throw new UnsupportedOperationException(
                    "expectedFilter not recognised: " + actualFilter.getClass().getCanonicalName());
        }
    }

    /**
     * Test that {@link OpenICFFilterAdapter#visitPresentFilter(ObjectClassInfoHelper, JsonPointer)}
     * throws an {@link EmptyResultSetException} when a requested field is not present
     */
    @Test(expectedExceptions = EmptyResultSetException.class)
    public void testVisitPresentFilterException() {
        filterAdapter.visitPresentFilter(helper, DOES_NOT_EXIST_PTR);
    }

    /**
     * Test that {@link OpenICFFilterAdapter#visitExtendedMatchFilter(ObjectClassInfoHelper, JsonPointer, String,
     * Object)} throws an {@code Exception} when an unsupported {@code matchingRuleId} argument is passed in
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testVisitExtendedMatchFilterException() {
        filterAdapter.visitExtendedMatchFilter(helper, STRING_PTR, "doesNotExist", STRING_VALUE);
    }

    /**
     * Test that {@link OpenICFFilterAdapter#visitAndFilter(ObjectClassInfoHelper, List)}
     * throws an {@code Exception} when an empty-list {@code subFilters} argument is passed in
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testVisitAndFilterException() {
        filterAdapter.visitAndFilter(helper, Collections.<QueryFilter<JsonPointer>>emptyList());
    }

    /**
     * Test that {@link OpenICFFilterAdapter#visitOrFilter(ObjectClassInfoHelper, List)}
     * throws an {@code Exception} when an empty-list {@code subFilters} argument is passed in
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testVisitOrFilterException() {
        filterAdapter.visitOrFilter(helper, Collections.<QueryFilter<JsonPointer>>emptyList());
    }

    /**
     * Test that {@link OpenICFFilterAdapter#visitBooleanLiteralFilter(ObjectClassInfoHelper, boolean)}
     * throws an {@link EmptyResultSetException} when literal {@code false} is the {@code value} argument
     */
    @Test(expectedExceptions = EmptyResultSetException.class)
    public void testVisitBooleanFilterWithFalse() {
        filterAdapter.visitBooleanLiteralFilter(helper, false);
    }

    private static JsonValue toJsonValue(final String json) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return new JsonValue(mapper.readValue(json, Map.class));
    }

    private static void assertEquals(final AttributeFilter expected, final AttributeFilter actual) {
        // test equality regardless of order of attributes
        final Set<Object> expectedValues = new HashSet<>(expected.getAttribute().getValue());
        final Set<Object> actualValues = new HashSet<>(actual.getAttribute().getValue());
        if (!expectedValues.equals(actualValues)) {
            fail("Expected " + actualValues + " to be equal to " + actualValues + " but it was not");
        }
    }
}

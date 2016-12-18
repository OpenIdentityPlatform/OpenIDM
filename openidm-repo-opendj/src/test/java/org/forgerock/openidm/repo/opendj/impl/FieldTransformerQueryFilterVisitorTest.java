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

package org.forgerock.openidm.repo.opendj.impl;

import org.forgerock.guava.common.base.Function;
import org.forgerock.util.query.QueryFilter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.util.query.QueryFilter.alwaysFalse;
import static org.forgerock.util.query.QueryFilter.alwaysTrue;
import static org.forgerock.util.query.QueryFilter.and;
import static org.forgerock.util.query.QueryFilter.contains;
import static org.forgerock.util.query.QueryFilter.equalTo;
import static org.forgerock.util.query.QueryFilter.extendedMatch;
import static org.forgerock.util.query.QueryFilter.greaterThan;
import static org.forgerock.util.query.QueryFilter.greaterThanOrEqualTo;
import static org.forgerock.util.query.QueryFilter.lessThan;
import static org.forgerock.util.query.QueryFilter.lessThanOrEqualTo;
import static org.forgerock.util.query.QueryFilter.not;
import static org.forgerock.util.query.QueryFilter.or;
import static org.forgerock.util.query.QueryFilter.present;
import static org.forgerock.util.query.QueryFilter.startsWith;

public class FieldTransformerQueryFilterVisitorTest {

    private FieldTransformerQueryFilterVisitor<String> visitor = new FieldTransformerQueryFilterVisitor<>(new Function<String, String>() {
        @Nullable
        @Override
        public String apply(@Nullable String jsonPointer) {
            return "/transformed";
        }
    });

    @DataProvider
    public Object[][] toStringData() {
        // Use longs for integer values because valueOf parses integers as Longs and
        // equals() is sensitive to the type.
        return new Object[][] {
                // @formatter:off
                { alwaysTrue(), "true" },
                { alwaysFalse(), "false" },
                { equalTo("/name", "alice"), "/transformed eq \"alice\""},
                { equalTo("/age", 1234L), "/transformed eq 1234" },
                { equalTo("/balance", 3.14159), "/transformed eq 3.14159" },
                { equalTo("/isAdmin", false), "/transformed eq false" },
                { lessThan("/age", 1234L), "/transformed lt 1234" },
                { lessThanOrEqualTo("/age", 1234L), "/transformed le 1234" },
                { greaterThan("/age", 1234L), "/transformed gt 1234" },
                { greaterThanOrEqualTo("/age", 1234L), "/transformed ge 1234" },
                { contains("/name", "al"), "/transformed co \"al\"" },
                { startsWith("/name", "al"), "/transformed sw \"al\"" },
                { present("/name"), "/transformed pr" },
                { or(), "false" }, // zero operand or is always false
                { and(), "true" }, // zero operand and is always true
                { or(equalTo("/age", 1234L)), "/transformed eq 1234" }, // single operand or is no-op
                { and(equalTo("/age", 1234L)), "/transformed eq 1234" }, // single operand and is no-op
                { or(lessThan("/age", 18L), greaterThan("/age", 30L)), "(/transformed lt 18 or /transformed gt 30)" },
                { and(lessThan("/age", 18L), greaterThan("/age", 30L)), "(/transformed lt 18 and /transformed gt 30)" },
                { or(equalTo("/role", "a"), equalTo("/role", "b"), equalTo("/role", "c")),
                        "(/transformed eq \"a\" or /transformed eq \"b\" or /transformed eq \"c\")" },
                { and(equalTo("/role", "a"), equalTo("/role", "b"), equalTo("/role", "c")),
                        "(/transformed eq \"a\" and /transformed eq \"b\" and /transformed eq \"c\")" },
                { or(equalTo("/role", "a"), and(equalTo("/role", "b"), equalTo("/role", "c"))),
                        "(/transformed eq \"a\" or (/transformed eq \"b\" and /transformed eq \"c\"))" },
                { and(equalTo("/role", "a"), or(equalTo("/role", "b"), equalTo("/role", "c"))),
                        "(/transformed eq \"a\" and (/transformed eq \"b\" or /transformed eq \"c\"))" },
                { not(equalTo("/age", 1234L)), "! (/transformed eq 1234)" },
                { not(not(equalTo("/age", 1234L))), "! (! (/transformed eq 1234))" },
                { extendedMatch("/name", "regex", "al.*"), "/transformed regex \"al.*\"" },
                { extendedMatch("/name", "eq", "alice"), "/transformed eq \"alice\"" },
                // @formatter:on
        };
    }

    @Test(dataProvider = "toStringData")
    public void testToString(QueryFilter<String> filter, String filterString) {
        assertThat(filter.accept(visitor, null).toString()).isEqualTo(filterString);
    }
}

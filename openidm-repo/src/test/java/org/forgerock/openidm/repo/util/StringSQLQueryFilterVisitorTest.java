/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014-2015 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.repo.util;

import org.forgerock.util.query.QueryFilter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import org.forgerock.json.JsonPointer;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.forgerock.util.query.QueryFilter.*;

/**
 * Tests basic QueryFilter-to-SQL-Where-Clause creation using a basic SQL syntax.
 */
public class StringSQLQueryFilterVisitorTest {

    /* a visitor to generate base value assertions */
    private StringSQLQueryFilterVisitor<Void> visitor = new StringSQLQueryFilterVisitor<Void>() {
        @Override
        public StringSQLRenderer visitValueAssertion(Void parameters, String operand, JsonPointer field, Object valueAssertion) {
            String quote = valueAssertion instanceof String ? "'" : "";
            return new StringSQLRenderer(field.leaf())
                    .append(" ")
                    .append(operand)
                    .append(" ")
                    .append(quote)
                    .append(String.valueOf(valueAssertion))
                    .append(quote);
        }

        @Override
        public StringSQLRenderer visitPresentFilter(Void parameters, JsonPointer field) {
            return new StringSQLRenderer(field.leaf()).append(" IS NOT NULL");
        }
    };

    @DataProvider
    public Object[][] sqlData() {
        // Use longs for integer values because valueOf parses integers as Longs and
        // equals() is sensitive to the type.
        return new Object[][] {
                // @formatter:off
                { alwaysTrue(), "1 = 1" },
                { alwaysFalse(), "1 <> 1" },
                { equalTo(ptr("/name"), "alice"), "name = 'alice'"},
                { equalTo(ptr("/age"), 1234L), "age = 1234" },
                { equalTo(ptr("/balance"), 3.14159), "balance = 3.14159" },
                { equalTo(ptr("/isAdmin"), false), "isAdmin = false" },
                { lessThan(ptr("/age"), 1234L), "age < 1234" },
                { lessThanOrEqualTo(ptr("/age"), 1234L), "age <= 1234" },
                { greaterThan(ptr("/age"), 1234L), "age > 1234" },
                { greaterThanOrEqualTo(ptr("/age"), 1234L), "age >= 1234" },
                { contains(ptr("/name"), "al"), "name LIKE '%al%'" },
                { startsWith(ptr("/name"), "al"), "name LIKE 'al%'" },
                { present(ptr("/name")), "name IS NOT NULL" },
                { or(), "1 <> 1" }, // zero operand or is always false
                { and(), "1 = 1" }, // zero operand and is always true
                { or(equalTo(ptr("/age"), 1234L)), "age = 1234" }, // single operand or is no-op
                { and(equalTo(ptr("/age"), 1234L)), "age = 1234" }, // single operand and is no-op
                { or(lessThan(ptr("/age"), 18L), greaterThan(ptr("/age"), 30L)), "(age < 18 OR age > 30)" },
                { and(lessThan(ptr("/age"), 18L), greaterThan(ptr("/age"), 30L)), "(age < 18 AND age > 30)" },
                { or(equalTo(ptr("/role"), "a"), equalTo(ptr("/role"), "b"), equalTo(ptr("/role"), "c")),
                        "(role = 'a' OR role = 'b' OR role = 'c')" },
                { and(equalTo(ptr("/role"), "a"), equalTo(ptr("/role"), "b"), equalTo(ptr("/role"), "c")),
                        "(role = 'a' AND role = 'b' AND role = 'c')" },
                { or(equalTo(ptr("/role"), "a"), and(equalTo(ptr("/role"), "b"), equalTo(ptr("/role"), "c"))),
                        "(role = 'a' OR (role = 'b' AND role = 'c'))" },
                { and(equalTo(ptr("/role"), "a"), or(equalTo(ptr("/role"), "b"), equalTo(ptr("/role"), "c"))),
                        "(role = 'a' AND (role = 'b' OR role = 'c'))" },
                { not(equalTo(ptr("/age"), 1234L)), "NOT age = 1234" },
                { not(not(equalTo(ptr("/age"), 1234L))), "NOT NOT age = 1234" },
                // @formatter:on
        };
    }

    @Test(dataProvider = "sqlData")
    public void testToString(QueryFilter<JsonPointer> filter, String whereClause) {
        assertThat(filter.accept(visitor, null).toSQL()).isEqualTo(whereClause);
    }

    private static JsonPointer ptr(String jsonPointer){
        return new JsonPointer(jsonPointer);
    }
}

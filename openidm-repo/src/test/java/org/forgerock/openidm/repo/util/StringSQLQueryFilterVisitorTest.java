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

import org.forgerock.json.resource.QueryFilter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import org.forgerock.json.fluent.JsonPointer;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.forgerock.json.resource.QueryFilter.*;


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
                { equalTo("/name", "alice"), "name = 'alice'"},
                { equalTo("/age", 1234L), "age = 1234" },
                { equalTo("/balance", 3.14159), "balance = 3.14159" },
                { equalTo("/isAdmin", false), "isAdmin = false" },
                { lessThan("/age", 1234L), "age < 1234" },
                { lessThanOrEqualTo("/age", 1234L), "age <= 1234" },
                { greaterThan("/age", 1234L), "age > 1234" },
                { greaterThanOrEqualTo("/age", 1234L), "age >= 1234" },
                { contains("/name", "al"), "name LIKE '%al%'" },
                { startsWith("/name", "al"), "name LIKE 'al%'" },
                { present("/name"), "name IS NOT NULL" },
                { or(), "1 <> 1" }, // zero operand or is always false
                { and(), "1 = 1" }, // zero operand and is always true
                { or(equalTo("/age", 1234L)), "age = 1234" }, // single operand or is no-op
                { and(equalTo("/age", 1234L)), "age = 1234" }, // single operand and is no-op
                { or(lessThan("/age", 18L), greaterThan("/age", 30L)), "(age < 18 OR age > 30)" },
                { and(lessThan("/age", 18L), greaterThan("/age", 30L)), "(age < 18 AND age > 30)" },
                { or(equalTo("/role", "a"), equalTo("/role", "b"), equalTo("/role", "c")),
                        "(role = 'a' OR role = 'b' OR role = 'c')" },
                { and(equalTo("/role", "a"), equalTo("/role", "b"), equalTo("/role", "c")),
                        "(role = 'a' AND role = 'b' AND role = 'c')" },
                { or(equalTo("/role", "a"), and(equalTo("/role", "b"), equalTo("/role", "c"))),
                        "(role = 'a' OR (role = 'b' AND role = 'c'))" },
                { and(equalTo("/role", "a"), or(equalTo("/role", "b"), equalTo("/role", "c"))),
                        "(role = 'a' AND (role = 'b' OR role = 'c'))" },
                { not(equalTo("/age", 1234L)), "NOT age = 1234" },
                { not(not(equalTo("/age", 1234L))), "NOT NOT age = 1234" },
                // @formatter:on
        };
    }

    @Test(dataProvider = "sqlData")
    public void testToString(QueryFilter filter, String whereClause) {
        assertThat(filter.accept(visitor, null).toSQL()).isEqualTo(whereClause);
    }


}

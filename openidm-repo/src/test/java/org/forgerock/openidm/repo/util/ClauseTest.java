/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 ForgeRock AS. All rights reserved.
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

import static org.forgerock.openidm.repo.util.Clauses.where;
import static org.forgerock.openidm.repo.util.Clauses.not;
import static org.fest.assertions.api.Assertions.assertThat;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests basic Clause-to-SQL creation using a basic SQL syntax.
 */
public class ClauseTest {

    @DataProvider
    public Object[][] sqlData() {
        // Use longs for integer values because valueOf parses integers as Longs and
        // equals() is sensitive to the type.
        return new Object[][] {
                // @formatter:off
                { where("1 = 1"), "1 = 1" },
                { where("age < 18").or("age > 30"),
                        "(age < 18 OR age > 30)" },
                { where("age > 18").and("age < 30"),
                        "(age > 18 AND age < 30)" },
                { where("role = 'a'").or("role = 'b'").or("role = 'c'"),
                        "(role = 'a' OR role = 'b' OR role = 'c')" },
                { where("role = 'a'").and("role = 'b'").and("role = 'c'"),
                        "(role = 'a' AND role = 'b' AND role = 'c')" },
                { where("role = 'a'").or(where("role = 'b'").and("role = 'c'")),
                        "(role = 'a' OR (role = 'b' AND role = 'c'))" },
                { where("role = 'a'").and(where("role = 'b'").or("role = 'c'")),
                        "(role = 'a' AND (role = 'b' OR role = 'c'))" },
                // three flavors of not invocation
                { where("age = 1234").not(), "NOT age = 1234" },
                { not("age = 1234"), "NOT age = 1234" },
                { not(where("age = 1234")), "NOT age = 1234" },
                // two flavors of double-not invocation
                { where("age = 1234").not().not(), "NOT NOT age = 1234" },
                { not(not("age = 1234")), "NOT NOT age = 1234" },
                { not(not(where("age = 1234"))), "NOT NOT age = 1234" },

                // misc tests
                { where("a").and("b").or("c"), "((a AND b) OR c)" },
                { where("a").or(where("b").and(not("c"))), "(a OR (b AND NOT c))"}
                // @formatter:on
        };
    }

    @Test(dataProvider = "sqlData")
    public void testToSQL(Clause clause, String sql) {
        assertThat(clause.toSQL()).isEqualTo(sql);
    }

    // tests equality (of toString) method if we accidentally forget to call Clause#toSQL()
    @Test(dataProvider = "sqlData")
    public void testToString(Clause clause, String sql) {
        assertThat(clause.toString()).isEqualTo(sql);
    }
}

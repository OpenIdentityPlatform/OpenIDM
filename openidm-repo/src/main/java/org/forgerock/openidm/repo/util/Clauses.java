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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.guava.common.base.Function;
import org.forgerock.guava.common.collect.FluentIterable;

/**
 * A utility class containing factory methods for creating {@link Clause}s.
 */
public final class Clauses {

    /**
     * An abstract clause implementation to combine clauses with each other through
     * the {@link Clause} interface's and(), or(), and not() methods.
     */
    private static abstract class AbstractClause implements Clause {
        public Clause and(String clause) {
            return and(where(clause));
        }
        public Clause and(Clause and) {
            return new AndClause(this, and);
        }
        public Clause or(String clause) {
            return or(where(clause));
        }
        public Clause or(Clause or) {
            return new OrClause(this, or);
        }
        public Clause not() {
            return new NotClause(this);
        }
        @Override
        public String toString() {
            return toSQL();
        }
    }

    /**
     * A simple clause to wrap a String expression.
     */
    private static class SimpleClause extends AbstractClause implements Clause {
        private final String clause;

        SimpleClause(String clause) {
            this.clause = clause;
        }
        public String toSQL() {
            return clause;
        }
    }

    /**
     * An abstract composite clause implementation to join multiple clauses using AND or OR.
     */
    private abstract static class CompositeClause extends AbstractClause implements Clause {
        protected final List<Clause> clauses = new ArrayList<Clause>();

        CompositeClause(Clause... clauses) {
            this(Arrays.asList(clauses));
        }
        CompositeClause(Iterable<Clause> clauses) {
            for (Clause clause : clauses) {
                this.clauses.add(clause);
            }
        }

        String toSQL(String operand) {
            return new StringBuffer("(")
                    .append(StringUtils.join(FluentIterable.from(clauses)
                            .transform(new Function<Clause, String>() {
                                @Override
                                public String apply(Clause value) {
                                    return value.toSQL();
                                }
                            }), operand))
                    .append(")")
                    .toString();
        }
    }

    /**
     * A composite clause that models an OR-expression of multiple clauses.
     */
    private static class OrClause extends CompositeClause implements Clause {
        OrClause(Clause... clauses) {
            super(clauses);
        }
        OrClause(Iterable<Clause> clauses) {
            super(clauses);
        }
        @Override
        public Clause and(Clause clause) {
            return new AndClause(Arrays.asList(this, clause));
        }
        @Override
        public Clause or(Clause clause) {
            clauses.add(clause);
            return this;
        }
        public String toSQL() {
            return super.toSQL(" OR ");
        }
    }

    /**
     * A composite clause that models an AND-expression of multiple clauses.
     */
    private static class AndClause extends CompositeClause implements Clause {
        AndClause(Clause... clauses) {
            super(Arrays.asList(clauses));
        }
        AndClause(Iterable<Clause> clauses) {
            super(clauses);
        }
        @Override
        public Clause and(Clause clause) {
            clauses.add(clause);
            return this;
        }
        @Override
        public OrClause or(Clause clause) {
            return new OrClause(Arrays.asList(this, clause));
        }
        public String toSQL() {
            return super.toSQL(" AND ");
        }
    }

    /**
     * A clause that models the negation of another clause.
     */
    private static class NotClause extends AbstractClause implements Clause {
        private final Clause clause;
        NotClause(Clause clause) {
            this.clause = clause;
        }
        public String toSQL() {
            return "NOT " + clause.toSQL();
        }
    }

    /**
     * Returns a new where-clause from the provided String expression.
     *
     * @param clause a clause/expression
     * @return a Clause object
     */
    public static Clause where(String clause) {
        return new SimpleClause(clause);
    }

    /**
     * Returns a not-clause from the provided String expression.
     *
     * @param clause a clause/expression
     * @return a Clause object
     */
    public static Clause not(String clause) {
        return new NotClause(where(clause));
    }

    /**
     * Returns a negative of the provided clause.
     *
     * @param clause a clause/expression
     * @return a Clause object
     */
    public static Clause not(Clause clause) {
        return new NotClause(clause);
    }

    /**
     * Returns a composite "AND" clause of the provided constituent clauses.
     *
     * @param clauses a iterable (list) of clauses
     * @return a composite "and" clause
     */
    public static Clause and(Iterable<Clause> clauses) {
        return new AndClause(clauses);
    }

    /**
     * Returns a composite "OR" clause of the provided constituent clauses.
     *
     * @param clauses a iterable (list) of clauses
     * @return a composite "or" clause
     */
    public static Clause or(Iterable<Clause> clauses) {
        return new OrClause(clauses);
    }

    private Clauses() {
        // no construction
    }
}


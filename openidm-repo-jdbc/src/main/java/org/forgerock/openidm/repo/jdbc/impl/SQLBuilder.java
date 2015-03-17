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
package org.forgerock.openidm.repo.jdbc.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.guava.common.base.Function;
import org.forgerock.guava.common.collect.FluentIterable;
import org.forgerock.openidm.config.enhanced.InternalErrorException;
import org.forgerock.openidm.repo.util.Clause;
import org.forgerock.openidm.repo.util.SQLRenderer;

/**
 * An {@link org.forgerock.openidm.repo.util.SQLRenderer} that models an SQL SELECT statement and
 * renders table joins as part of the where clause.  Abstract so database implementations may subclass
 * for final SQL assembly, most notably with paging and offset query idioms.
 * <p>
 * For instance, a simple MySQL expression-builder might employ:
 * <pre>
 * SQLBuilder builder = new SQLBuilder() {
 *     public String toSQL() {
 *         return "SELECT " + getColumns().toSQL()
 *                 + getFromClause().toSQL()
 *                 + getJoinClause().toSQL()
 *                 + getWhereClause().toSQL()
 *                 + getOrderByClause().toSQL()
 *                 + " LIMIT " + pageSizeParam
 *                 + " OFFSET " + offsetParam;
 *     }
 * }
 * </pre>
 */
abstract class SQLBuilder implements SQLRenderer<String> {

    /**
     * Renders a select column.
     */
    private class Column implements SQLRenderer<String> {
        final String column;

        Column(String column) {
            this.column = column;
        }

        public String toSQL() {
            return column;
        }
    }

    /**
     * Renders a table with an option alias.
     */
    private class Table implements SQLRenderer<String> {
        final String table;
        final String alias;

        Table(String table, String alias) {
            this.table = table;
            this.alias = alias;
        }

        public String toSQL() {
            return table + (alias != null ? " " + alias : "");
        }
    }

    /**
     * Models the type of table join.
     */
    private enum JoinType implements SQLRenderer<String> {
        INNER {
            @Override
            public String toSQL() { return "INNER JOIN"; }
        },
        LEFT_OUTER {
            @Override
            public String toSQL() { return "LEFT OUTER JOIN"; }
        },
        RIGHT_OUTER {
            @Override
            public String toSQL() { return "RIGHT OUTER JOIN"; }
        }
    }

    /**
     * Models/renders a table join.
     */
    class Join implements SQLRenderer<String> {
        final JoinType type;
        final Table table;
        final Clause onClause;
        final SQLBuilder builder;

        /**
         * Construct the first pass of a join with the type, table, and alias.
         *
         * @param builder the calling SQLBuilder
         * @param type the type of join
         * @param table the table name
         * @param alias the table alias
         */
        Join(SQLBuilder builder, JoinType type, String table, String alias) {
            this.builder = builder;
            this.type = type;
            this.table = new Table(table, alias);
            this.onClause = null;
        }

        /**
         * Construct the second pass of a join with the type, table clause, and on clause.
         *
         * @param builder the calling SQLBuilder
         * @param type the type of join
         * @param table the table object
         * @param clause the on clause
         */
        Join(SQLBuilder builder, JoinType type, Table table, Clause clause) {
            this.builder = builder;
            this.type = type;
            this.table = table;
            this.onClause = clause;
        }

        /**
         * Complete this join on the on-clause.
         *
         * @param clause the on clause
         * @return the calling SQLBuilder with this completed join added
         */
        SQLBuilder on(Clause clause) {
            builder.addJoin(new Join(builder, type, table, clause));
            return builder;
        }

        @Override
        public String toSQL() {
            return new StringBuilder(type.toSQL())
                    .append(" ")
                    .append(table.toSQL())
                    .append(" ON ")
                    .append(onClause.toSQL())
                    .toString();
        }
    }

    /**
     * Renders an ORDER BY clause.
     */
    private class OrderBy implements SQLRenderer<String> {
        final String order;
        final boolean ascending;

        OrderBy(String order, boolean ascending) {
            this.order = order;
            this.ascending = ascending;
        }

        public String toSQL() {
            return order + " " + (ascending ? "ASC" : "DESC");
        }
    }

    private final List<SQLRenderer<String>> columns = new ArrayList<SQLRenderer<String>>();
    private final List<SQLRenderer<String>> tables = new ArrayList<SQLRenderer<String>>();
    private final List<SQLRenderer<String>> joins = new ArrayList<SQLRenderer<String>>();
    // the where clause is not final because it is not set at build time
    private SQLRenderer<String> whereClause = null;
    private final List<SQLRenderer<String>> orderBys = new ArrayList<SQLRenderer<String>>();

    /**
     * Add a column.
     *
     * @param column the column to add to the select list
     * @return the builder
     */
    SQLBuilder addColumn(String column) {
        columns.add(new Column(column));
        return this;
    }

    /**
     * Select from a particular table.
     *
     * @param table a table to select from
     * @return the builder
     */
    SQLBuilder from(String table) {
        return from(table, null);
    }

    /**
     * Select from a particular aliased table.
     *
     * @param table the table to select from
     * @param alias the table alias
     * @return the builder
     */
    SQLBuilder from(String table, String alias) {
        tables.add(new Table(table, alias));
        return this;
    }

    /**
     * Create a left [outer] table join.
     *
     * @param table the table to join
     * @return the Join
     */
    Join leftJoin(String table) {
        return leftJoin(table, null);
    }

    /**
     * Create an aliased left [outer] table join.
     *
     * @param table the table to join
     * @param alias the table's alias
     * @return the Join
     */
    Join leftJoin(String table, String alias) {
        return join(JoinType.LEFT_OUTER, table, alias);
    }

    /**
     * Create a right [outer] table join.
     *
     * @param table the table to join
     * @return the Join
     */
    Join rightJoin(String table) {
        return rightJoin(table, null);
    }

    /**
     * Create an aliased right [outer] table join.
     *
     * @param table the table to join
     * @param alias the table's alias
     * @return the Join
     */
    Join rightJoin(String table, String alias) {
        return join(JoinType.RIGHT_OUTER, table, alias);
    }

    /**
     * Create an inner table join.
     *
     * @param table the table to join
     * @return the Join
     */
    Join join(String table) {
        return join(table, null);
    }

    /**
     * Create an aliased table join.
     *
     * @param table the table to join
     * @param alias the table's alias
     * @return the Join
     */
    Join join(String table, String alias) {
        return join(JoinType.INNER, table, alias);
    }

    /**
     * Create a join given the type, table, and alias.
     *
     * @param type the join type
     * @param table the table to join
     * @param alias the alias for the table to join
     * @return
     */
    private Join join(JoinType type, String table, String alias) {
        return new Join(this, type, table, alias);
    }

    private SQLBuilder addJoin(Join join) {
        joins.add(join);
        return this;
    }

    /**
     * Set the where clause.
     *
     * @param whereClause the WhereClause
     * @return the builder
     */
    SQLBuilder where(Clause whereClause) {
        this.whereClause = whereClause;
        return this;
    }

    /**
     * Add an order-by clause.
     *
     * @param orderBy the order-by clause
     * @param ascending whether it is ascending
     * @return the builder
     */
    SQLBuilder orderBy(String orderBy, boolean ascending) {
        this.orderBys.add(new OrderBy(orderBy, ascending));
        return this;
    }

    /** Function to render the SQL from a renderer. */
    private static final Function<SQLRenderer<String>, String> TO_SQL =
            new Function<SQLRenderer<String>, String>() {
                @Override
                public String apply(SQLRenderer<String> renderer) {
                    return renderer.toSQL();
                }
            };

    /**
     * Produce a Renderer to render the column list.
     *
     * @return a renderer for the column list
     */
    SQLRenderer<String> getColumns() {
        return new SQLRenderer<String>() {
            @Override
            public String toSQL() {
                return columns.isEmpty()
                    ? "*"
                    : StringUtils.join(FluentIterable.from(columns).transform(TO_SQL), ", ");
            }
        };
    }

    /**
     * Produce a Renderer to render the from clause.
     *
     * @return a renderer for the from clause
     */
    SQLRenderer<String> getFromClause() {
        if (tables.isEmpty()) {
            throw new InternalErrorException("SQL query contains no tables in FROM clause");
        }

        return new SQLRenderer<String>() {
            @Override
            public String toSQL() {
                return " FROM " + StringUtils.join(FluentIterable.from(tables).transform(TO_SQL), ", ");
            }
        };
    }

    private static final SQLRenderer<String> NO_STRING =
            new SQLRenderer<String>() {
                @Override
                public String toSQL() {
                    return "";
                }
            };

    SQLRenderer<String> getJoinClause() {
        if (joins.isEmpty()) {
            return NO_STRING;
        }

        return new SQLRenderer<String>() {
            @Override
            public String toSQL() {
                return " " + StringUtils.join(FluentIterable.from(joins).transform(TO_SQL), " ");
            }
        };
    }

    /**
     * Produce a Renderer to render the where clause.
     *
     * @return a renderer for the where clause
     */
    SQLRenderer<String> getWhereClause() {
        return new SQLRenderer<String>() {
            @Override
            public String toSQL() {
                return " WHERE " + whereClause.toSQL();
            }
        };
    }

    /**
     * Produce a Renderer to render the order-by clause.
     *
     * @return a renderer for the order-by clause
     */
    SQLRenderer<String> getOrderByClause() {
        if (orderBys.isEmpty()) {
            return NO_STRING;
        }

        return new SQLRenderer<String>() {
            @Override
            public String toSQL() {
                return " ORDER BY " + StringUtils.join(FluentIterable.from(orderBys).transform(TO_SQL), ", ");
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract String toSQL();

    /**
     * Return a string representation of this builder.
     *
     * @return the SQL string created by this builder
     */
    @Override
    public String toString() {
        return toSQL();
    }
}

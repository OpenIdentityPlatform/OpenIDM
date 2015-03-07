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
import org.forgerock.openidm.config.enhanced.InternalErrorException;
import org.forgerock.openidm.repo.util.Clause;
import org.forgerock.openidm.repo.util.SQLRenderer;
import org.forgerock.util.Iterables;
import org.forgerock.util.promise.Function;
import org.forgerock.util.promise.NeverThrowsException;

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
     * Models an inner join.
     */
    class Join {
        final Table table;
        final Clause onClause;
        final SQLBuilder builder;

        Join(SQLBuilder builder, String table, String alias) {
            this.builder = builder;
            this.table = new Table(table, alias);
            this.onClause = null;
        }

        Join(SQLBuilder builder, Table table, Clause clause) {
            this.builder = builder;
            this.table = table;
            this.onClause = clause;
        }

        SQLBuilder on(Clause clause) {
            builder.addJoin(new Join(builder, table, clause));
            return builder;
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
    private final List<Join> joins = new ArrayList<Join>();
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
     * Create a table join.
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
        return new Join(this, table, alias);
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
    private static final Function<SQLRenderer<String>, String, NeverThrowsException> TO_SQL =
            new Function<SQLRenderer<String>, String, NeverThrowsException>() {
                @Override
                public String apply(SQLRenderer<String> where) throws NeverThrowsException {
                    return where.toSQL();
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
                    : StringUtils.join(Iterables.from(columns).map(TO_SQL), ", ");
            }
        };
    }

    /**
     * Produce a Renderer to render the from clause.
     *
     * @return a renderer for the from clause
     */
    SQLRenderer<String> getFromClause() {
        final int tableSize =  tables.size() + joins.size();
        if (tableSize == 0) {
            throw new InternalErrorException("SQL query contains no tables in FROM clause");
        }
        final List<SQLRenderer<String>> allTables = new ArrayList<SQLRenderer<String>>(tableSize);
        allTables.addAll(tables);
        for (final Join join : joins) {
            allTables.add(join.table);
        }

        return new SQLRenderer<String>() {
            @Override
            public String toSQL() {
                return " FROM " + StringUtils.join(Iterables.from(allTables).map(TO_SQL), ", ");
            }
        };
    }

    /**
     * Produce a Renderer to render the where clause.
     *
     * @return a renderer for the where clause
     */
    SQLRenderer<String> getWhereClause() {
        final int whereSize = joins.size() + 1;
        final List<SQLRenderer<String>> allWhere = new ArrayList<SQLRenderer<String>>(whereSize);
        for (Join join : joins) {
            allWhere.add(join.onClause);
        }
        allWhere.add(whereClause);

        return new SQLRenderer<String>() {
            @Override
            public String toSQL() {
                return " WHERE " + StringUtils.join(Iterables.from(allWhere).map(TO_SQL), " AND ");
            }
        };
    }

    /**
     * Produce a Renderer to render the order-by clause.
     *
     * @return a renderer for the order-by clause
     */
    SQLRenderer<String> getOrderByClause() {
        return new SQLRenderer<String>() {
            @Override
            public String toSQL() {
                return orderBys.isEmpty()
                        ? ""
                        : " ORDER BY " + StringUtils.join(Iterables.from(orderBys).map(TO_SQL), ", ");
            }
        };
    }

}

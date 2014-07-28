/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2013 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.audit.impl;

import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceName;
import org.forgerock.json.resource.ServerContext;

import org.osgi.framework.BundleContext;

/**
 * OpenIDM audit logger
 * @author aegloff
 * @author brmiller
 */
public interface AuditLogger {

    /**
     * Set the audit logger configuration which is a logger specific
     * map
     * @param config the configuration
     */
    void setConfig(JsonValue config);

    /**
     * Cleanup called when auditlogger no longer needed
     */
    void cleanup();

    /**
     * Whether this audit logger is used for reads/queries, when multiple
     * loggers are configured.
     *
     * @return whether to use this logger for reads/queries.
     */
    boolean isUsedForQueries();

    /**
     * Returns whether to ignore logging failures
     *
     * @return whether to ignore logging failures
     */
    boolean isIgnoreLoggingFailures();

    /**
     * Creates a new object in the object set.
     * <p>
     * On completion, this method sets the {@code _id} property to the assigned identifier for
     * the object, and the {@code _rev} property to object version if optimistic concurrency
     * is supported.
     *
     * @param type the requested type of audit object to create.
     * @param object the contents of the object to create in the object set.
     * @throws ForbiddenException if access to the object or object set is forbidden.
     */
    void create(ServerContext context, String type, Map<String, Object> object) throws ResourceException;

    /**
     * Reads an object from the object set.
     * <p>
     * The returned object will contain metadata properties, including relative object
     * identifier {@code _id}. If optimistic concurrency is supported, the object version
     * {@code _rev} will be set in the returned object. If optimistic concurrency is not
     * supported, then {@code _rev} must be {@code null} or absent.
     *
     * @param type the type of audit log entry to read
     * @param localId the id of the object to log
     * @throws NotFoundException if the specified object could not be found.
     * @throws ForbiddenException if access to the object is forbidden.
     * @return the requested object.
     */
    Map<String, Object> read(ServerContext context, String type, String localId) throws ResourceException;

    /**
     * Performs a query on the specified object and returns the associated result. The
     * execution of a query is not allowed to incur side effects.
     * <p>
     * Queries are parametric; a set of named parameters is provided as the query criteria.
     * The query result is a JSON object structure composed of basic Java types; its overall
     * structure is defined by the implementation.
     *
     * @param type type of audit log entry for which to query
     * @param params the parameters of the query to perform.
     * @param formatted
     * @return the query result object.
     * @throws NotFoundException if the specified object could not be found.
     * @throws ForbiddenException if access to the object or the specified query is forbidden.
     */
    Map<String, Object> query(ServerContext context, String type, Map<String, String> params, boolean formatted) throws ResourceException;

}

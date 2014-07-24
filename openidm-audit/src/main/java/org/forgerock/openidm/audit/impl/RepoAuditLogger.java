/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All rights reserved.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.audit.AuditService;
import org.forgerock.openidm.config.enhanced.InvalidException;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audit logger that logs to a repository
 *
 * @author aegloff
 * @author brmiller
 */
public class RepoAuditLogger extends AbstractAuditLogger implements AuditLogger {
    final static Logger logger = LoggerFactory.getLogger(RepoAuditLogger.class);

    /**
     * Event names for monitoring audit behavior
     */
    public static final Name EVENT_AUDIT_CREATE = Name.get("openidm/internal/audit/repo/create");

    /** provides a reference to the router for real-time query handling */
    private ConnectionFactory connectionFactory;

    /**
     * Constructor.
     *
     * @param connectionFactory
     */
    public RepoAuditLogger(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void setConfig(Map config, BundleContext ctx) throws InvalidException {
        super.setConfig(config, ctx);
    }

    public void cleanup() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> read(ServerContext context, String type, String id) throws ResourceException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("_queryId", "query-all");
        params.put("fields", "*");
        Map<String, Object> result = new HashMap<String, Object>();

        List<Map<String, Object>> entries = new ArrayList<Map<String, Object>>();
        if (id == null) {
            QueryRequest request = Requests.newQueryRequest(getRepoTarget(type));
            request.setQueryId("query-all");
            request.getAdditionalParameters().putAll(params);
            Set<Resource> r = new HashSet<Resource>();
            connectionFactory.getConnection().query(context, request, r);
            for (Resource entry : r) {
                entries.add(AuditServiceImpl.formatLogEntry(entry.getContent().asMap(), type));
            }
            formatActivityList(entries);
            result.put("entries", entries);
        } else {
            ReadRequest request = Requests.newReadRequest(getRepoTarget(type), id);
            Map<String, Object> entry = connectionFactory.getConnection().read(context, request).getContent().asMap();
            formatActivityEntry(entry);
            result = AuditServiceImpl.formatLogEntry(entry, type);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> query(ServerContext context, String type, Map<String, String> params, boolean formatted) throws ResourceException {
        String queryId = params.get("_queryId");
        try {
            QueryRequest request = Requests.newQueryRequest(getRepoTarget(type));
            request.setQueryId(queryId);
            request.getAdditionalParameters().putAll(params);
            final List<Map<String, Object>> queryResults = new ArrayList<Map<String, Object>>();
            connectionFactory.getConnection().query(context, request, new QueryResultHandler() {
                @Override
                public void handleError(ResourceException error) {
                    // Continue
                }

                @Override
                public boolean handleResource(Resource resource) {
                    queryResults.add(resource.getContent().asMap());
                    return true;
                }

                @Override
                public void handleResult(QueryResult result) {
                    // Ignore
                }
            });
            if (type.equals(AuditServiceImpl.TYPE_RECON)) {
                return AuditServiceImpl.getReconResults(queryResults, formatted);
            } else if (type.equals(AuditServiceImpl.TYPE_ACTIVITY)) {
                formatActivityList(queryResults);
                return AuditServiceImpl.getActivityResults(queryResults, formatted);
            } else if (type.equals(AuditServiceImpl.TYPE_ACCESS)) {
                return AuditServiceImpl.getAccessResults(queryResults, formatted);
            } else {
                throw new BadRequestException("Unsupported queryId " +  queryId + " on type " + type);
            }
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    public void formatActivityList(List<Map<String, Object>> entryList) {
        for (Map<String, Object> entry : entryList) {
            formatActivityEntry(entry);
        }
    }

    public void formatActivityEntry(Map<String, Object> entry) {
        Object beforeValue = entry.get("before");
        Object afterValue = entry.get("after");
        if (beforeValue != null) {
            entry.put("before", AuditServiceImpl.parseJsonString((String)beforeValue).getObject());
        }
        if (afterValue != null) {
            entry.put("after", AuditServiceImpl.parseJsonString((String)afterValue).getObject());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create(ServerContext context, String type, Map<String, Object> obj) throws ResourceException {
        EventEntry measure = Publisher.start(EVENT_AUDIT_CREATE, obj, null);
        try {
            AuditServiceImpl.preformatLogEntry(type, obj);
            createImpl(context, type, obj);
        } finally {
            measure.end();
        }
    }

    private void createImpl(ServerContext context, String type, Map<String, Object> obj) throws ResourceException {
        CreateRequest request = Requests.newCreateRequest(
                getRepoTarget(type), (String) obj.get(Resource.FIELD_CONTENT_ID), new JsonValue(obj));
        connectionFactory.getConnection().create(context, request);
    }

    private String getRepoTarget(String type) {
        return new StringBuilder("/repo").append(AuditService.ROUTER_PREFIX).append("/").append(type).toString();
    }
}

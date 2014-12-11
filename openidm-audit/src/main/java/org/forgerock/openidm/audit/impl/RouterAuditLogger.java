/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013-2014 ForgeRock AS.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.config.enhanced.InvalidException;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audit logger that logs to a router target.
 */
public class RouterAuditLogger extends AbstractAuditLogger implements AuditLogger {
    final static Logger logger = LoggerFactory.getLogger(RouterAuditLogger.class);

    /** config property for the router target */
    public final static String CONFIG_LOG_LOCATION = "location";

    /** Event names for monitoring audit behavior */
    public static final Name EVENT_AUDIT_CREATE = Name.get("openidm/internal/audit/router/create");

    /** Jackson parser */
    final ObjectMapper mapper = new ObjectMapper();

    /** the router target */
    private String location;


    /** provides a reference to the router for real-time query handling */
    private ConnectionFactory connectionFactory;

    /**
     * Constructor.
     *
     * @param connectionFactory
     */
    public RouterAuditLogger(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void setConfig(JsonValue config) throws InvalidException {
        super.setConfig(config);
        location = config.get(CONFIG_LOG_LOCATION).asString();
        if (location == null || location.length() == 0) {
            throw new InvalidException("Configured router location must not be empty");
        }
        logger.info("Audit logging to: {}", location);
    }

    public void cleanup() {
    }

    /**
     * If we're logging on the router, wrap the current ServerContext with an AuditContext to
     * indicate this is an audit log operation.  Necessary to avoid logging audit log events
     * if the configured router endpoint also logs CRUD operations (such as OpenICFProvisioner).
     *
     * @param context the ServerContext
     * @return a ServerContext indicating auditing
     */
    private ServerContext createAuditContext(ServerContext context) {
        return new AuditContext(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> read(ServerContext context, String type, String id) throws ResourceException {

        Map<String, Object> result = new HashMap<String, Object>();

        if (id == null) {
            Map<String, String> params = new HashMap<String, String>();
            params.put("_queryId", "query-all-ids");
            params.put("fields", "*");

            QueryRequest request = Requests.newQueryRequest(getRouterLocation(type));
            request.setQueryId("query-all-ids");
            request.getAdditionalParameters().putAll(params);
            Set<Resource> results = new HashSet<Resource>();
            connectionFactory.getConnection().query(createAuditContext(context), request, results);

            List<Map<String, Object>> entries = new ArrayList<Map<String, Object>>();
            for (Resource entry : results) {
                AuditServiceImpl.unflattenEntry(entry.getContent().asMap());
                        entries.add(
                                AuditServiceImpl.formatLogEntry(entry.getContent().asMap(), type));
            }
            result.put("entries", entries);
        } else {
            ReadRequest request = Requests.newReadRequest(getRouterLocation(type), id);
            Map<String, Object> entry = connectionFactory.getConnection().read(createAuditContext(context), request)
                    .getContent().asMap();
            AuditServiceImpl.unflattenEntry(entry);
            result = AuditServiceImpl.formatLogEntry(entry, type);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void query(ServerContext context, QueryRequest request, final QueryResultHandler handler, 
            final String type, final boolean formatted) throws ResourceException {
        try {
            QueryRequest newRequest = Requests.copyOfQueryRequest(request);
            newRequest.setResourceName(getRouterLocation(type));
            connectionFactory.getConnection().query(context, newRequest, new QueryResultHandler() {
                @Override
                public void handleError(ResourceException error) {
                    handler.handleError(error);
                }

                @Override
                public boolean handleResource(Resource resource) {
                    JsonValue content = resource.getContent();
                    if (formatted) {
                        if (type.equals(AuditServiceImpl.TYPE_RECON)) {
                            content = new JsonValue(AuditServiceImpl.formatReconEntry(content.asMap()));
                        } else if (type.equals(AuditServiceImpl.TYPE_ACTIVITY)) {
                            content = new JsonValue(AuditServiceImpl.formatActivityEntry(content.asMap()));
                        } else if (type.equals(AuditServiceImpl.TYPE_ACCESS)) {
                            content = new JsonValue(AuditServiceImpl.formatAccessEntry(content.asMap()));
                        }
                    }
                    return handler.handleResource(new Resource(resource.getId(), resource.getRevision(), content));
                }

                @Override
                public void handleResult(QueryResult result) {
                    handler.handleResult(result);
                }
            });
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void create(ServerContext context, String type, Map<String, Object> object) throws ResourceException {
        EventEntry measure = Publisher.start(EVENT_AUDIT_CREATE, object, null);

        try {
            AuditServiceImpl.preformatLogEntry(type, object);
            Map<String, Object> sanitized = sanitizeObject(object);
            CreateRequest request = Requests.newCreateRequest(getRouterLocation(type), new JsonValue(sanitized));
            connectionFactory.getConnection().create(createAuditContext(context), request);
        } catch (IOException e) {
            throw new InternalServerErrorException("Unable to stringify object to be logged", e);
        } finally {
            measure.end();
        }
    }

    /**
     * Pre-sanitize data types that need special handling before being sent to the router.
     *
     * @param obj the object being logged
     * @return a sanitized object
     * @throws IOException on failure for Mapper to convert object to a string
     */
    private Map<String, Object> sanitizeObject(Map<String, Object> obj) throws IOException {
        Map<String, Object> sanitized = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : obj.entrySet()) {
            // we know, for example, that if the router target is an openicf connector,
            // it may not be able to stringify Maps on its own
            // TODO This should be validated that it is safe for all router targets;
            // i.e, if it's just an OpenICF thing, we should continue moving it to the
            // OpenICFProvisionerService
            if (entry.getValue() instanceof Map) {
                sanitized.put(entry.getKey(), mapper.writeValueAsString(entry.getValue()));
            } else if (entry.getValue() instanceof List) {
                sanitized.put(entry.getKey(), mapper.writeValueAsString(entry.getValue()));
            }
            else {
                sanitized.put(entry.getKey(), entry.getValue());
            }
        }
        return sanitized;
    }

    private String getRouterLocation(String type) {
        return new StringBuilder(location).append("/").append(type).toString();
    }

}

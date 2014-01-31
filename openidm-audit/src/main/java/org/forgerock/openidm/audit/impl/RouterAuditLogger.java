/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All rights reserved.
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
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audit logger that logs to a router target.
 *
 * @author brmiller
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

    public void setConfig(Map config, BundleContext ctx) throws InvalidException {
        super.setConfig(config, ctx);
        location = (String) config.get(CONFIG_LOG_LOCATION);
        if (location == null || location.length() == 0) {
            throw new InvalidException("Configured router location must not be empty");
        }
        logger.info("Audit logging to: {}", location);
    }

    public void cleanup() {
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
            connectionFactory.getConnection().query(context, request, results);

            List<Map<String, Object>> entries = new ArrayList<Map<String, Object>>();
            for (Resource entry : results) {
                entries.add(
                        AuditServiceImpl.formatLogEntry(
                            unflattenActivityEntry(entry.getContent().asMap()), type));
            }
            result.put("entries", entries);
        } else {
            ReadRequest request = Requests.newReadRequest(getRouterLocation(type), id);
            Map<String, Object> entry = connectionFactory.getConnection().read(context, request).getContent().asMap();
            result = AuditServiceImpl.formatLogEntry(unflattenActivityEntry(entry), type);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> query(ServerContext context, String type, Map<String, String> params)
        throws ResourceException {

        try {
            boolean formatted = true;
            if (params.get("formatted") != null
                    && !AuditServiceImpl.getBoolValue(params.get("formatted"))) {
                formatted = false;
            }

            QueryRequest request = Requests.newQueryRequest(getRouterLocation(type));
            request.setQueryId(params.get("_queryId"));
            request.getAdditionalParameters().putAll(params);
            final List<Map<String, Object>> queryResults = new ArrayList<Map<String, Object>>();
            connectionFactory.getConnection().query(context, request,
                    new QueryResultHandler() {
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

            if (AuditServiceImpl.TYPE_RECON.equals(type)) {
                return AuditServiceImpl.getReconResults(queryResults, formatted);
            } else if (AuditServiceImpl.TYPE_ACTIVITY.equals(type)) {
                return AuditServiceImpl.getActivityResults(unflattenActivityList(queryResults), formatted);
            } else if (AuditServiceImpl.TYPE_ACCESS.equals(type)) {
                return AuditServiceImpl.getAccessResults(queryResults, formatted);
            } else {
                String queryId = params.get("_queryId");
                throw new BadRequestException("Unsupported queryId " + queryId + " on type " + type);
            }
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
            connectionFactory.getConnection().create(context, request);
        } catch (IOException e) {
            throw new InternalServerErrorException("Unable to stringify object to be logged", e);
        } finally {
            measure.end();
        }
    }


    /**
     * As a consequence of "santizeObject" (below), certain structures may be
     * "flattened" on write, and must be re-inflated on read.  This is a list of
     * attributes known to be Maps that were flattened.
     */
    private static final String[] MAP_ATTRIBUTES_TO_UNFLATTEN = new String[] {
            "before", "after", "messageDetail"
    };

    /**
     * Format the activity entry by unflattening JSON-Maps.
     *
     * @param entry the Map of attributes read from the router that may contain
     *              JSON-Map data as values
     * @return the unflattened entry
     */
    private Map<String, Object> unflattenActivityEntry(Map<String, Object> entry) {
        for (String attribute : MAP_ATTRIBUTES_TO_UNFLATTEN) {
            if (entry.get(attribute) != null) {
                entry.put(attribute, AuditServiceImpl.parseJsonString((String) entry.get(attribute)).getObject());
            }
        }
        return entry;
    }

    /**
     * format each activity entry in the list by unflattening JSON-Maps.
     *
     * @param entryList the list of activity entries
     * @return the list of unflattened entries
     */
    private List<Map<String, Object>> unflattenActivityList(List<Map<String, Object>> entryList) {
        for (Map<String, Object> entry : entryList) {
            unflattenActivityEntry(entry);
        }
        return entryList;
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

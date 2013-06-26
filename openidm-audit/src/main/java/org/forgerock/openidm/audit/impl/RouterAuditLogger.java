/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All rights reserved.
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
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Reference;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceAccessor;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.audit.AuditService;
import org.forgerock.openidm.audit.util.ActivityLog;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.ObjectSetContext;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.openidm.util.Accessor;
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
    private Accessor<JsonResource> routerReference;
    // TODO revisit Accessor<?> and name of this reference indirection member 

    /**
     * Constructor.
     *
     * @param routerAccessor an accessor to the router
     */
    public RouterAuditLogger(Accessor<JsonResource> routerReference) {
        super();
        this.routerReference = routerReference;
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
     * Interface to perform an operation on/with/using a router accessor.
     *
     * @param <R> The arbitrary return type of the operation.
     */
    private interface RouterAccessorOperation<R> {
        public R execute(JsonResourceAccessor accessor) throws ObjectSetException;
    }

    /**
     * A utility function to send a message to the router, using the provided router accessor.
     * This is a convenience method to abstract the boilerplate of
     * <ul>
     *     <li>accessing the router via <tt>routerReference</tt></li>
     *     <li>obtaining the context and adding our "logging marker"</li>
     *     <li>calling the router with our read/query/create</li>
     *     <li>removing the logging marker from the context when finished</li>
     * </ul>
     *
     * @param operation A functional unit of work (namely, sending a message on the router to log)
     * @param <R> the return type from this operation
     * @return the return value of the operation
     * @throws ObjectSetException on failure to send message to router
     */
    private <R> R sendToRouter(RouterAccessorOperation<R> operation) throws ObjectSetException {
        final JsonResource router = routerReference.access();
        if (router == null)
            throw new InternalServerErrorException("Router unavailable");

        final JsonValue context = ObjectSetContext.get();
        try {
            ActivityLog.enterLogActivity(context, this.getClass().getName());
            return operation.execute(new JsonResourceAccessor(router, context));
        } finally {
            ActivityLog.exitLogActivity(context);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> read(final String fullId) throws ObjectSetException {
        final String[] split = AuditServiceImpl.splitFirstLevel(fullId);
        final String type = split[0];
        final String id = split[1];

        return sendToRouter(
                new RouterAccessorOperation<Map<String, Object>>() {
                    public Map<String, Object> execute(JsonResourceAccessor accessor) throws ObjectSetException {
                        try {
                            Map<String, Object> result = new HashMap<String, Object>();

                            if (id == null) {
                                Map<String, Object> params = new HashMap<String, Object>();
                                params.put("_queryId", "query-all-ids");
                                params.put("fields", "*");
                                Map<String, Object> queryResult = accessor.query(
                                        location + "/" + fullId,  new JsonValue(params))
                                    .asMap();
                                List<Map<String, Object>> entries = new ArrayList<Map<String, Object>>();
                                for (Map<String, Object> entry :
                                        (List<Map<String, Object>>) queryResult.get(QueryConstants.QUERY_RESULT)) {
                                    entries.add(AuditServiceImpl.formatLogEntry(unflattenActivityEntry(entry), type));
                                }
                                result.put("entries", entries);
                            } else {
                                Map<String, Object> entry = accessor.read(location + "/" + fullId).asMap();
                                result = AuditServiceImpl.formatLogEntry(unflattenActivityEntry(entry), type);
                            }

                            return result;
                        } catch (JsonResourceException e) {
                            throw new InternalServerErrorException("Unable to route " + fullId + " to " + location, e);
                        }
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> query(final String fullId, final Map<String, Object> params) throws ObjectSetException {
        final String[] split = AuditServiceImpl.splitFirstLevel(fullId);
        final String type = split[0];
        final String id = split[1];

        return sendToRouter(
                new RouterAccessorOperation<Map<String, Object>>() {
                    public Map<String, Object> execute(JsonResourceAccessor accessor) throws ObjectSetException {
                        try {
                            boolean formatted = true;
                            if (params.get("formatted") != null 
                                    && !AuditServiceImpl.getBoolValue(params.get("formatted"))) {
                                formatted = false;
                            }
                            Map<String, Object> queryResults = accessor.query(
                                    location + "/" + fullId, new JsonValue(params)).asMap();
                            List<Map<String, Object>> entryList = 
                                (List<Map<String, Object>>) queryResults.get(QueryConstants.QUERY_RESULT);
                        
                            if (AuditServiceImpl.TYPE_RECON.equals(type)) {
                                return AuditServiceImpl.getReconResults(
                                        entryList, (String)params.get("reconId"), formatted);
                            } else if (AuditServiceImpl.TYPE_ACTIVITY.equals(type)) {
                                return AuditServiceImpl.getActivityResults(unflattenActivityList(entryList), formatted);
                            } else if (AuditServiceImpl.TYPE_ACCESS.equals(type)) {
                                return AuditServiceImpl.getAccessResults(entryList, formatted);
                            } else {
                                String queryId = (String) params.get("_queryId");
                                throw new BadRequestException("Unsupported queryId " + queryId + " on type " + type);
                            }
                        } catch (Exception e) {
                            throw new BadRequestException(e);
                        }
                    }
                });
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void create(final String fullId, final Map<String, Object> obj) throws ObjectSetException {
        EventEntry measure = Publisher.start(EVENT_AUDIT_CREATE, obj, null);
        final String[] split = AuditServiceImpl.splitFirstLevel(fullId);
        final String type = split[0];
        final String id = split[1];

        try {
            sendToRouter(
                    new RouterAccessorOperation<Void>() {
                        public Void execute(JsonResourceAccessor accessor) throws ObjectSetException {
                            try {
                                AuditServiceImpl.preformatLogEntry(type, obj);
                                Map<String, Object> sanitized = sanitizeObject(obj);
                                accessor.create(location + "/" + fullId, new JsonValue(sanitized));
                                return null;
                            } catch (IOException e) {
                                throw new InternalServerErrorException("Unable to stringify object to be logged", e);
                            } catch (JsonResourceException e) {
                                throw new InternalServerErrorException("Unable to route " + fullId + " to " + location, e);
                            }
                        }
                    });

        } finally {
            measure.end();
        }
    }

    /**
     * Audit service does not support changing audit entries.
     */
    @Override
    public void update(String fullId, String rev, Map<String, Object> obj) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on audit service");
    }

    /**
     * Audit service currently does not support deleting audit entries.
     */ 
    @Override
    public void delete(String fullId, String rev) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on audit service");
    }

    /**
     * Audit service does not support changing audit entries.
     */
    @Override
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on audit service");
    }

    /**
     * Audit service does not support actions on audit entries.
     */
    @Override
    public Map<String, Object> action(String id, Map<String, Object> params) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on audit service");
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

}

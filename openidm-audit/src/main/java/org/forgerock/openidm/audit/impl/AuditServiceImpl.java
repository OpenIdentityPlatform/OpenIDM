/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;

import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

import org.forgerock.openidm.audit.util.Action;
import org.forgerock.openidm.audit.AuditService;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ConflictException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.repo.RepositoryService; 


/**
 * Audit module
 * @author aegloff
 */
@Component(name = "org.forgerock.openidm.audit", immediate=true, policy=ConfigurationPolicy.REQUIRE)
@Service
@Properties({
    @Property(name = "service.description", value = "Audit Service"),
    @Property(name = "service.vendor", value = "ForgeRock AS"),
    @Property(name = "openidm.router.prefix", value = AuditService.ROUTER_PREFIX)
})
public class AuditServiceImpl implements AuditService {
    final static Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);
    
    // Keys in the JSON configuration
    public final static String CONFIG_LOG_TO = "logTo";
    public final static String CONFIG_LOG_TYPE = "logType";
    public final static String CONFIG_LOG_TYPE_CSV = "csv";
    public final static String CONFIG_LOG_TYPE_REPO = "repository";
    
    EnhancedConfig enhancedConfig = new JSONEnhancedConfig();

    Map<String, List<String>> filters;
    
    List<AuditLogger> auditLoggers;
    
    /**
     * Gets an object from the audit logs by identifier. The returned object is not validated 
     * against the current schema and may need processing to conform to an updated schema.
     * <p>
     * The object will contain metadata properties, including object identifier {@code _id},
     * and object version {@code _rev} to enable optimistic concurrency 
     *
     * @param id the identifier of the object to retrieve from the object set.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws BadRequestException if the passed identifier is invalid
     * @return the requested object.
     */
    @Override
    public Map<String, Object> read(String fullId) throws ObjectSetException {
        // TODO
        return new HashMap();
    }

    /**
     * Creates a new object in the object set.
     * <p>
     * This method sets the {@code _id} property to the assigned identifier for the object,
     * and the {@code _rev} property to the revised object version (For optimistic concurrency)
     *
     * @param id the client-generated identifier to use, or {@code null} if server-generated identifier is requested.
     * @param object the contents of the object to create in the object set.
     * @throws NotFoundException if the specified id could not be resolved. 
     * @throws ForbiddenException if access to the object or object set is forbidden.
     * @throws PreconditionFailedException if an object with the same ID already exists.
     */
    @Override
    public void create(String fullId, Map<String, Object> obj) throws ObjectSetException {
        logger.debug("Audit create called for {} with {}", fullId, obj);        
        // Work-around until router id strategy in sync
        if (fullId.startsWith(ROUTER_PREFIX)) {
            String[] withoutAuditLevel = splitFirstLevel(fullId);
            fullId = withoutAuditLevel[1];
        }

        String[] splitTypeAndId =  splitFirstLevel(fullId);
        String type = splitTypeAndId[0];
        String localId = splitTypeAndId[1];

        // Filter
        List<String> actionFilter = filters.get(type);
        if (actionFilter != null) {
            // TODO: make filters that can operate on a variety of conditions
            if (!actionFilter.contains(obj.get("action"))) {
                return;
            }
        }
        
        // Generate an ID if there is none
        if (localId == null) {
            localId = UUID.randomUUID().toString();
            obj.put(ObjectSet.ID, localId);
            logger.debug("Assigned id {}", localId);
        }
        String id = type + "/" + localId;
        
        logger.debug("Create audit entry for {} with {}", id, obj);
        for (AuditLogger auditLogger : auditLoggers) {
            try {
                auditLogger.create(id, obj);
            } catch (ObjectSetException ex) {
                logger.warn("Failure writing audit log: {} with logger {}", new String[] {id, auditLogger.toString(), ex.getMessage()});
                throw ex;
            } catch (RuntimeException ex) {
                logger.warn("Failure writing audit log: {} with logger {}", new String[] {id, auditLogger.toString(), ex.getMessage()});
                throw ex;
            } 
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
     * 
     * Deletes the specified object from the object set.
     *
     * @param id the identifier of the object to be deleted.
     * @param rev the version of the object to delete or {@code null} if not provided.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws ConflictException if version is required but is {@code null}.
     * @throws PreconditionFailedException if version did not match the existing object in the set.
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
     * Performs the query on the specified object and returns the associated results.
     * <p>
     * Queries are parametric; a set of named parameters is provided as the query criteria.
     * The query result is a JSON object structure composed of basic Java types. 
     * 
     * The returned map is structured as follow: 
     * - The top level map contains meta-data about the query, plus an entry with the actual result records.
     * - The <code>QueryConstants</code> defines the map keys, including the result records (QUERY_RESULT)
     *
     * @param id identifies the object to query.
     * @param params the parameters of the query to perform.
     * @return the query results, which includes meta-data and the result records in JSON object structure format.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws BadRequestException if the specified params contain invalid arguments, e.g. a query id that is not
     * configured, a query expression that is invalid, or missing query substitution tokens.
     * @throws ForbiddenException if access to the object or specified query is forbidden.
     */
    @Override
    public Map<String, Object> query(String fullId, Map<String, Object> params) throws ObjectSetException {
        // TODO
        return new HashMap();
    }

    /**
     * Audit service does not support actions on audit entries.
     */
    @Override
    public Map<String, Object> action(String fullId, Map<String, Object> params) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on audit service");
    }

    // TODO: replace with common utility to handle ID, this is temporary
    // Assumes single level type
    static String[] splitFirstLevel(String id) {
        String firstLevel = id;
        String rest = null;
        int firstSlashPos = id.indexOf("/");
        if (firstSlashPos > -1) {
            firstLevel = id.substring(0, firstSlashPos);
            rest = id.substring(firstSlashPos + 1);
        }
        logger.trace("Extracted first level: {} rest: {}", firstLevel, rest);
        return new String[] { firstLevel, rest };
    }
    
    @Activate
    void activate(ComponentContext compContext) { 
        logger.debug("Activating Service with configuration {}", compContext.getProperties());
        JsonNode config = null;
        try {
            config = enhancedConfig.getConfigurationAsJson(compContext);
            auditLoggers = getAuditLoggers(config, compContext);
            filters = getFilters(config);
            logger.debug("Audit service filters enabled: {}", filters);
        } catch (RuntimeException ex) {
            logger.warn("Configuration invalid, can not start Audit service.", ex);
            throw ex;
        }
        logger.info("Audit service started.");
    }

    Map<String, List<String>> getFilters(JsonNode config) {
        Map<String, List<String>> configFilters = new HashMap<String, List<String>>();
        
        Map<String, Object> eventTypes = config.get("eventTypes").asMap();
        if (eventTypes == null) {
            return configFilters;
        }
        for (Map.Entry<String, Object> eventType : eventTypes.entrySet()) {
            String eventTypeName = eventType.getKey();
            JsonNode eventTypeNode = new JsonNode(eventType.getValue());
            JsonNode filterActions = eventTypeNode.get("filter").get("actions");
            // TODO: proper filter mechanism
            if (!filterActions.isNull()) {
                List<String> filter = new ArrayList<String>();
                for (JsonNode action : filterActions) {
                    Enum actionEnum = action.asEnum(Action.class);
                    filter.add(actionEnum.toString());
                }
                configFilters.put(eventTypeName, filter);
            }
        }
        return configFilters;
    }
    
    List getAuditLoggers(JsonNode config, ComponentContext compContext) {
        List configuredLoggers = new ArrayList();
        List logTo = config.get(CONFIG_LOG_TO).asList();
        for (Map entry : (List<Map>)logTo) {
            String logType = (String) entry.get(CONFIG_LOG_TYPE);
            // TDDO: make pluggable
            AuditLogger auditLogger = null;
            if (logType != null && logType.equalsIgnoreCase(CONFIG_LOG_TYPE_CSV)) {
                auditLogger = new CSVAuditLogger();
            } else if (logType != null && logType.equalsIgnoreCase(CONFIG_LOG_TYPE_REPO)) {
                auditLogger = new RepoAuditLogger();
            } else {
                throw new InvalidException("Configured audit logType is unknown: " + logType);
            }
            if (auditLogger != null) {
                auditLogger.setConfig(entry, compContext.getBundleContext());
                logger.info("Audit configured to log to {}", logType);
                configuredLoggers.add(auditLogger);
            }
        }
        return configuredLoggers;
    }
    
    
    /* Currently rely on deactivate/activate to be called by DS if config changes instead
    @Modified
    void modified(ComponentContext compContext) {
    }
    */

    
    @Deactivate
    void deactivate(ComponentContext compContext) { 
        logger.debug("Deactivating Service {}", compContext.getProperties());
        for (AuditLogger auditLogger : auditLoggers) {
            try {
                auditLogger.cleanup();
            } catch (Exception ex) {
                logger.info("AuditLogger cleanup reported failure", ex);
            }
        }
        logger.info("Audit service stopped.");
    }
}

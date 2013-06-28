/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All rights reserved.
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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.audit.AuditService;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.JsonResourceObjectSet;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.objset.ServiceUnavailableException;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audit logger that logs to a repository
 * @author aegloff
 */
public class RepoAuditLogger extends AbstractAuditLogger implements AuditLogger {
    final static Logger logger = LoggerFactory.getLogger(RepoAuditLogger.class);
    
    /**
     * Event names for monitoring audit behavior
     */
    public static final Name EVENT_AUDIT_CREATE = Name.get("openidm/internal/audit/repo/create");

    BundleContext ctx;

    JsonResourceObjectSet repo;
    
    String fullIdPrefix = AuditService.ROUTER_PREFIX + "/";
    
    public void setConfig(Map config, BundleContext ctx) throws InvalidException {
        super.setConfig(config, ctx);
        this.ctx = ctx;
    }
    
    public void cleanup() {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> read(String fullId) throws ObjectSetException {
        getRepoService();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("_queryId", "query-all");
        params.put("fields", "*");
        String[] split = AuditServiceImpl.splitFirstLevel(fullId);
        String type = split[0];
        String id = split[1];
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            List<Map<String, Object>> entries = new ArrayList<Map<String, Object>>();
            if (id == null) {
                Map<String, Object> queryResult = repo.query(fullIdPrefix + fullId, params);
                for (Map<String, Object> entry : (List<Map<String, Object>>) queryResult.get(QueryConstants.QUERY_RESULT)) {
                    entries.add(AuditServiceImpl.formatLogEntry(entry, type));
                }
                parseJsonValuesFromList(entries);
                result.put("entries", entries);
            } else {
                Map<String, Object> entry = repo.read(fullIdPrefix + fullId);
                parseJsonValuesFromEntry(entry);
                result = AuditServiceImpl.formatLogEntry(entry, type);
            }

        } catch (ObjectSetException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            repo = null; // For unexpected exceptions start fresh
            throw ex;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> query(String fullId, Map<String, Object> params) throws ObjectSetException {
        getRepoService();
        String queryId = (String)params.get("_queryId");
        boolean formatted = true;
        String[] split = AuditServiceImpl.splitFirstLevel(fullId);
        String type = split[0];
        try {
            if (params.get("formatted") != null && !AuditServiceImpl.getBoolValue(params.get("formatted"))) {
                formatted = false;
            }
            Map<String, Object> queryResults = repo.query(fullIdPrefix + fullId, params);
        
            if (type.equals(AuditServiceImpl.TYPE_RECON)) {
                return AuditServiceImpl.getReconResults((List<Map<String, Object>>)queryResults.get(QueryConstants.QUERY_RESULT), 
                        (String)params.get("reconId"), formatted);
            } else if (type.equals(AuditServiceImpl.TYPE_ACTIVITY)) {
                List<Map<String, Object>> entryList = (List<Map<String, Object>>)queryResults.get(QueryConstants.QUERY_RESULT);
                parseJsonValuesFromList(entryList);
                return AuditServiceImpl.getActivityResults(entryList, formatted);
            } else if (type.equals(AuditServiceImpl.TYPE_ACCESS)) {
                return AuditServiceImpl.getAccessResults((List<Map<String, Object>>)queryResults.get(QueryConstants.QUERY_RESULT), formatted);
            } else {
                throw new BadRequestException("Unsupported queryId " +  queryId + " on type " + type);
            }
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }
    
    public void parseJsonValuesFromList(List<Map<String, Object>> entryList) {
        for (Map<String, Object> entry : entryList) {
            parseJsonValuesFromEntry(entry);
        }
    }

    public void parseJsonValuesFromEntry(Map<String, Object> entry) {
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
    public void create(String fullId, Map<String, Object> obj) throws ObjectSetException {
        EventEntry measure = Publisher.start(EVENT_AUDIT_CREATE, obj, null);
        String[] split = AuditServiceImpl.splitFirstLevel(fullId);
        String type = split[0];
        try {
            AuditServiceImpl.preformatLogEntry(type, obj);
            createImpl(fullId, obj);
        } finally {
            measure.end();
        }
    }
    
    private void createImpl(String fullId, Map<String, Object> obj) throws ObjectSetException {
        JsonResourceObjectSet svc = getRepoService();
        try {
            svc.create(fullIdPrefix + fullId, obj);
        } catch (ObjectSetException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            repo = null; // For unexpected exceptions start fresh
            throw ex;
        }
    }

    private JsonResourceObjectSet getRepoService() throws ServiceUnavailableException, InternalServerErrorException {
        if (repo == null) {
            if (ctx != null) {
                try {
                    ServiceTracker<RepositoryService, RepositoryService> serviceTracker
                            = new ServiceTracker<RepositoryService, RepositoryService>(ctx, RepositoryService.class,
                            null);
                    serviceTracker.open();
                    int timeout = 10000;
                    logger.debug("Look for repository service for {} ms", Integer.valueOf(timeout));
                    RepositoryService repositoryService = serviceTracker.waitForService(timeout);
                    serviceTracker.close();
                    if (null == repositoryService) {
                        ServiceReference<RepositoryService> ref = ctx.getServiceReference(RepositoryService.class);
                        if (null != ref) {
                            repositoryService = ctx.getService(ref);
                        }
                    }
                    if (null != repositoryService) {
                        repo = new JsonResourceObjectSet(repositoryService);
                        logger.debug("Repository service found: {}", repo);
                    }
                } catch (Exception ex) {
                    throw new InternalServerErrorException("Repository audit logger failure to obtain the repo service."
                            + ex.getMessage(), ex);
                }
                if (null == repo){
                    throw new InternalServerErrorException("Repository audit logger failure to obtain the repo service.");
                }
            }
        }
        if (repo == null) {
            throw new ServiceUnavailableException("Repository audit logger could not find the repository service.");
        }
        return repo;
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
}

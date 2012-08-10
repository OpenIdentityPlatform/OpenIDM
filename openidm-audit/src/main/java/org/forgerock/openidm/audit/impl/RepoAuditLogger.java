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

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.forgerock.openidm.audit.AuditService;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.repo.RepositoryService;

// Deprecated
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ConflictException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.JsonResourceObjectSet;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.objset.ServiceUnavailableException;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;

/**
 * Audit logger that logs to a repository
 * @author aegloff
 */
public class RepoAuditLogger implements AuditLogger {
    final static Logger logger = LoggerFactory.getLogger(RepoAuditLogger.class);
    
    /**
     * Event names for monitoring audit behavior
     */
    public static final Name EVENT_AUDIT_CREATE = Name.get("openidm/internal/audit/repo/create");

    BundleContext ctx;

    JsonResourceObjectSet repo;
    
    String fullIdPrefix = AuditService.ROUTER_PREFIX + "/";
    
    public void setConfig(Map config, BundleContext ctx) throws InvalidException {
        this.ctx = ctx;
    }
    
    public void cleanup() {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> read(String fullId) throws ObjectSetException {
        JsonResourceObjectSet svc = getRepoService();
        Map<String, Object> result = null;
        try {
            result = svc.read(fullIdPrefix + fullId);
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
        JsonResourceObjectSet svc = getRepoService();
        Map<String, Object> result = null;
        try {
            result = svc.query(fullIdPrefix + fullId, params);
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
    public void create(String fullId, Map<String, Object> obj) throws ObjectSetException {
        EventEntry measure = Publisher.start(EVENT_AUDIT_CREATE, obj, null);
        try {
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
                    ServiceTracker serviceTracker = new ServiceTracker(ctx, RepositoryService.class.getName(), null);
                    serviceTracker.open();
                    int timeout = 10000;
                    logger.debug("Look for repository service for {} ms", Integer.valueOf(timeout));
                    repo = new JsonResourceObjectSet((RepositoryService)serviceTracker.waitForService(timeout));
                    logger.debug("Repository service found: {}", repo);
                    serviceTracker.close();
                } catch (Exception ex) {
                    throw new InternalServerErrorException("Repository audit logger failure to obtain the repo service." 
                            + ex.getMessage(), ex);
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

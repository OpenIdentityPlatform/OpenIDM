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

import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

import org.forgerock.openidm.audit.AuditService;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ConflictException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.MethodNotAllowedException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.objset.ServiceUnavailableException;
import org.forgerock.openidm.repo.RepositoryService;

/**
 * Audit logger that logs to a repository
 * @author aegloff
 */
public class RepoAuditLogger implements AuditLogger {
    final static Logger logger = LoggerFactory.getLogger(RepoAuditLogger.class);

    BundleContext ctx;
    RepositoryService repo;
    
    String fullIdPrefix = AuditService.ROUTER_PREFIX + "/";
    
    public void setConfig(Map config, BundleContext ctx) throws InvalidException {
        this.ctx = ctx;
    }
    
    public void cleanup() {
    }
    
    /**
     * {@inheritdoc}
     */
    public Map<String, Object> read(String fullId) throws ObjectSetException {
        RepositoryService svc = getRepoService();
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
     * {@inheritdoc}
     */
    public Map<String, Object> query(String fullId, Map<String, Object> params) throws ObjectSetException {
        RepositoryService svc = getRepoService();
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
     * {@inheritdoc}
     */
    public void create(String fullId, Map<String, Object> obj) throws ObjectSetException {
        RepositoryService svc = getRepoService();
        try {
            svc.create(fullIdPrefix + fullId, obj);
        } catch (ObjectSetException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            repo = null; // For unexpected exceptions start fresh
            throw ex;
        }
    }

    private RepositoryService getRepoService() throws ServiceUnavailableException, InternalServerErrorException {
        if (repo == null) {
            if (ctx != null) {
                try {
                    ServiceTracker serviceTracker = new ServiceTracker(ctx, RepositoryService.class.getName(), null);
                    serviceTracker.open();
                    int timeout = 10000;
                    logger.debug("Look for repository service for {} ms", Integer.valueOf(timeout));
                    repo = (RepositoryService) serviceTracker.waitForService(timeout);
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
    public void update(String fullId, String rev, Map<String, Object> obj) throws ObjectSetException {
        throw new MethodNotAllowedException("Not allowed on audit service");
    }

    /**
     * Audit service currently does not support deleting audit entries.
     */ 
    public void delete(String fullId, String rev) throws ObjectSetException {
        throw new MethodNotAllowedException("Not allowed on audit service");
    }

    /**
     * Audit service does not support changing audit entries.
     */
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        throw new MethodNotAllowedException("Not allowed on audit service");
    }
}

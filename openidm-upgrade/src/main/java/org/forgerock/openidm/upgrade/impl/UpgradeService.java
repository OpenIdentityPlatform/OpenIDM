/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.upgrade.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;

import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ConflictException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.JsonResourceObjectSet;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSetContext;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.ObjectSetJsonResource;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.objset.PreconditionFailedException;

import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basis and entry point to initiate the product
 * upgrade and patching mechanisms over REST
 * 
 * @author aegloff
 */
@Component(name = UpgradeService.PID, policy = ConfigurationPolicy.IGNORE,
description = "OpenIDM Product Upgrade Management Service", immediate = true)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Product Upgrade Management Service"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "product")
})
public class UpgradeService extends ObjectSetJsonResource {

    private final static Logger logger = LoggerFactory.getLogger(UpgradeService.class);
    
    public static final String PID = "org.forgerock.openidm.upgrade";

    /**
     * Service does not support reading entries yet.
     */
    @Override
    public Map<String, Object> read(String fullId) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on upgrade service");
    }

    /**
     * Service does not allow creating entries.
     */
    @Override
    public void create(String fullId, Map<String, Object> obj) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on upgrade service");
    }

    /**
     * Service does not support changing entries.
     */
    @Override
    public void update(String fullId, String rev, Map<String, Object> obj) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on audit service");
    }

    /**
     * Service does not support deleting entries..
     */
    @Override
    public void delete(String fullId, String rev) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on audit service");
    }

    /**
     * Service does not support changing entries.
     */
    @Override
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on upgrade service");
    }

    /**
     * Service does not support querying entries yet.
     */
    @Override
    public Map<String, Object> query(String fullId, Map<String, Object> params) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on upgrade service");
    }

    /**
     * Upgrade action support
     */
    @Override
    public Map<String, Object> action(String fullId, Map<String, Object> params) throws ObjectSetException {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        
        JsonValue paramsVal = new JsonValue(params);
        String action = paramsVal.get("_action").asString();
        if (action == null) {
            throw new BadRequestException("_action parameter is not present or value is null");
        } else if (action.equals("upgrade")) {
            try {
                new UpgradeManager().execute(params.get("url").toString(), IdentityServer.getFileForWorkingPath(""),
                        IdentityServer.getFileForInstallPath(""), params);
            } catch (InvalidArgsException ex) {
                throw new BadRequestException(ex.getMessage(), ex);
            } catch (UpgradeException ex) {
                throw new InternalServerErrorException(ex.getMessage(), ex);
            }
        }
        return result;
    }

    @Activate
    void activate(ComponentContext compContext) throws Exception {
        logger.debug("Activating Upgrade service {}", compContext.getProperties());
        logger.info("Upgrade service started.");
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Service {}", compContext.getProperties());
        logger.info("Upgrade service stopped.");
    }
}

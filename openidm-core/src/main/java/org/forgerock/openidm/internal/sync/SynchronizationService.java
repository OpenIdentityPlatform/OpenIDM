/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.internal.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.script.ScriptRegistry;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;


/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 * @author aegloff
 */

//@Component(name = SynchronizationService.PID, immediate = true, policy = ConfigurationPolicy.REQUIRE)
@Service
@Properties({ @Property(name = Constants.SERVICE_DESCRIPTION, value = "Synchronization Service"),
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/sync") })
public class SynchronizationService implements SingletonResourceProvider {

    /** TODO: Description. */
    private enum Action {
        onCreate, onUpdate, onDelete, performAction/* , recon */
    }

    public static final String PID = "org.forgerock.openidm.sync";

    /**
     * Setup logging for the {@link SynchronizationService}.
     */
    private final static Logger logger = LoggerFactory.getLogger(SynchronizationService.class);

    /**
     * Object mappings. Order of mappings evaluated during synchronization is
     * significant.
     */
    private final ArrayList<ObjectMapping> mappings = new ArrayList<ObjectMapping>();

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private ScriptRegistry scriptRegistry;

    protected void bindScriptRegistry(final ScriptRegistry service) {
        scriptRegistry = service;
    }

    protected void unbindScriptRegistry(final ScriptRegistry service) {
        scriptRegistry = null;
    }

    @Activate
    protected void activate(ComponentContext context) {
        JsonValue config = JSONEnhancedConfig.newInstance().getConfigurationAsJson(context);
        try {
            for (JsonValue jv : config.get("mappings").expect(List.class)) {
                mappings.add(new ObjectMapping(scriptRegistry, jv)); // throws
                                                           // JsonValueException
            }
            for (ObjectMapping mapping : mappings) {
                mapping.initRelationships(mappings);
            }
        } catch (JsonValueException jve) {
            throw new ComponentException("Configuration error: " + jve.getMessage(), jve);
        } catch (ScriptException e) {
            throw new ComponentException("Configuration error: " + e.getMessage(), e);
        }
        logger.info("Synchronization service is activated.");
    }

    @Modified
    void modified(ComponentContext context) {
        logger.info("Synchronization service is modified.");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        mappings.clear();
        logger.info("Synchronization service is deactivated.");
    }

    // ----- Implementation of CollectionResourceProvider interface

    @Override
    public void readInstance(final ServerContext context, final ReadRequest request,
            final ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Read are not supported for resource instances");
        handler.handleError(e);
    }

    @Override
    public void patchInstance(final ServerContext context, final PatchRequest request,
            final ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void updateInstance(final ServerContext context, final UpdateRequest request,
            final ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Update operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void actionInstance(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        try {
            JsonValue _params = new JsonValue(request.getAdditionalActionParameters());
            switch (findAction(request.getAction())) {
            case onCreate: {
                String id = _params.get("id").required().asString();
                logger.debug("Synchronization _action=onCreate, id={}", id);
                onCreate(id, request.getContent().expect(Map.class));
                break;
            }
            case onUpdate: {
                String id = _params.get("id").required().asString();
                logger.debug("Synchronization _action=onUpdate, id={}", id);
                onUpdate(id, null, _params.get("_entity").expect(Map.class));
                break;
            }
            case onDelete: {
                String id = _params.get("id").required().asString();
                logger.debug("Synchronization _action=onDelete, id={}", id);
                onDelete(id, null);
                break;
            }
            // case recon: {
            // JsonValue result = new JsonValue( new HashMap<String, Object>());
            // JsonValue mapping = _params.get("mapping").required();
            // logger.debug("Synchronization _action=recon, mapping={}",
            // mapping);
            // String reconId = reconService.reconcile(mapping, Boolean.TRUE);
            // result.put("reconId", reconId);
            // result.put("_id", reconId);
            // result.put("comment1",
            // "Deprecated API on sync service. Call recon action on recon service instead.");
            // result.put("comment2",
            // "Deprecated return property reconId, use _id instead.");
            // break; }
//            case performAction: {
//                logger.debug("Synchronization _action=performAction, params={}", _params);
//                ObjectMapping mapping = getMapping(_params.get("mapping").required().asString());
//                mapping.performAction(_params);
//                break;
//            }
            }
            handler.handleResult(new JsonValue(null));
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (JsonValueException e) {
            handler.handleError(new BadRequestException(e.getMessage(), e));
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    private Action findAction(String actionId) throws ResourceException {
        for (Action action : Action.values()) {
            action.name().compareToIgnoreCase(actionId);
            return action;
        }
        throw new NotSupportedException("Unrecognized action ID '" + actionId
                + "'. Supported action IDs: clear");
    }

    /**
     * TODO: Description.
     *
     * @param name
     *            TODO.
     * @return TODO.
     * @throws ResourceException
     */
    public ObjectMapping getMapping(String name) throws ResourceException {
        for (ObjectMapping mapping : mappings) {
            if (mapping.getName().equals(name)) {
                return mapping;
            }
        }
        throw new BadRequestException("No such mapping: " + name);
    }

    public void onCreate(String id, JsonValue object) throws ResourceException {
        for (ObjectMapping mapping : mappings) {
            if (mapping.isSyncEnabled()) {
                //mapping.onCreateSync()
            }
        }
    }

    public void onUpdate(String id, JsonValue oldValue, JsonValue newValue)
            throws ResourceException {
        for (ObjectMapping mapping : mappings) {
            if (mapping.isSyncEnabled()) {
               // mapping.onUpdateSync()
            }
        }
    }

    public void onDelete(String id, JsonValue oldValue) throws ResourceException {
        for (ObjectMapping mapping : mappings) {
            if (mapping.isSyncEnabled()) {
                //mapping.onDeleteSync()
            }
        }
    }

}

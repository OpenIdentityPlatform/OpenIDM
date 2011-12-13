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
 * $Id$
 */
package org.forgerock.openidm.provisioner.impl;

import org.apache.felix.scr.annotations.*;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.provisioner.ConfigurationService;
import org.forgerock.openidm.provisioner.Id;
import org.forgerock.openidm.provisioner.ProvisionerService;
import org.forgerock.openidm.provisioner.SystemIdentifier;
import org.forgerock.openidm.scheduler.ExecutionException;
import org.forgerock.openidm.scheduler.ScheduledService;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.openidm.sync.SynchronizationListener;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

// JSON Resource


/**
 * SystemObjectSetService implement the {@link JsonResource}.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
@Component(name = "org.forgerock.openidm.provisioner", policy = ConfigurationPolicy.IGNORE, description = "OpenIDM System Object Set Service")
@Service(value = {JsonResource.class, ScheduledService.class})
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM System Object Set Service"),
        @Property(name = ServerConstants.ROUTER_PREFIX, value = "system")
})
public class SystemObjectSetService implements JsonResource,
// TODO: Deprecate the following interfaces when the discovery-engine:
        SynchronizationListener, ScheduledService {
    private final static Logger TRACE = LoggerFactory.getLogger(SystemObjectSetService.class);

    @Reference(referenceInterface = ProvisionerService.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
            bind = "bind",
            unbind = "unbind",
            policy = ReferencePolicy.DYNAMIC,
            strategy = ReferenceStrategy.EVENT)
    private Map<SystemIdentifier, ProvisionerService> provisionerServices = new HashMap<SystemIdentifier, ProvisionerService>();

    @Reference(referenceInterface = JsonResource.class,
            cardinality = ReferenceCardinality.MANDATORY_UNARY,
            policy = ReferencePolicy.STATIC,
            target = "(service.pid=org.forgerock.openidm.router)")
    private JsonResource router;


    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    private ConfigurationService configurationService;

    protected void bind(ProvisionerService service, Map properties) {
        provisionerServices.put(service.getSystemIdentifier(), service);
        TRACE.info("ProvisionerService {} is bound with system identifier {}.",
                properties.get(ComponentConstants.COMPONENT_ID),
                service.getSystemIdentifier());
    }

    protected void unbind(ProvisionerService service, Map properties) {
        for (Map.Entry<SystemIdentifier, ProvisionerService> entry : provisionerServices.entrySet()) {
            if (service.equals(entry.getValue())) {
                provisionerServices.remove(entry.getKey());
                break;
            }
        }
        TRACE.info("ProvisionerService {} is unbound.", properties.get(ComponentConstants.COMPONENT_ID));
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue handle(JsonValue request) throws JsonResourceException {
        JsonValue params = request.get("params");
        if ("action".equalsIgnoreCase(request.get("method").asString()) && !params.isNull() &&
                "CREATECONFIGURATION".equalsIgnoreCase(params.get(ServerConstants.ACTION_NAME).asString())) {
            return configurationService.configure(request.get("value"));
        } else {
            return locateService(request).handle(request);
        }
    }

    /**
     * Called when a source object has been created.
     *
     * @param id    the fully-qualified identifier of the object that was created.
     * @param value the value of the object that was created.
     * @throws org.forgerock.openidm.sync.SynchronizationException
     *          if an exception occurs processing the notification.
     */
    public void onCreate(String id, JsonValue value) throws SynchronizationException {
        try {
            Map<String, Object> params = new HashMap<String, Object>(2);
            params.put("_action", "ONCREATE");
            params.put("id", id);
            JsonValue request = new JsonValue(new HashMap<String, Object>(5));
            request.put("method", "action");
            request.put("type", "resource");
            request.put("id", "sync");
            request.put("params", params);
            request.put("value", value.getObject());
            router.handle(request);
        } catch (JsonResourceException e) {
            throw new SynchronizationException(e);
        }
    }

    /**
     * Called when a source object has been updated.
     *
     * @param id       the fully-qualified identifier of the object that was updated.
     * @param oldValue the old value of the object prior to the update.
     * @param newValue the new value of the object after the update.
     * @throws org.forgerock.openidm.sync.SynchronizationException
     *          if an exception occurs processing the notification.
     */
    public void onUpdate(String id, JsonValue oldValue, JsonValue newValue) throws SynchronizationException {
        try {
            Map<String, Object> params = new HashMap<String, Object>(2);
            params.put("_action", "ONUPDATE");
            params.put("id", id);
            JsonValue request = new JsonValue(new HashMap<String, Object>(5));
            request.put("method", "action");
            request.put("type", "resource");
            request.put("id", "sync");
            request.put("params", params);
            request.put("value", newValue.getObject());
            router.handle(request);
        } catch (JsonResourceException e) {
            throw new SynchronizationException(e);
        }
    }

    /**
     * Called when a source object has been deleted.
     *
     * @param id the fully-qualified identifier of the object that was deleted.
     * @throws org.forgerock.openidm.sync.SynchronizationException
     *          if an exception occurs processing the notification.
     */
    public void onDelete(String id) throws SynchronizationException {
        try {
            Map<String, Object> params = new HashMap<String, Object>(2);
            params.put("_action", "ONDELETE");
            params.put("id", id);
            JsonValue request = new JsonValue(new HashMap<String, Object>(4));
            request.put("method", "action");
            request.put("type", "resource");
            request.put("id", "sync");
            request.put("params", params);
            router.handle(request);
        } catch (JsonResourceException e) {
            throw new SynchronizationException(e);
        }
    }

    /**
     * Invoked by the scheduler when the scheduler triggers.
     *
     * @param schedulerContext Context information passed by the scheduler service
     * @throws org.forgerock.openidm.scheduler.ExecutionException
     *          if execution of the scheduled work failed.
     *          Implementations can also throw RuntimeExceptions which will get logged.
     */
    public void execute(Map<String, Object> schedulerContext) throws ExecutionException {
        try {
            JsonValue params = new JsonValue(schedulerContext).get(CONFIGURED_INVOKE_CONTEXT);
            String action = params.get("action").asString();
            if ("liveSync".equals(action) || "activeSync".equals(action)) {
                Id id = new Id(params.get("source").asString());
                String previousStageId = "repo/synchronisation/pooledSyncStage/" + id.toString().replace("/", "").toUpperCase();
                try {
                    JsonValue previousStage = null;
                    try {
                        JsonValue readRequest = new JsonValue(new HashMap());
                        readRequest.put("type", "resource");
                        readRequest.put("method", "read");
                        readRequest.put("id", previousStageId);
                        previousStage = router.handle(readRequest);

                        JsonValue updateRequest = new JsonValue(new HashMap());
                        updateRequest.put("type", "resource");
                        updateRequest.put("method", "update");
                        updateRequest.put("id", previousStageId);
                        updateRequest.put("rev", previousStage.get("_rev"));
                        updateRequest.put("value", locateService(id).liveSynchronize(id.getObjectType(), previousStage != null ? previousStage : null, this).asMap());
                        router.handle(updateRequest);
                    } catch (JsonResourceException e) {
                        if (null == previousStage) {
                            TRACE.info("PooledSyncStage object {} is not found. First execution.");
                            JsonValue createRequest = new JsonValue(new HashMap());
                            createRequest.put("type", "resource");
                            createRequest.put("method", "create");
                            createRequest.put("id", previousStageId);
                            createRequest.put("value", locateService(id).liveSynchronize(id.getObjectType(), null, this).asMap());
                            router.handle(createRequest);
                        }
                    }
                } catch (JsonResourceException e) {
                    throw new ExecutionException(e);
                }
            }
        } catch (JsonValueException jve) {
            throw new ExecutionException(jve);
        } catch (JsonResourceException e) {
            throw new ExecutionException(e);
        }
    }

    private ProvisionerService locateService(JsonValue request) throws JsonResourceException {
        Id identifier = new Id(request.get("id").required().asString());
        return locateService(identifier);
    }

    private ProvisionerService locateService(Id identifier) throws JsonResourceException {
        for (Map.Entry<SystemIdentifier, ProvisionerService> entry : provisionerServices.entrySet()) {
            if (entry.getKey().is(identifier)) {
                return entry.getValue();
            }
        }
        throw new JsonResourceException(404, "System: " + identifier + " is not available.");
    }
}

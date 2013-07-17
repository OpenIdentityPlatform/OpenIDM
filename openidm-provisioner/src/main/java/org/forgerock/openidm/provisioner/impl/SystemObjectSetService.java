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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.provisioner.ConfigurationService;
import org.forgerock.openidm.provisioner.Id;
import org.forgerock.openidm.provisioner.ProvisionerService;
import org.forgerock.openidm.provisioner.SystemIdentifier;
import org.forgerock.openidm.quartz.impl.ExecutionException;
import org.forgerock.openidm.quartz.impl.ScheduledService;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.openidm.sync.SynchronizationListener;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// JSON Resource


/**
 * SystemObjectSetService implement the {@link JsonResource}.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
@Component(name = "org.forgerock.openidm.provisioner", immediate = true, policy = ConfigurationPolicy.IGNORE, description = "OpenIDM System Object Set Service")
@Service(value = {JsonResource.class, ScheduledService.class})
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM System Object Set Service"),
        @Property(name = ServerConstants.ROUTER_PREFIX, value = SystemObjectSetService.ROUTER_PREFIX)
})
public class SystemObjectSetService implements JsonResource,
// TODO: Deprecate the following interfaces when the discovery-engine:
        SynchronizationListener, ScheduledService {
    private final static Logger TRACE = LoggerFactory.getLogger(SystemObjectSetService.class);
    
    public final static String ROUTER_PREFIX = "system";

    public final static String ACTION_CREATE_CONFIGURATION = "CREATECONFIGURATION";
    public final static String ACTION_TEST_CONFIGURATION = "testConfig";
    public final static String ACTION_TEST_CONNECTOR = "test";
    public final static String ACTION_LIVE_SYNC = "liveSync";
    public final static String ACTION_ACTIVE_SYNC = "activeSync";

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
        if ("action".equalsIgnoreCase(request.get("method").asString()) && !params.isNull()) {
            String action = params.get(ServerConstants.ACTION_NAME).asString();
            if (ACTION_CREATE_CONFIGURATION.equalsIgnoreCase(action)) {
                return configurationService.configure(request.get("value"));
            } else if (ACTION_TEST_CONFIGURATION.equalsIgnoreCase(action)) {
                JsonValue config = request.get("value");
                if (!request.get("id").isNull()) {
                    throw new JsonResourceException(JsonResourceException.BAD_REQUEST,  
                            "A system ID must not be specified in the request");
                }
                ProvisionerService ps = locateServiceForTest(config.get("name"));
                if (ps != null) {
                    return new JsonValue(ps.testConfig(request.get("value")));
                } else {
                    throw new JsonResourceException(JsonResourceException.BAD_REQUEST, 
                            "Invalid configuration to test: no 'name' specified");
                }
                
            } else if (ACTION_TEST_CONNECTOR.equalsIgnoreCase(action)) {
                ProvisionerService ps = locateServiceForTest(request.get("id"));
                if (ps != null) {
                    return new JsonValue(ps.getStatus());
                } else {
                    List<Object> list = new ArrayList<Object>();
                    for (Map.Entry<SystemIdentifier, ProvisionerService> entry : provisionerServices.entrySet()) {
                        list.add(entry.getValue().getStatus());
                    }
                    return new JsonValue(list);
                }
            } else if (isLiveSyncAction(action)) {
                // Expose liveSync as callable in two ways
                // Directly on system, taking a source param; matches the scheduler contract
                // On the resource directly, e.g. system/ldap/account; RESTful contract
                String source = params.get("source").asString();
                boolean detailedFailure = booleanValue(params.get("detailedFailure"));
                if (source == null) {
                    String id = request.get("id").asString();
                    if (id == null) {
                        throw new JsonResourceException(JsonResourceException.BAD_REQUEST, 
                                "liveSync action requires either an explicit source parameter, "
                                + "or needs to be called on a specific provisioner URI");
                    }
                    source = ROUTER_PREFIX + "/" + id;
                    TRACE.debug("liveSync called without explicit source parameter, assume it is targeted at the request URI {}", source);
                } else {
                    TRACE.debug("liveSync called with explicit source parameter {}", source);
                }
                return liveSync(source, detailedFailure);
            } else {
                return locateService(request).handle(request);
            }
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
     * @param the value before the delete, or null if not supplied 
     * @throws org.forgerock.openidm.sync.SynchronizationException
     *          if an exception occurs processing the notification.
     */
    public void onDelete(String id, JsonValue oldValue) throws SynchronizationException {
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
     * @throws org.forgerock.openidm.quartz.impl.ExecutionException
     *          if execution of the scheduled work failed.
     *          Implementations can also throw RuntimeExceptions which will get logged.
     */
    public void execute(Map<String, Object> schedulerContext) throws ExecutionException {
        try {
            JsonValue params = new JsonValue(schedulerContext).get(CONFIGURED_INVOKE_CONTEXT);
            String action = params.get("action").asString();
            if (isLiveSyncAction(action)) {
                String source = params.get("source").required().asString();
                liveSync(source, true);
            }
        } catch (JsonValueException jve) {
            throw new ExecutionException(jve);
        } catch (JsonResourceException e) {
            throw new ExecutionException(e);
        } catch (RuntimeException e) {
            throw new ExecutionException(e);
        }
    }
    
    /**
     * @param action the requested action
     * @return true if the action string is to live sync
     */
    private boolean isLiveSyncAction(String action) {
        return (ACTION_LIVE_SYNC.equalsIgnoreCase(action) || ACTION_ACTIVE_SYNC.equalsIgnoreCase(action));
    }

    /**
     * Live sync the specified provisioner resource
     * @param source the URI of the provisioner instance to live sync
     * @param detailedFailure whether in the case of failures additional details such as the 
     * record content of where it failed should be included in the response
     */
    private JsonValue liveSync(String source, boolean detailedFailure) throws JsonResourceException {
        JsonValue response = null;
        Id id = new Id(source);
        String previousStageId = "repo/synchronisation/pooledSyncStage/" + id.toString().replace("/", "").toUpperCase();

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
            response = locateService(id).liveSynchronize(id.getObjectType(), previousStage != null ? previousStage : null, this);
            updateRequest.put("value", response.asMap());
            router.handle(updateRequest);
        } catch (JsonResourceException e) {
            if (null == previousStage) {
                TRACE.info("PooledSyncStage object {} is not found. First execution.");
                JsonValue createRequest = new JsonValue(new HashMap());
                createRequest.put("type", "resource");
                createRequest.put("method", "create");
                createRequest.put("id", previousStageId);
                response = locateService(id).liveSynchronize(id.getObjectType(), null, this);
                createRequest.put("value", response.asMap());
                router.handle(createRequest);
            } else {
                throw e;
            }
        }
        if (response != null && !detailedFailure) {
            // The detailedFailure option handling ideally should move into provisioners
            response.get("lastException").remove("syncDelta");
        }
        return response;
    }

    /** 
     * @param value to convert to boolean
     * Allows boolean values both as Boolean or in String-ified form. 
     * Non-boolean or null argument returns false.
     */
    private boolean booleanValue(JsonValue value) {
        if (value.isBoolean()) {
            return value.asBoolean().booleanValue();
        } else {
            return Boolean.parseBoolean(value.asString());
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
    
    private ProvisionerService locateServiceForTest(JsonValue requestId) throws JsonResourceException {
        ProvisionerService ps = null;
        if (!requestId.isNull()) {
            Id id = new Id(requestId.asString() + "/test");
            for (Map.Entry<SystemIdentifier, ProvisionerService> entry : provisionerServices.entrySet()) {
                if (entry.getKey().is(id)) {
                    ps = entry.getValue();
                }
            }
            if (ps == null) {
                throw new JsonResourceException(404, "System: " + requestId.asString() + " is not available.");
            }
            return ps;
        }
        return null;
    }
}

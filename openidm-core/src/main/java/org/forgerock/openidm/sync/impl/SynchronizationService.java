/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.sync.impl;

// Java SE
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// OSGi
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;

// Felix SCR
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;

// JSON Fluent
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.fluent.JsonPointer;

// JSON Resource
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceContext;

// OpenIDM
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.quartz.impl.ExecutionException;
import org.forgerock.openidm.quartz.impl.ScheduledService;
import org.forgerock.openidm.scope.ScopeFactory;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.openidm.sync.SynchronizationListener;

// Deprecated
import org.forgerock.openidm.objset.ConflictException;
import org.forgerock.openidm.objset.JsonResourceObjectSet;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetContext;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.ObjectSetJsonResource;

/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 * @author aegloff
 */
@Component(
    name = "org.forgerock.openidm.sync",
    policy = ConfigurationPolicy.REQUIRE,
    immediate = true
)
@Properties({
    @Property(name = "service.description", value = "OpenIDM object synchronization service"),
    @Property(name = "service.vendor", value = "ForgeRock AS"),
    @Property(name = "openidm.router.prefix", value = "sync")
})
@Service
public class SynchronizationService extends ObjectSetJsonResource
// TODO: Deprecate these interfaces:
 implements SynchronizationListener, ScheduledService, Mappings {

    /** TODO: Description. */
    private enum Action {
        onCreate, onUpdate, onDelete, recon, performAction
    }

    /** TODO: Description. */
    private final static Logger logger = LoggerFactory.getLogger(SynchronizationService.class);

    /** Object mappings. Order of mappings evaluated during synchronization is significant. */
    private final ArrayList<ObjectMapping> mappings = new ArrayList<ObjectMapping>();

    /** TODO: Description. */
    private ComponentContext context;

    @Reference
    Reconcile reconService;
    
    /** Object set router service. */
    @Reference(
        name = "ref_SynchronizationService_JsonResourceRouterService",
        referenceInterface = JsonResource.class,
        bind = "bindRouter",
        unbind = "unbindRouter",
        cardinality = ReferenceCardinality.MANDATORY_UNARY,
        policy = ReferencePolicy.STATIC,
        target = "(service.pid=org.forgerock.openidm.router)"
    )
    private ObjectSet router;
    protected void bindRouter(JsonResource router) {
        this.router = new JsonResourceObjectSet(router);
    }
    protected void unbindRouter(JsonResource router) {
        this.router = null;
    }

    /** Scope factory service. */
    @Reference(
        name = "ref_SynchronizationService_ScopeFactory",
        referenceInterface = ScopeFactory.class,
        bind = "bindScopeFactory",
        unbind = "unbindScopeFactory",
        cardinality = ReferenceCardinality.MANDATORY_UNARY,
        policy = ReferencePolicy.STATIC
    )
    private ScopeFactory scopeFactory;
    protected void bindScopeFactory(ScopeFactory scopeFactory) {
        this.scopeFactory = scopeFactory;
    }
    protected void unbindScopeFactory(ScopeFactory scopeFactory) {
        this.scopeFactory = null;
    }

    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;
        JsonValue config = new JsonValue(new JSONEnhancedConfig().getConfiguration(context));
        try {
            for (JsonValue jv : config.get("mappings").expect(List.class)) {
                mappings.add(new ObjectMapping(this, jv)); // throws JsonValueException
            }
            for (ObjectMapping mapping : mappings) {
                mapping.initRelationships(this, mappings);
            }
        } catch (JsonValueException jve) {
            throw new ComponentException("Configuration error: " + jve.getMessage(), jve);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        mappings.clear();
        this.context = null;
    }

    /**
     * TODO: Description.
     *
     * @param name TODO.
     * @return TODO.
     * @throws org.forgerock.openidm.sync.SynchronizationException
     */
    public ObjectMapping getMapping(String name) throws SynchronizationException {
        for (ObjectMapping mapping : mappings) {
            if (mapping.getName().equals(name)) {
                return mapping;
            }
        }
        throw new SynchronizationException("No such mapping: " + name);
    }
    
    /**
     * Instantiate an ObjectMapping with the given config
     */
    public ObjectMapping createMapping(JsonValue mappingConfig) {
        ObjectMapping createdMapping = new ObjectMapping(this, mappingConfig);
        List<ObjectMapping> augmentedMappings = new ArrayList<ObjectMapping>(mappings);
        augmentedMappings.add(createdMapping);
        createdMapping.initRelationships(this, augmentedMappings);
        return createdMapping;
    }

    /**
     * TODO: Description.
     *
     * @throws SynchronizationException TODO.
     * @return
     */
    ObjectSet getRouter() throws SynchronizationException {
        if (router == null) {
            throw new SynchronizationException("Not bound to internal router");
        }
        return router;
    }

    /**
     * TODO: Description.
     *
     * @return TODO.
     */
    Map<String, Object> newScope() {
        return scopeFactory.newInstance(ObjectSetContext.get());
    }

    /**
     * @deprecated Use {@code sync} resource interface.
     */
    @Override // SynchronizationListener
    @Deprecated // use resource interface
    public void onCreate(String id, JsonValue object) throws SynchronizationException {
        PendingLink.handlePendingLinks(mappings, id, object);
        for (ObjectMapping mapping : mappings) {
            mapping.onCreate(id, object);
        }
    }

    /**
     * @deprecated Use {@code sync} resource interface.
     */
    @Override // SynchronizationListener
    @Deprecated // use resource interface
    public void onUpdate(String id, JsonValue oldValue, JsonValue newValue) throws SynchronizationException {
        for (ObjectMapping mapping : mappings) {
            mapping.onUpdate(id, oldValue, newValue);
        }
    }

    /**
     * @deprecated Use {@code sync} resource interface.
     */
    @Override // SynchronizationListener
    @Deprecated // use resource interface
    public void onDelete(String id) throws SynchronizationException {
        for (ObjectMapping mapping : mappings) {
            mapping.onDelete(id);
        }
    }

    /**
     * @deprecated Use {@code sync} resource interface.
     */
    @Deprecated
    private JsonValue newFauxContext(JsonValue mapping) {
        JsonValue context = JsonResourceContext.newContext("resource", ObjectSetContext.get());
        context.put("method", "action");
        context.put("id", "sync");
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("mapping", mapping == null ? null : mapping.getObject());
        context.put("params", params);
        return context;
    }

    /**
     * @deprecated Use {@code sync} resource interface.
     */
    @Override // ScheduledService
    @Deprecated // use resource interface
    public void execute(Map<String, Object> context) throws ExecutionException {
        try {
            JsonValue params = new JsonValue(context).get(CONFIGURED_INVOKE_CONTEXT);
            String action = params.get("action").asString();
            if ("reconcile".equals(action)) { // "action": "reconcile"
                JsonValue mapping = params.get("mapping");
                ObjectSetContext.push(newFauxContext(mapping));
                try {
                    reconService.reconcile(mapping, Boolean.TRUE);
                } finally {
                    ObjectSetContext.pop();
                }
            } else {
                throw new ExecutionException("Unknown action '" + action + "' configured in schedule. "
                        + "valid action(s) are: 'reconcile'");
            }
        } catch (JsonValueException jve) {
            throw new ExecutionException(jve);
        } catch (SynchronizationException se) {
            throw new ExecutionException(se);
        }
    }

    /**
     *
     * @deprecated Use the discovery engine.
     */
    private void performAction(JsonValue params) throws SynchronizationException {
        ObjectMapping mapping = getMapping(params.get("mapping").required().asString());
        mapping.performAction(params);
    }

    @Override // ObjectSetJsonResource
    public Map<String, Object> action(String id, Map<String, Object> params) throws ObjectSetException {
        if (id != null) { // operation on entire set only... for now
            throw new NotFoundException();
        }
        Map<String, Object> result = null;
        JsonValue _params = new JsonValue(params, new JsonPointer("params"));
        Action action = _params.get("_action").required().asEnum(Action.class);
        try {
            switch (action) {
                case onCreate:
                    id = _params.get("id").required().asString();
                    logger.debug("Synchronization _action=onCreate, id={}", id);
                    onCreate(id, _params.get("_entity").expect(Map.class));
                    break;
                case onUpdate:
                    id = _params.get("id").required().asString();
                    logger.debug("Synchronization _action=onUpdate, id={}", id);
                    onUpdate(id, null, _params.get("_entity").expect(Map.class));
                    break;
                case onDelete:
                    id = _params.get("id").required().asString();
                    logger.debug("Synchronization _action=onUpdate, id={}", id);
                    onDelete(id);
                    break;
                case recon:
                    result = new HashMap<String, Object>();
                    JsonValue mapping = _params.get("mapping").required();
                    logger.debug("Synchronization _action=recon, mapping={}", mapping);
                    String reconId = reconService.reconcile(mapping, Boolean.TRUE);
                    result.put("reconId", reconId);
                    result.put("_id", reconId);
                    result.put("comment1", "Deprecated API on sync service. Call recon action on recon service instead.");
                    result.put("comment2", "Deprecated return property reconId, use _id instead.");
                    break;
                case performAction:
                    logger.debug("Synchronization _action=performAction, params={}", _params);
                    performAction(_params);
                    result = new HashMap<String, Object>();
                    //result.put("status", performAction(_params));
                    break;
            }
        } catch (SynchronizationException se) {
            throw new ConflictException(se);
        }
        return result;
    }
}

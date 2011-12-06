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

// Java Standard Edition
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

// OSGi Framework
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;

// Apache Felix Maven SCR Plugin
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

// JSON Fluent library
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.fluent.JsonPointer;

// OpenIDM
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.context.InvokeContext;
import org.forgerock.openidm.objset.ConflictException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.scheduler.ExecutionException;
import org.forgerock.openidm.scheduler.ScheduledService;
import org.forgerock.openidm.scope.ScopeFactory;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.openidm.sync.SynchronizationListener;

/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
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
public class SynchronizationService implements ObjectSet, SynchronizationListener, ScheduledService {

    /** TODO: Description. */
    private enum Action { ONCREATE, ONUPDATE, ONDELETE, RECON }

    /** TODO: Description. */
    private final static Logger LOGGER = LoggerFactory.getLogger(SynchronizationService.class);

    /** Object mappings. Order of mappings evaluated during synchronization is significant. */
    private final ArrayList<ObjectMapping> mappings = new ArrayList<ObjectMapping>();

    /** TODO: Description. */
    private ComponentContext context;

    /** Object set router service. */
    @Reference(
        name = "ref_SynchronizationService_ObjectSetRouterService",
        referenceInterface = ObjectSet.class,
        bind = "bindRouter",
        unbind = "unbindRouter",
        cardinality = ReferenceCardinality.MANDATORY_UNARY,
        policy = ReferencePolicy.STATIC,
        target = "(service.pid=org.forgerock.openidm.router)"
    )
    private ObjectSet router;
    protected void bindRouter(ObjectSet router) {
        this.router = router;
    }
    protected void unbindRouter(ObjectSet router) {
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
            throw new ComponentException("Configuration error", jve);
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
    ObjectMapping getMapping(String name) throws SynchronizationException {
        for (ObjectMapping mapping : mappings) {
            if (mapping.getName().equals(name)) {
                return mapping;
            }
        }
        throw new SynchronizationException("No such mapping: " + name);
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
        return scopeFactory.newInstance();
    }

    @Override
    public void onCreate(String id, JsonValue object) throws SynchronizationException {
// TODO: Deprecate this; use resource interface instead.
        for (ObjectMapping mapping : mappings) {
            mapping.onCreate(id, object);
        }
    }

    @Override
    public void onUpdate(String id, JsonValue oldValue, JsonValue newValue) throws SynchronizationException {
// TODO: Deprecate this; use resource interface instead.
        for (ObjectMapping mapping : mappings) {
            mapping.onUpdate(id, oldValue, newValue);
        }
    }

    @Override
    public void onDelete(String id) throws SynchronizationException {
// TODO: Deprecate this; use resource interface instead.
        for (ObjectMapping mapping : mappings) {
            mapping.onDelete(id);
        }
    }

    @Override
    public void execute(Map<String, Object> context) throws ExecutionException {
// TODO: Deprecate this; use resource interface instead.
        try {
            JsonValue params = new JsonValue(context).get(CONFIGURED_INVOKE_CONTEXT);
            String action = params.get("action").asString();
            if ("reconcile".equals(action)) { // "action": "reconcile"
                if (params.get("mapping").required().isString()) {
                    reconcile(params.get("mapping").asString()); // "mapping": string (mapping name)
                } else if (params.get("mapping").required().isMap()) {
                    ObjectMapping schedulerMapping = new ObjectMapping(this, params.get("mapping"));
                    List<ObjectMapping> augmentedMappings = new ArrayList<ObjectMapping>(mappings);
                    schedulerMapping.initRelationships(this, augmentedMappings);
                        
                    schedulerMapping.recon(UUID.randomUUID().toString());
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
     * TODO: Description.
     *
     * @param mapping TODO.
     * @throws SynchronizationException TODO.
     * @return
     */
    public String reconcile(String mapping) throws SynchronizationException {
// TODO: Deprecate this; use resource interface instead.
        String reconId = UUID.randomUUID().toString();
        getMapping(mapping).recon(reconId); // throws SynchronizationException
        return reconId;
    }


    @Override
    public void create(String id, Map<String, Object> object) throws ObjectSetException {
        throw new ForbiddenException(); // nothing to create... yet
    }

    @Override
    public Map<String, Object> read(String id) throws ObjectSetException {
        throw new ForbiddenException(); // nothing to read... yet
    }

    @Override
    public void update(String id, String rev, Map<String, Object> object) throws ObjectSetException {
        throw new ForbiddenException(); // nothing to update... yet
    }

    @Override
    public void delete(String id, String rev) throws ObjectSetException {
        throw new ForbiddenException(); // nothing to delete... yet
    }

    @Override
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        throw new ForbiddenException(); // nothing to patch... yet
    }

    @Override
    public Map<String, Object> query(String id, Map<String, Object> params) throws ObjectSetException {
// TODO: Allow polling of asynchronous reconciliation status.
        throw new ForbiddenException(); // nothing to query... yet
    }

    @Override
    public Map<String, Object> action(String id, Map<String, Object> params) throws ObjectSetException {
        if (id != null) { // operation on entire set only... for now
            throw new NotFoundException();
        }
        Map<String, Object> result = null;
        JsonValue _params = new JsonValue(params, new JsonPointer("params"));
        Action action = _params.get("_action").required().asEnum(Action.class);
        try {
            switch (action) {
            case ONCREATE:
                id = _params.get("id").required().asString();
                LOGGER.debug("Synchronization _action=onCreate, id={}", id);
                onCreate(id, _params.get("_entity").expect(Map.class));
                break;
            case ONUPDATE:
                id = _params.get("id").required().asString();
                LOGGER.debug("Synchronization _action=onUpdate, id={}", id);
                onUpdate(id, null, _params.get("_entity").expect(Map.class));
                break;
            case ONDELETE:
                id = _params.get("id").required().asString();
                LOGGER.debug("Synchronization _action=onUpdate, id={}", id);
                onDelete(id);
                break;
            case RECON:
                result = new HashMap<String, Object>();
                String mapping = _params.get("mapping").required().asString();
                LOGGER.debug("Synchronization _action=recon, mapping={}", mapping);
                result.put("reconId", reconcile(mapping));
// TODO: Make asynchronous, and provide polling mechanism for reconciliation status.
                break;
            }
        } catch (SynchronizationException se) {
            throw new ConflictException(se);
        }
        return result;
    }
}

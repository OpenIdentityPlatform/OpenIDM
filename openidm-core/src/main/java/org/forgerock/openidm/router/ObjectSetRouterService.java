/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.router;

// Java Standard Edition
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;

// JSON Fluent library
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;

// ForgeRock OpenIDM
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.ObjectSetRouter;
import org.forgerock.openidm.scope.ScopeFactory;
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.ScriptException;
import org.forgerock.openidm.script.ScriptThrownException;
import org.forgerock.openidm.script.Scripts;

/**
 * Provides internal routing for a top-level object set.
 *
 * @author Paul C. Bryan
 * @author aegloff
 */
@Component(
    name = "org.forgerock.openidm.router",
    policy = ConfigurationPolicy.OPTIONAL,
    metatype = true,
    configurationFactory = false,
    immediate = true
)
@Properties({
    @Property(name = "service.description", value = "OpenIDM internal object set router"),
    @Property(name = "service.vendor", value = "ForgeRock AS"),
    @Property(name = "openidm.restlet.path", value = "/")
})
@Service
public class ObjectSetRouterService extends ObjectSetRouter {

    /** TODO: Description. */
    private final static Logger LOGGER = LoggerFactory.getLogger(ObjectSetRouterService.class);

    /** TODO: Description. */
    private static final String PREFIX_PROPERTY = "openidm.router.prefix";

    /** TODO: Description. */
    private ComponentContext context;

    private List<Filter> filters = new ArrayList<Filter>();

    /** TODO: Description. */
    private enum Method { CREATE, READ, UPDATE, DELETE, QUERY, ACTION, ALL };

    @Reference(
        name = "ref_ObjectSetRouterService_ObjectSet",
        referenceInterface = ObjectSet.class,
        bind = "bind",
        unbind = "unbind",
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        strategy = ReferenceStrategy.EVENT
    )
    protected int _dummy; // whiteboard pattern
    protected synchronized void bind(ObjectSet route, Map<String, Object> properties) {
        Object prefix = properties.get(PREFIX_PROPERTY);
        if (prefix != null && prefix instanceof String) { // service is specified as internally routable
            routes.put((String)prefix, route);
        }
    }
    protected synchronized void unbind(ObjectSet route, Map<String, Object> properties) {
        Object prefix = properties.get(PREFIX_PROPERTY);
        if (prefix != null && prefix instanceof String) { // service is specified as internally routable
            routes.remove((String)prefix);
        }
    }

    /** Scope factory service. */
    @Reference(
        name = "ref_ObjectSetRouterService_ScopeFactory",
        referenceInterface = ScopeFactory.class,
        bind = "bindScopeFactory",
        unbind = "unbindScopeFactory",
        cardinality = ReferenceCardinality.MANDATORY_UNARY,
        policy = ReferencePolicy.STATIC
    )
    private ScopeFactory scopeFactory;
    protected void bindScopeFactory(ScopeFactory scopeFactory) {
        this.scopeFactory = scopeFactory;
        this.scopeFactory.setRouter(this);
    }
    protected void unbindScopeFactory(ScopeFactory scopeFactory) {
        this.scopeFactory.setRouter(null);
        this.scopeFactory = null;
    }

    @Activate
    protected synchronized void activate(ComponentContext context) {
        LOGGER.info("Activate router configuration, properties: {}", context.getProperties());
        init(context);
    }
    
    @Modified
    protected synchronized void modified(ComponentContext context) {
        LOGGER.debug("Modified router configuration, properties: {}", context.getProperties());
        init(context);
    }
    
    /**
     * Initialize the router with configuration. Supports modifying router configuration.
     */
    private void init(ComponentContext context) {
        String pid = (String) context.getProperties().get("service.pid");
        String factoryPid = (String) context.getProperties().get("service.factoryPid");
        if (factoryPid != null) {
            LOGGER.warn("Factory config for router not allowed, ignoring config {}-{}", pid, factoryPid );
            return;
        }
        this.context = context;
        try {
            JsonValue config = new JsonValue(new JSONEnhancedConfig().getConfiguration(context));
            List<Filter> changedFilters = new ArrayList<Filter>();
            for (JsonValue jv : config.get("filters").expect(List.class)) { // optional
                changedFilters.add(new Filter(jv));
            }
            filters = changedFilters;
        } catch (JsonValueException jve) {
            // The router should stay up for basic support even with invalid config, do not throw Exception
            LOGGER.warn("Router configuration error", jve);
        } catch (Exception ex) {
            // The router should stay up for basic support even with invalid config, do not throw Exception
            LOGGER.warn("Failed to configure router", ex);
        }
    }
    
    @Deactivate
    protected synchronized void deactivate(ComponentContext context) {
        filters.clear();
        this.context = null;
    }

    private void onRequest(Method method, String id, Map<String, Object> object,
    Map<String, Object> params) throws ObjectSetException {
        for (Filter filter : filters) {
            filter.onRequest(method, id, object, params);
        }
    }

    private void onResponse(Method method, String id, Map<String, Object> object,
    Map<String, Object> params, Map<String, Object> result) throws ObjectSetException {
        for (Filter filter : filters) {
            filter.onResponse(method, id, object, params, result);
        }
    }

    @Override
    public void create(String id, Map<String, Object> object) throws ObjectSetException {
        onRequest(Method.CREATE, id, object, null);
        super.create(id, object);
        onResponse(Method.CREATE, id, object, null, null);
    }

    @Override
    public Map<String, Object> read(String id) throws ObjectSetException {
        onRequest(Method.READ, id, null, null);
        Map<String, Object> result = super.read(id);
        onResponse(Method.READ, id, null, null, result);
        return result;
    }

    @Override
    public void update(String id, String rev, Map<String, Object> object) throws ObjectSetException {
        onRequest(Method.UPDATE, id, object, null);
        super.update(id, rev, object);
        onResponse(Method.UPDATE, id, object, null, null);
    }

    @Override
    public void delete(String id, String rev) throws ObjectSetException {
        onRequest(Method.DELETE, id, null, null);
        super.delete(id, rev);
        onResponse(Method.DELETE, id, null, null, null); 
    }

    @Override
    public Map<String, Object> query(String id, Map<String, Object> params) throws ObjectSetException {
        onRequest(Method.QUERY, id, null, params);
        Map<String, Object> result = super.query(id, params);
        onResponse(Method.QUERY, id, null, params, result);
        return result;
    }

    @Override
    public Map<String, Object> action(String id, Map<String, Object> params) throws ObjectSetException {
        onRequest(Method.ACTION, id, null, params);
        Map<String, Object> result = super.action(id, params);
        onResponse(Method.ACTION, id, null, params, result);
        return result;
    }

    /**
     * TODO: Description.
     */
    private class Filter {

        /** TODO: Description. */
        private Pattern pattern;

        /** TODO: Description. */
        private String pointer;

        /** TODO: Description. */
        private HashSet<Method> methods;

        /** TODO: Description. */
        private Script condition;

        /** TODO: Description. */
        private Script onRequest;

        /** TODO: Description. */
        private Script onResponse;

        /**
         * TODO: Description.
         *
         * @param config TODO.
         * @throws JsonValueException TODO.
         */
        public Filter(JsonValue config) throws JsonValueException {
            pointer = config.getPointer().toString();
            pattern = config.get("pattern").asPattern();
            for (JsonValue method : config.get("methods").expect(List.class)) {
                if (methods == null) { // lazy initialization
                    methods = new HashSet<Method>();
                }
                methods.add(method.asEnum(Method.class));
            }
            condition = Scripts.newInstance("ObjectSetRouterService", config.get("condition")); // optional
            onRequest = Scripts.newInstance("ObjectSetRouterService", config.get("onRequest")); // optional
            onResponse = Scripts.newInstance("ObjectSetRouterService", config.get("onResponse")); // optional
        }

        /**
         * TODO: Description.
         *
         * @param method TODO.
         * @param id TODO.
         * @param object TODO.
         * @param params TODO.
         * @param result TODO.
         * @return
         */
        private Map<String, Object> newScope(Method method, String id,
        Map<String, Object> object, Map<String, Object> params, Map<String, Object> result) {
            Map<String, Object> scope = scopeFactory.newInstance();
            scope.put("method", method.toString().toLowerCase());
            scope.put("id", id);
            scope.put("object", object);
            scope.put("params", params);
            scope.put("result", result);
            return scope;
        }

        /**
         * TODO: Description.
         *
         * @param method TODO.
         * @param id TODO.
         * @return TODO.
         */
        private boolean matches(Method method, String id) {
            boolean result = (id != null && (methods == null || methods.contains(Method.ALL) || methods.contains(method)) && pattern.matcher(id).matches());
            LOGGER.debug("{} matches yielded {} on {}", new Object[]{pointer, Boolean.toString(result) , id});
            return result;
        }

        /**
         * TODO: Description.
         *
         * @param scope TODO
         * @return TODO.
         * @throws InternalServerErrorException TODO.
         */
        private boolean evalCondition(Map<String, Object> scope) throws InternalServerErrorException {
            boolean result = true; // default true unless script proves otherwise
            if (condition != null) {
                try {
                    result = Boolean.TRUE.equals(condition.exec(scope));
                } catch (ScriptException se) {
                    String msg = pointer + " condition script encountered exception";
                    LOGGER.debug(msg, se);
                    throw new InternalServerErrorException(msg, se);
                }
            }
            LOGGER.debug("{} evalCondition yielded {}", pointer, Boolean.toString(result));
            return result;
        }

       /**
         * TODO: Description.
         *
         * @param method
        * @param id
        * @param object
        * @param params
        * @throws ForbiddenException if the script throws an exception.
         * @throws InternalServerErrorException if any other exception occurs during execution.
         */
        void onRequest(Method method, String id, Map<String, Object> object, Map<String, Object> params)
        throws ForbiddenException, InternalServerErrorException {
            if (onRequest != null && matches(method, id)) {
                try {
                    Map<String, Object> scope = newScope(method, id, object, params, null);
                    if (evalCondition(scope)) {
                        LOGGER.debug("Calling {} onRequest script", pointer); 
                        onRequest.exec(scope);
                    }
                } catch (ScriptThrownException ste) {
                    throw new ForbiddenException(ste.getValue().toString()); // validation failed
                } catch (ScriptException se) {
                    String msg = pointer + " onRequest script encountered exception";
                    LOGGER.debug(msg, se);
                    throw new InternalServerErrorException(msg, se);
                }
            }
        }

        /**
         * TODO: Description.
         *
         * @param method
         * @param id
         * @param object
         * @param params
         * @param result
         * @throws InternalServerErrorException if any exception occurs during script execution.
         */
        void onResponse(Method method, String id, Map<String, Object> object, Map<String, Object> params,
        Map<String, Object> result) throws InternalServerErrorException {
            if (onResponse != null && matches(method, id)) {
                try {
                    Map<String, Object> scope = newScope(method, id, object, params, result);
                    if (evalCondition(scope)) {
                        LOGGER.debug("Calling {} onResponse script", pointer); 
                        onResponse.exec(scope);
                    }
                } catch (ScriptException se) {
                    String msg = pointer + " onResponse script encountered exception";
                    LOGGER.debug(msg, se);
                    throw new InternalServerErrorException(msg, se);
                }
            }
        }
    }
}

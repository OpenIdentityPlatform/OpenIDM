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
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;

// JSON Fluent library
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

// ForgeRock OpenIDM
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.ObjectSetRouter;
import org.forgerock.openidm.scope.ObjectSetFunctions;
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.ScriptException;
import org.forgerock.openidm.script.ScriptThrownException;
import org.forgerock.openidm.script.Scripts;

/**
 * Provides internal routing for a top-level object set.
 *
 * @author Paul C. Bryan
 */
@Component(
    name = "org.forgerock.openidm.router",
    policy = ConfigurationPolicy.OPTIONAL,
    immediate = true
)
@Properties({
    @Property(name = "service.description", value = "OpenIDM internal object set router"),
    @Property(name = "service.vendor", value = "ForgeRock AS")
})
@Service
public class ObjectSetRouterService extends ObjectSetRouter {

    /** TODO: Description. */
    private final static Logger LOGGER = LoggerFactory.getLogger(ObjectSetRouterService.class);

    /** TODO: Description. */
    private static final String PREFIX_PROPERTY = "openidm.router.prefix";

    /** TODO: Description. */
    private ComponentContext context;

    private final List<Trigger> triggers = new ArrayList<Trigger>();

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

    @Activate
    protected synchronized void activate(ComponentContext context) {
        this.context = context;
        JsonNode config = new JsonNode(new JSONEnhancedConfig().getConfiguration(context));
        try {
            for (JsonNode node : config.get("triggers").expect(List.class)) { // optional
                triggers.add(new Trigger(node));
            }
        } catch (JsonNodeException jne) {
            throw new ComponentException("Configuration error", jne);
        }
    }
    @Deactivate
    protected synchronized void deactivate(ComponentContext context) {
        triggers.clear();
        this.context = null;
    }

    private void before(Method method, String id, Map<String, Object> object,
    Map<String, Object> params) throws ObjectSetException {
        for (Trigger trigger : triggers) {
            trigger.before(method, id, object, params);
        }
    }

    private void after(Method method, String id, Map<String, Object> object,
    Map<String, Object> params, Map<String, Object> result) throws ObjectSetException {
        for (Trigger trigger : triggers) {
            trigger.after(method, id, object, params, result);
        }
    }

    @Override
    public void create(String id, Map<String, Object> object) throws ObjectSetException {
        before(Method.CREATE, id, object, null);
        super.create(id, object);
        after(Method.CREATE, id, object, null, null);
    }

    @Override
    public Map<String, Object> read(String id) throws ObjectSetException {
        before(Method.READ, id, null, null);
        Map<String, Object> result = super.read(id);
        after(Method.READ, id, null, null, result);
        return result;
    }

    @Override
    public void update(String id, String rev, Map<String, Object> object) throws ObjectSetException {
        before(Method.UPDATE, id, object, null);
        super.update(id, rev, object);
        after(Method.UPDATE, id, object, null, null);
    }

    @Override
    public void delete(String id, String rev) throws ObjectSetException {
        before(Method.DELETE, id, null, null);
        super.delete(id, rev);
        after(Method.DELETE, id, null, null, null); 
    }

    @Override
    public Map<String, Object> query(String id, Map<String, Object> params) throws ObjectSetException {
        before(Method.QUERY, id, null, params);
        Map<String, Object> result = super.query(id, params);
        after(Method.QUERY, id, null, params, result);
        return result;
    }

    @Override
    public Map<String, Object> action(String id, Map<String, Object> params) throws ObjectSetException {
        before(Method.ACTION, id, null, params);
        Map<String, Object> result = super.action(id, params);
        after(Method.ACTION, id, null, params, result);
        return result;
    }

    /**
     * TODO: Description.
     */
    private class Trigger {

        /** TODO: Description. */
        private Pattern pattern;

        /** TODO: Description. */
        private final HashSet<Method> methods = new HashSet<Method>();

        /** TODO: Description. */
        private Script before;

        /** TODO: Description. */
        private Script after;

        /**
         * TODO: Description.
         *
         * @param config TODO.
         * @throws JsonNodeException TODO.
         */
        public Trigger(JsonNode config) throws JsonNodeException {
            pattern = config.get("pattern").required().asPattern();
            for (JsonNode method : config.get("methods").required().expect(List.class)) {
                methods.add(method.asEnum(Method.class));
            }
            before = Scripts.newInstance(config.get("before"));
            after = Scripts.newInstance(config.get("after"));
        }

        private Map<String, Object> newScope(Method method, String id,
        Map<String, Object> object, Map<String, Object> params, Map<String, Object> result) {
            Map<String, Object> scope = ObjectSetFunctions.addToScope(new HashMap<String, Object>(), ObjectSetRouterService.this);
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
         * @return TODO.
         */
        private boolean matches(Method method, String id) {
            return (id != null && (methods.contains(Method.ALL) || methods.contains(method)) && pattern.matcher(id).matches());
        }

        /**
         * TODO: Description.
         *
         * @throws ForbiddenException if the script throws an exception.
         * @throws InternalServerErrorException if any other exception occurs during execution.
         */
        void before(Method method, String id, Map<String, Object> object, Map<String, Object> params)
        throws ForbiddenException, InternalServerErrorException {
            if (before != null && matches(method, id)) {
                try {
                    before.exec(newScope(method, id, object, params, null));
                } catch (ScriptThrownException ste) {
                    throw new ForbiddenException(ste.getValue().toString()); // validation failed
                } catch (ScriptException se) {
                    String msg = pattern.toString() + " before script encountered exception";
                    LOGGER.debug(msg, se);
                    throw new InternalServerErrorException(msg, se);
                }
            }
        }

        /**
         * TODO: Description.
         *
         * @throws InternalServerErrorException if any exception occurs during script execution.
         */
        void after(Method method, String id, Map<String, Object> object, Map<String, Object> params,
        Map<String, Object> result) throws InternalServerErrorException {
            if (after != null && matches(method, id)) {
                try {
                    after.exec(newScope(method, id, object, params, result));
                } catch (ScriptException se) {
                    String msg = pattern.toString() + " after script encountered exception";
                    LOGGER.debug(msg, se);
                    throw new InternalServerErrorException(msg, se);
                }
            }
        }
    }
}

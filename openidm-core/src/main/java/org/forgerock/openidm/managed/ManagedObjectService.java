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

package org.forgerock.openidm.managed;

// Java Standard Edition
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

// OpenIDM
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetRouter;
import org.forgerock.openidm.sync.SynchronizationListener;
import org.forgerock.openidm.scope.ObjectSetFunctions;

/**
 * Provides access to managed objects.
 *
 * @author Paul C. Bryan
 */
@Component(
    name = "org.forgerock.openidm.managed",
    immediate = true,
    policy = ConfigurationPolicy.REQUIRE
)
@Properties({
    @Property(name = "service.description", value = "OpenIDM managed objects service"),
    @Property(name = "service.vendor", value = "ForgeRock AS"),
    @Property(name = "openidm.router.prefix", value = "managed")
})
@Service
public class ManagedObjectService extends ObjectSetRouter {

    /** Internal object set router service. */
    @Reference(
        name = "ref_ManagedObjectService_ObjectSetRouterService",
        referenceInterface = ObjectSet.class,
        bind = "bindRouter",
        unbind = "unbindRouter",
        cardinality = ReferenceCardinality.MANDATORY_UNARY,
        policy = ReferencePolicy.STATIC,
        target = "(service.pid=org.forgerock.openidm.router)"
    )
    protected ObjectSet router;
    protected void bindRouter(ObjectSet router) {
        this.router = router;
    }
    protected void unbindRouter(ObjectSet router) {
        this.router = null;
    }

    /** Synchronization listeners. */
    @Reference(
        name="ref_ManagedObjectService_SynchronizationListener",
        referenceInterface=SynchronizationListener.class,
        bind="bindListener",
        unbind="unbindListener",
        cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
        policy=ReferencePolicy.DYNAMIC,
        strategy=ReferenceStrategy.EVENT
    )
    protected final HashSet<SynchronizationListener> listeners = new HashSet<SynchronizationListener>();
    protected void bindListener(SynchronizationListener listener) {
        listeners.add(listener);
    }
    protected void unbindListener(SynchronizationListener listener) {
        listeners.remove(listener);
    }

    /** Cryptographic service. */
    @Reference(
        name="ref_ManagedObjectService_CryptoService",
        referenceInterface=CryptoService.class,
        bind="bindCryptoService",
        unbind="unbindCryptoService",
        cardinality = ReferenceCardinality.MANDATORY_UNARY,
        policy = ReferencePolicy.STATIC
    )
    protected CryptoService cryptoService;
    protected void bindCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }
    protected void unbindCryptoService(CryptoService cryptoService) {
        this.cryptoService = null;
    }

    /** TODO: Description. */
    private ComponentContext context;

    /**
     * TODO: Description.
     *
     * @param context TODO.
     */
    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;
        JsonNode config = new JsonNode(new JSONEnhancedConfig().getConfiguration(context));
        try {
            for (JsonNode node : config.get("objects").expect(List.class)) {
                ManagedObjectSet objectSet = new ManagedObjectSet(this, node); // throws JsonNodeException
                String name = objectSet.getName();
                if (routes.containsKey(name)) {
                    throw new JsonNodeException(node, "object " + name + " already defined");
                }
                routes.put(name, objectSet);
            }
        } catch (JsonNodeException jne) {
            throw new ComponentException("Configuration error", jne);
        }
    }

    /**
     * TODO: Description.
     *
     * @param context TODO.
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        this.context = null;
        routes.clear();
    }

    /**
     * @return The internal object set for which operations will be applied. If there is no
     * router, throws {@link InternalServerErrorException}.
     * @throws org.forgerock.openidm.objset.InternalServerErrorException
     */
    ObjectSet getRouter() throws InternalServerErrorException {
        if (router == null) {
            throw new InternalServerErrorException("Not bound to internal router");
        }
        return router;
    }

    /**
     * TODO: Description.
     *
     * @return TODO.
     * @throws InternalServerErrorException TODO.
     */
    Map<String, Object> newScope() throws InternalServerErrorException {
        return ObjectSetFunctions.addToScope(new HashMap<String, Object>(), getRouter());
    }

    /**
     * TODO: Description.
     * @return
     */
    Set<SynchronizationListener> getListeners() {
        return listeners;
    }

    /**
     * TODO: Description.
     * @return
     */
    CryptoService getCryptoService() {
        return cryptoService;
    }
}

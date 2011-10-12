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

package org.forgerock.openidm.scope.impl;

// OSGi Framework
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;

// Java Standard Edition
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;
import org.forgerock.json.fluent.JsonPointer;

// OpenIDM
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.scope.ScopeFactory;
import org.forgerock.openidm.script.Function;


/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 */
@Component(
    name = "org.forgerock.openidm.scope",
    policy = ConfigurationPolicy.OPTIONAL
)
@Properties({
    @Property(name = "service.description", value = "OpenIDM scope factory service"),
    @Property(name = "service.vendor", value = "ForgeRock AS")
})
@Service
public class ScopeFactoryService implements ScopeFactory {

    /** Cryptographic service. */
    @Reference(
        name="ref_ScopeFactoryService_CryptoService",
        referenceInterface=CryptoService.class,
        bind="bindCryptoService",
        unbind="unbindCryptoService",
        cardinality = ReferenceCardinality.MANDATORY_UNARY,
        policy = ReferencePolicy.STATIC
    )
    private CryptoService cryptoService;
    protected void bindCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }
    protected void unbindCryptoService(CryptoService cryptoService) {
        this.cryptoService = null;
    }

    private ObjectSet router;

    @Override
    public void setRouter(ObjectSet router) {
        this.router = router;
    }

    /**
     * TODO: Description.
     *
     * @param params TODO.
     * @return TODO.
     */
    private static JsonNode paramsNode(List<Object> params) {
        return new JsonNode(params, new JsonPointer("params"));
    }

    @Override
    public Map<String, Object> newInstance() {

        HashMap<String, Object> openidm = new HashMap<String, Object>();

        // create(string id, object value)
        openidm.put("create", new Function() {
            @Override
            public Object call(Map<String, Object> scope, Map<String, Object> _this, List<Object> params) throws Throwable {
                JsonNode node = paramsNode(params);
                router.create(node.get(0).required().asString(), node.get(1).required().asMap());
                return null; // no news is good news
            }
        });

        // read(string id)
        openidm.put("read", new Function() {
            @Override
            public Object call(Map<String, Object> scope, Map<String, Object> _this, List<Object> params) throws Throwable {
                JsonNode node = paramsNode(params);
                try {
                    return router.read(node.get(0).required().asString());
                } catch (NotFoundException nfe) {
                    return null; // indicates no such record without throwing exception
                }
            }
        });

        // update(string id, string rev, object value)
        openidm.put("update", new Function() {
            @Override
            public Object call(Map<String, Object> scope, Map<String, Object> _this, List<Object> params) throws Throwable {
                JsonNode node = paramsNode(params);
                router.update(node.get(0).required().asString(), node.get(1).asString(), node.get(2).required().asMap());
                return null; // no news is good news
            }
        });

        // delete(string id, string rev)
        openidm.put("delete", new Function() {
            @Override
            public Object call(Map<String, Object> scope, Map<String, Object> _this, List<Object> params) throws Throwable {
                JsonNode node = paramsNode(params);
                router.delete(node.get(0).required().asString(), node.get(1).asString());
                return null; // no news is good news
            }
        });

        // query(string id, object params)
        openidm.put("query", new Function() {
            @Override
            public Object call(Map<String, Object> scope, Map<String, Object> _this, List<Object> params) throws Throwable {
                JsonNode node = paramsNode(params);
                return router.query(node.get(0).required().asString(), node.get(1).required().asMap());
            }
        });

        // action(string id, object params)
        openidm.put("action", new Function() {
            @Override
            public Object call(Map<String, Object> scope, Map<String, Object> _this, List<Object> params) throws Throwable {
                JsonNode node = paramsNode(params);
                return router.action(node.get(0).required().asString(), node.get(1).required().asMap());
            }
        });

        // encrypt(any value, string cipher, string alias) 
        openidm.put("encrypt", new Function() {
            @Override
            public Object call(Map<String, Object> scope, Map<String, Object> _this, List<Object> params) throws Throwable {
                JsonNode node = paramsNode(params);
                return cryptoService.encrypt(node.get(0).required(), node.get(1).required().asString(), node.get(2).required().asString()).getValue();
            }
        });

        // decrypt(any value)
        openidm.put("decrypt", new Function() {
            @Override
            public Object call(Map<String, Object> scope, Map<String, Object> _this, List<Object> params) throws Throwable {
                JsonNode node = paramsNode(params);
                return cryptoService.decrypt(node.get(0).required()).getValue();
            }
        });

        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("openidm", openidm);

        return result;
    }
}

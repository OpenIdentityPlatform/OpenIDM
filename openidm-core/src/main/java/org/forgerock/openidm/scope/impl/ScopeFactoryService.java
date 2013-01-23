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
 * Copyright ¬© 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.scope.impl;

// Java SE
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceAccessor;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.scope.ScopeFactory;
import org.forgerock.openidm.script.Function;
import org.forgerock.util.Factory;
import org.forgerock.util.LazyMap;
import org.osgi.service.component.ComponentContext;

/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 */
@Component(
    name = "org.forgerock.openidm.scope",
    immediate = true, 
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
        policy = ReferencePolicy.DYNAMIC
    )
    private CryptoService cryptoService;
    protected void bindCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }
    protected void unbindCryptoService(CryptoService cryptoService) {
        this.cryptoService = null;
    }

    private JsonResource router;

    @Override
    public void setRouter(JsonResource router) {
        this.router = router;
    }
    
    /**
     * TODO: Description.
     *
     * @param params TODO.
     * @return TODO.
     */
    private static JsonValue paramsValue(List<Object> params) {
        return new JsonValue(params, new JsonPointer("params"));
    }

    /**
     * TODO: Description.
     *
     * @param context TODO.
     * @return TODO.
     */
    private JsonResourceAccessor accessor(JsonValue context) {
        return new JsonResourceAccessor(router, context);
    }

    private IdentityServer identityServer;
    private Map<String, String> propertiesCache;
    
    @Activate
    void activate(ComponentContext compContext) {
        identityServer = IdentityServer.getInstance();
        propertiesCache = new ConcurrentHashMap<String, String>();
    }
    
    private String getProperty(String key, String defaultValue, boolean useCache) {
        if (useCache) {
            if (!propertiesCache.containsKey(key)) {
                String value = identityServer.getProperty(key, defaultValue);
                propertiesCache.put(key, value);
            }
            return propertiesCache.get(key);
        } else {
            return identityServer.getProperty(key, defaultValue);
        }
    }
    
    private String getWorkingLocation() {
        return identityServer.getWorkingLocation().getPath();
    }
    
    private String getProjectLocation() {
        return identityServer.getProjectLocation().getPath();
    }
    
    private String getInstallLocation() {
        return identityServer.getInstallLocation().getPath();
    }
    
    @Override
    public Map<String, Object> newInstance(final JsonValue context) {
        Map<String, Object> scope = new HashMap<String, Object>();
        scope.put("identityServer", new LazyMap<String, Object>(new Factory<Map<String, Object>>() {
            @Override public Map<String, Object> newInstance() {
                HashMap<String, Object> identityServer = new HashMap<String, Object>();
                // getProperty
                identityServer.put("getProperty", new Function() {
                    @Override
                    public Object call(Map<String, Object> scope,
                     Map<String, Object> _this, List<Object> params) throws Throwable {
                        JsonValue p = paramsValue(params);
                        String key = p.get(0).required().asString();
                        String def = p.get(1).asString();
                        boolean useCache = p.get(2).defaultTo(false).asBoolean();
                        return getProperty(key, def, useCache);
                    }
                });
                identityServer.put("getWorkingLocation", new Function() {
                    @Override
                    public Object call(Map<String, Object> scope,
                     Map<String, Object> _this, List<Object> params) throws Throwable {
                        return getWorkingLocation();
                    }
                });
                identityServer.put("getProjectLocation", new Function() {
                    @Override
                    public Object call(Map<String, Object> scope,
                     Map<String, Object> _this, List<Object> params) throws Throwable {
                        return getProjectLocation();
                    }
                });
                identityServer.put("getInstallLocation", new Function() {
                    @Override
                    public Object call(Map<String, Object> scope,
                     Map<String, Object> _this, List<Object> params) throws Throwable {
                        return getInstallLocation();
                    }
                });
                return identityServer;
            }
        }));
        scope.put("openidm", new LazyMap<String, Object>(new Factory<Map<String, Object>>() {
            @Override public Map<String, Object> newInstance() {
                HashMap<String, Object> openidm = new HashMap<String, Object>();
                // create(string id, any value)
                openidm.put("create", new Function() {
                    @Override
                    public Object call(Map<String, Object> scope,
                     Map<String, Object> _this, List<Object> params) throws Throwable {
                        JsonValue p = paramsValue(params);
                        JsonValue result = accessor(context).create(
                          p.get(0).required().asString(),
                          p.get(1).required().expect(Map.class) // OpenIDM resources are maps only
                        );
                        if (result != null) {
                            return result.getWrappedObject();
                        } else {
                            return null;
                        }
                    }
                });
                // read(string id)
                openidm.put("read", new Function() {
                    @Override
                    public Object call(Map<String, Object> scope,
                     Map<String, Object> _this, List<Object> params) throws Throwable {
                        JsonValue p = paramsValue(params);
                        try {
                            JsonValue result = accessor(context).read(
                              p.get(0).required().asString()
                            );
                            if (result != null) {
                                return result.getWrappedObject();
                            } else {
                                return null;
                            }
                        } catch (JsonResourceException jre) {
                            if (jre.getCode() == JsonResourceException.NOT_FOUND) {
                                return null; // indicates no such record without throwing exception
                            } else {
                                throw jre;
                            }
                        }
                    }
                });
                // update(string id, string rev, object value)
                openidm.put("update", new Function() {
                    @Override
                    public Object call(Map<String, Object> scope,
                     Map<String, Object> _this, List<Object> params) throws Throwable {
                        JsonValue p = paramsValue(params);
                        JsonValue result = accessor(context).update(
                          p.get(0).required().asString(),
                          p.get(1).asString(),
                          p.get(2).required().expect(Map.class) // OpenIDM resources are maps only
                        );
                        if (result != null) {
                            return result.getWrappedObject();
                        } else {
                            return null;
                        }
                    }
                });
                // patch(string id, string rev, object value)
                openidm.put("patch", new Function() {
                    @Override
                    public Object call(Map<String, Object> scope,
                     Map<String, Object> _this, List<Object> params) throws Throwable {
                        JsonValue p = paramsValue(params);
                        JsonValue result = accessor(context).patch(
                         p.get(0).required().asString(),
                         p.get(1).asString(),
                         p.get(2).required().expect(List.class)
                        );
                        if (result != null) {
                            return result.getWrappedObject();
                        } else {
                            return null;
                        }
                    }
                });
                // delete(string id, string rev)
                openidm.put("delete", new Function() {
                    @Override
                    public Object call(Map<String, Object> scope,
                     Map<String, Object> _this, List<Object> params) throws Throwable {
                        JsonValue p = paramsValue(params);
                        JsonValue result = accessor(context).delete(
                          p.get(0).required().asString(),
                          p.get(1).asString()
                        );
                        if (result != null) {
                            return result.getWrappedObject();
                        } else {
                            return null;
                        }
                    }
                });
                // query(string id, object params)
                openidm.put("query", new Function() {
                    @Override
                    public Object call(Map<String, Object> scope,
                     Map<String, Object> _this, List<Object> params) throws Throwable {
                        JsonValue p = paramsValue(params);
                        JsonValue result = accessor(context).query(
                          p.get(0).required().asString(),
                          p.get(1).required()
                        );
                        if (result != null) {
                            return result.getWrappedObject();
                        } else {
                            return null;
                        }
                    }
                });
                // action(string id, object params, any value)
                openidm.put("action", new Function() {
                    @Override
                    public Object call(Map<String, Object> scope,
                     Map<String, Object> _this, List<Object> params) throws Throwable {
                        JsonValue p = paramsValue(params);
                        JsonValue value = p.get(2); // optional parameter
                        if (value.isNull()) {
                            value = p.get(1).get("_entity"); // backwards compatibility
                        }
                        JsonValue result = accessor(context).action(
                          p.get(0).required().asString(),
                          p.get(1).required(),
                          value
                        );
                        if (result != null) {
                            return result.getWrappedObject();
                        } else {
                            return null;
                        }
                    }
                });
                // encrypt(any value, string cipher, string alias)
                openidm.put("encrypt", new Function() {
                    @Override
                    public Object call(Map<String, Object> scope,
                     Map<String, Object> _this, List<Object> params) throws Throwable {
                        JsonValue jv = paramsValue(params);
                        JsonValue result = cryptoService.encrypt(
                          jv.get(0).required(),
                          jv.get(1).required().asString(),
                          jv.get(2).required().asString()
                        );
                        if (result != null) {
                            return result.getWrappedObject();
                        } else {
                            return null;
                        }
                    }
                });
                // decrypt(any value)
                openidm.put("decrypt", new Function() {
                    @Override
                    public Object call(Map<String, Object> scope,
                     Map<String, Object> _this, List<Object> params) throws Throwable {
                        JsonValue jv = paramsValue(params);
                        JsonValue result = cryptoService.decrypt(
                          jv.get(0).required()
                        );
                        if (result != null) {
                            return result.getObject();
                        } else {
                            return null;
                        }
                    }
                });
                // isEncrypted(any value)
                openidm.put("isEncrypted", new Function() {
                    @Override
                    public Object call(Map<String, Object> scope,
                     Map<String, Object> _this, List<Object> params) throws Throwable {
                        JsonValue jv = paramsValue(params);
                        return cryptoService.isEncrypted(jv.get(0).required());
                    }
                });
                return openidm;
            }
        }));
        return scope;
    }
}


/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.script.impl;

import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.File;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
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
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.crypto.JsonCrypto;
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.quartz.impl.ExecutionException;
import org.forgerock.openidm.quartz.impl.ScheduledService;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.engine.ScriptEngineFactory;
import org.forgerock.script.exception.ScriptCompilationException;
import org.forgerock.script.exception.ScriptThrownException;
import org.forgerock.script.registry.ScriptRegistryImpl;
import org.forgerock.script.scope.Function;
import org.forgerock.script.scope.FunctionFactory;
import org.forgerock.script.scope.Parameter;
import org.forgerock.script.scope.ResourceFunctions;
import org.forgerock.script.source.DirectoryContainer;
import org.forgerock.script.source.SourceUnit;
import org.forgerock.util.promise.Promise;
import org.ops4j.pax.swissbox.extender.BundleWatcher;
import org.ops4j.pax.swissbox.extender.ManifestEntry;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
@Component(name = ScriptRegistryService.PID, policy = ConfigurationPolicy.REQUIRE, metatype = true,
        description = "OpenIDM Script Registry Service", immediate = true)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Script Registry Service"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/script*") })
@References({
    @Reference(name = "CryptoServiceReference", referenceInterface = CryptoService.class,
            bind = "bindCryptoService", unbind = "unbindCryptoService",
            cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC),
    @Reference(name = "ConnectionFactoryReference", referenceInterface = ConnectionFactory.class,
            bind = "setConnectionFactory", unbind = "unsetConnectionFactory",
            target = "(service.pid=org.forgerock.openidm.internal)",
            cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC),
    @Reference(name = "ScriptEngineFactoryReference",
            referenceInterface = ScriptEngineFactory.class, bind = "addingEntries",
            unbind = "removingEntries", cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy = ReferencePolicy.DYNAMIC),
    @Reference(name = "FunctionReference", referenceInterface = Function.class,
            bind = "bindFunction", unbind = "unbindFunction",
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC,
            target = "(" + ScriptRegistryService.SCRIPT_NAME + "=*)") })
public class ScriptRegistryService extends ScriptRegistryImpl implements RequestHandler, ScheduledService {

    public static final Set<String> reservedNames;

    static {
        Set<String> _reservedNames = new HashSet<String>(15);
        _reservedNames.add("create");
        _reservedNames.add("read");
        _reservedNames.add("update");
        _reservedNames.add("patch");
        _reservedNames.add("query");
        _reservedNames.add("delete");
        _reservedNames.add("action");
        _reservedNames.add("encrypt");
        _reservedNames.add("decrypt");
        _reservedNames.add("isEncrypted");

        for (IdentityServerFunctions f : IdentityServerFunctions.values()) {
            _reservedNames.add(f.name());
        }
        reservedNames = Collections.unmodifiableSet(_reservedNames);
    }

    // TODO Move to public package
    public static final String SCRIPT_NAME = "org.forgerock.openidm.script.name";

    // Public Constants
    public static final String PID = "org.forgerock.openidm.script";

    private ConnectionFactory connectionFactory = null;

    /**
     * Setup logging for the {@link ScriptRegistryService}.
     */
    // private static final LocalizedLogger logger =
    // LocalizedLogger.getLocalizedLogger(ScriptRegistryService.class);
    private static final Logger logger = LoggerFactory.getLogger(ScriptRegistryService.class);
    private static final String PROP_IDENTITY_SERVER = "identityServer";
    private static final String PROP_OPENIDM = "openidm";
    private static final String PROP_CONSOLE = "console";
    
    private static final String SOURCE_DIRECTORY = "directory";
    private static final String SOURCE_FILE = "file";
    private static final String SOURCE_SUBDIRECTORIES = "subdirectories";
    private static final String SOURCE_VISIBILITY = "visibility";
    private static final String SOURCE_TYPE = "type";
    private static final String SOURCE_GLOBALS = "globals";

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private EnhancedConfig enhancedConfig;


    private final ConcurrentMap<String, Object> openidm = new ConcurrentHashMap<String, Object>();
    private static final ConcurrentMap<String, Object> propertiesCache = new ConcurrentHashMap<String, Object>();

    private enum Action {
        compile, eval
    }
    
    private BundleWatcher<ManifestEntry> manifestWatcher;

    @Activate
    protected void activate(ComponentContext context) throws Exception {
        JsonValue configuration = enhancedConfig.getConfigurationAsJson(context);

        setConfiguration(configuration.required().asMap());

        HashMap<String, Object> identityServer = new HashMap<String, Object>();
        for (IdentityServerFunctions f : IdentityServerFunctions.values()) {
            identityServer.put(f.name(), f);
        }
        Map<String, Object> console = new HashMap<String, Object>();
        console.put("log", new Function<Void>() {
            @Override
            public Void call(Parameter scope, Function<?> callback, Object... arguments) throws ResourceException, NoSuchMethodException {
                if (arguments.length > 0) {
                    if (arguments[0] instanceof String) {
                        System.out.println((String) arguments[0]);
                    }
                }
                return null;
            }
        });
        put(PROP_IDENTITY_SERVER, identityServer);
        put(PROP_CONSOLE, console);
        put(PROP_OPENIDM, openidm);
        JsonValue properties = configuration.get("properties");
        if (properties.isMap()) {
            for (Map.Entry<String, Object> entry : properties.asMap().entrySet()) {
                if (PROP_IDENTITY_SERVER.equals(entry.getKey())
                        || PROP_OPENIDM.equals(entry.getKey())
                        || PROP_CONSOLE.equals(entry.getKey())) {
                    continue;
                }
                put(entry.getKey(), entry.getValue());
            }
        }

        try {
            JsonValue sources = configuration.get("sources");
            if (!sources.isNull()) {
                // Must reverse default-first config since ScriptRegistryImpl loads scripts from the first dir it hits.
                List<String> keys = new ArrayList(sources.keys());
                Collections.reverse(keys);

                for (String key : keys) {
                    JsonValue source = sources.get(key);
                    String directory = source.get(SOURCE_DIRECTORY).asString();
                    URL directoryURL = (new File(directory)).toURI().toURL();
                    /* TODO: Support addition config properties (currently set to defaults in commons)
                    JsonValue subDirValue = source.get(SOURCE_SUBDIRECTORIES).defaultTo("auto-true");
                    boolean subdirectories = true;
                    if (subDirValue.isBoolean()) {
                        subdirectories = subDirValue.asBoolean();
                    } else {
                        subdirectories = Boolean.parseBoolean(subDirValue.asString());
                    }
                    String type = source.get(SOURCE_TYPE).defaultTo("auto-detect").asString();
                    String visibility = source.get(SOURCE_VISIBILITY).defaultTo("public").asString();
                    */
                    DirectoryContainer dc = new DirectoryContainer(key, directoryURL);
                    addSourceUnit(dc);
                }
            }
        } catch (Exception e) {
            logger.error("Error loading sources", e);
            throw e;
        }

        // OPENIDM-1746 Set the script registry class loader to this class, so that any libraries
        // bundled as an OSGI-Fragment attached to this bundle will be "seen" by scripts.
        setRegistryLevelScriptClassLoader(this.getClass().getClassLoader());

        /*
         * manifestWatcher = new BundleWatcher<ManifestEntry>(context, new
         * ScriptEngineManifestScanner(), null); manifestWatcher.start();
         */

        logger.info("OpenIDM Script Service component is activated.");
    }

    @Modified
    protected void modified(ComponentContext context) {
        JsonValue configuration = enhancedConfig.getConfigurationAsJson(context);
        setConfiguration(configuration.required().asMap());
        propertiesCache.clear();
        Set<String> keys =
                null != getBindings() ? new HashSet<String>(getBindings().keySet()) : Collections
                        .<String> emptySet();
        keys.remove(PROP_OPENIDM);
        keys.remove(PROP_IDENTITY_SERVER);
        keys.remove(PROP_CONSOLE);
        JsonValue properties = configuration.get("properties");
        if (properties.isMap()) {
            for (Map.Entry<String, Object> entry : properties.asMap().entrySet()) {
                if (PROP_IDENTITY_SERVER.equals(entry.getKey())
                        || PROP_OPENIDM.equals(entry.getKey())
                        || PROP_CONSOLE.equals(entry.getKey())) {
                    continue;
                }
                put(entry.getKey(), entry.getValue());
                keys.remove(entry.getKey());
            }
        }
        if (!keys.isEmpty()) {
            for (String name : keys) {
                getBindings().remove(name);
            }
        }
        logger.info("OpenIDM Script Service component is modified.");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (null != manifestWatcher) {
            manifestWatcher.stop();
        }
        propertiesCache.clear();
        openidm.clear();
        setBindings(null);
        logger.info("OpenIDM Script Service component is deactivated.");
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        openidm.put("create", ResourceFunctions.newCreateFunction(connectionFactory));
        openidm.put("read", ResourceFunctions.newReadFunction(connectionFactory));
        openidm.put("update", ResourceFunctions.newUpdateFunction(connectionFactory));
        openidm.put("patch", ResourceFunctions.newPatchFunction(connectionFactory));
        openidm.put("query", ResourceFunctions.newQueryFunction(connectionFactory));
        openidm.put("delete", ResourceFunctions.newDeleteFunction(connectionFactory));
        openidm.put("action", ResourceFunctions.newActionFunction(connectionFactory));
        this.connectionFactory = connectionFactory;
        logger.info("Resource functions are enabled");
    }

    public void unsetConnectionFactory(ConnectionFactory connectionFactory) {
        openidm.remove("create");
        openidm.remove("read");
        openidm.remove("update");
        openidm.remove("patch");
        openidm.remove("query");
        openidm.remove("delete");
        openidm.remove("action");
        this.connectionFactory = null;
        logger.info("Resource functions are disabled");
    }

    protected void bindCryptoService(final CryptoService cryptoService) {
        // encrypt(any value, string cipher, string alias)
        openidm.put("encrypt", new Function<JsonValue>() {

            static final long serialVersionUID = 1L;

            public JsonValue call(Parameter scope, Function<?> callback, Object... arguments)
                    throws ResourceException, NoSuchMethodException {
                if (arguments.length == 3) {
                    JsonValue value = null;
                    String cipher = null;
                    String alias = null;
                    if (arguments[0] instanceof Map
                            || arguments[0] instanceof List
                            || arguments[0] instanceof String
                            || arguments[0] instanceof Number
                            || arguments[0] instanceof Boolean) {
                        value = new JsonValue(arguments[0]);
                    } else if (arguments[0] instanceof JsonValue) {
                        value = (JsonValue) arguments[0];
                    } else {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "encrypt", arguments));
                    }
                    if (arguments[1] instanceof String) {
                        cipher = (String) arguments[1];
                    } else if (arguments[0] == null) {
                        cipher = ServerConstants.SECURITY_CRYPTOGRAPHY_DEFAULT_CIPHER;
                    } else {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "encrypt", arguments));
                    }

                    if (arguments[2] instanceof String) {
                        alias = (String) arguments[2];
                    } else {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "encrypt", arguments));
                    }
                    try {
                        return cryptoService.encrypt(value, cipher, alias);
                    } catch (JsonCryptoException e) {
                        throw new InternalServerErrorException(e.getMessage(), e);
                    }
                } else {
                    throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                            "encrypt", arguments));
                }
            }
        });
        // decrypt(any value)
        openidm.put("decrypt", new Function<JsonValue>() {

            static final long serialVersionUID = 1L;

            public JsonValue call(Parameter scope, Function<?> callback, Object... arguments)
                    throws ResourceException, NoSuchMethodException {
                if (arguments.length == 1
                        && (arguments[0] instanceof Map || arguments[0] instanceof JsonValue)) {
                    return cryptoService
                            .decrypt(arguments[0] instanceof JsonValue ? (JsonValue) arguments[0]
                                    : new JsonValue(arguments[0]));
                } else {
                    throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                            "decrypt", arguments));
                }
            }
        });
        // isEncrypted(any value)
        openidm.put("isEncrypted", new Function<Boolean>() {

            static final long serialVersionUID = 1L;

            public Boolean call(Parameter scope, Function<?> callback, Object... arguments)
                    throws ResourceException, NoSuchMethodException {
                if (arguments == null || arguments.length == 0) {
                    return false;
                } else if (arguments.length == 1) {
                    return JsonCrypto.isJsonCrypto(arguments[0] instanceof JsonValue
                        ? (JsonValue) arguments[0] 
                        : new JsonValue(arguments[0]));
                } else {
                    throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                            "isEncrypted", arguments));
                }
            }
        });
        logger.info("Crypto functions are enabled");
    }

    protected void unbindCryptoService(final CryptoService function) {
        openidm.remove("encrypt");
        openidm.remove("decrypt");
        openidm.remove("isEncrypted");
        logger.info("Crypto functions are disabled");
    }

    protected void bindFunction(final Function function, Map properties) {
        Object name = properties.get(SCRIPT_NAME);
        if (name instanceof String && StringUtils.isNotBlank((String) name)
                && !reservedNames.contains((String) name)) {
            openidm.put((String) name, function);
            logger.info("openidm.{} function is enabled", name);
        }
    }

    protected void unbindFunction(final Function function, Map properties) {
        Object name = properties.get(SCRIPT_NAME);
        if (name instanceof String && StringUtils.isNotBlank((String) name)
                && !reservedNames.contains((String) name)) {
            openidm.remove(name, function);
            logger.info("openidm.{} function is disabled", name);
        }
    }

    @Override
    public ScriptEntry takeScript(JsonValue script) throws ScriptException {
        JsonValue scriptConfig = script.clone();
        if (scriptConfig.get(SourceUnit.ATTR_NAME).isNull()) {
            JsonValue file = scriptConfig.get(SOURCE_FILE);
            JsonValue source = scriptConfig.get(SourceUnit.ATTR_SOURCE);

            if (!file.isNull()) {
                // If we do not have a name use the file.
                scriptConfig.put(SourceUnit.ATTR_NAME, file.asString());
            } else if (!source.isNull()) {
                /*
                 * We assign a name here otherwise ScriptRegistryImpl.takeScript() assigns a random UUID.
                 * This results in a newly created and cached class on every invocation.
                 */
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-1");

                    // digest source AND type as we could have identical source for different types
                    String type = scriptConfig.get(SourceUnit.ATTR_TYPE).asString();
                    byte[] nameAndType = (source.asString() + type).getBytes();
                    byte[] digest = md.digest(nameAndType);
                    String name = DatatypeConverter.printHexBinary(digest);

                    scriptConfig.put(SourceUnit.ATTR_NAME, name);
                } catch (NoSuchAlgorithmException e) {
                    // SHA-1 is a required implementation. This should never happen.
                    logger.error("Could not get SHA-1 MessageDigest instance. This should be implemented on any standard JVM.", e);
                }
            }
        }

        // Get and remove any defined globals
        JsonValue globals = scriptConfig.get(SOURCE_GLOBALS);
        scriptConfig.remove(SOURCE_GLOBALS);
        
        // Create the script entry
        ScriptEntry scriptEntry = super.takeScript(scriptConfig);
        
        // Add the globals (if any) to the script bindings
        if (!globals.isNull() && globals.isMap()) {
            for (String key : globals.keys()) {
                scriptEntry.put(key, globals.get(key));
            }
        }
        return scriptEntry;
    }
    
    private static enum IdentityServerFunctions implements Function<Object> {
        getProperty {
            public Object call(Parameter scope, Function<?> callback, Object... arguments)
                    throws ResourceException, NoSuchMethodException {
                boolean useCache = false;
                if (arguments.length < 1 || arguments.length > 3) {
                    throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(this.name(), arguments));
                }
                if (arguments.length == 3) {
                    useCache = (Boolean) arguments[2];
                }
                if (arguments[0] instanceof String) {
                    String name = (String) arguments[0];
                    Object result = null;
                    if (useCache) {
                        result = propertiesCache.get(name);
                    }
                    if (null == result) {
                        Object defaultValue = arguments.length == 2 ? arguments[1] : null;
                        result = IdentityServer.getInstance().getProperty(name, defaultValue, Object.class);
                        propertiesCache.putIfAbsent(name, result);
                    }
                    return result;
                } else {
                    throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(this.name(), arguments));
                }
            }
        },

        getWorkingLocation {
            public Object call(Parameter scope, Function callback, Object... arguments)
                    throws ResourceException, NoSuchMethodException {
                if (arguments.length != 0) {
                    throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(this.name(), arguments));
                }
                return IdentityServer.getInstance().getWorkingLocation().getAbsolutePath();
            }
        },

        getProjectLocation {
            public Object call(Parameter scope, Function callback, Object... arguments)
                    throws ResourceException, NoSuchMethodException {
                if (arguments.length != 0) {
                    throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(this
                            .name(), arguments));
                }
                return IdentityServer.getInstance().getProjectLocation().getAbsolutePath();
            }
        },
        getInstallLocation {
            public Object call(Parameter scope, Function callback, Object... arguments)
                    throws ResourceException, NoSuchMethodException {
                if (arguments.length != 0) {
                    throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(this
                            .name(), arguments));
                }
                return IdentityServer.getInstance().getInstallLocation().getAbsolutePath();
            }
        };
        static final long serialVersionUID = 1L;
    }
    
    private boolean isSourceUnit(String name) {
        if (SourceUnit.ATTR_NAME.equals(name) ||
                SourceUnit.ATTR_REVISION.equals(name) ||
                SourceUnit.ATTR_SOURCE.equals(name) ||
                SourceUnit.ATTR_TYPE.equals(name) ||
                SourceUnit.ATTR_VISIBILITY.equals(name) ||
                SourceUnit.AUTO_DETECT.equals(name) ||
                SOURCE_FILE.equals(name) ||
                SOURCE_GLOBALS.equals(name)) {
            return true;
        }
        return false;
    }

    // ----- Implementation of RequestHandler interface

    public Promise<ActionResponse, ResourceException> handleAction(final Context context, final ActionRequest request) {
        String resourcePath = request.getResourcePath();
        JsonValue content = request.getContent();
        Map<String, Object> bindings = new HashMap<String, Object>();
        JsonValue config = new JsonValue(new HashMap<String, Object>());
        ScriptEntry scriptEntry = null;
        try {
            if (resourcePath == null || "".equals(resourcePath)) {
                for (String key : content.keys()) {
                    if (isSourceUnit(key)) {
                        config.put(key, content.get(key).getObject());
                    } else {
                        bindings.put(key, content.get(key).getObject());
                    }
                }
                // The script will be in the request content
                scriptEntry = takeScript(config);
                // Add any additional parameters to the map of bindings
                bindings.putAll(request.getAdditionalParameters());
            } else {
                throw new NotSupportedException("Actions are not supported for resource instances");
            }
            switch (request.getActionAsEnum(Action.class)) {
                case compile:
                    if (scriptEntry.isActive()) {
                        // just get the script - compilation technically happened above in takeScript
                        scriptEntry.getScript(context);
                        return newActionResponse(new JsonValue(true)).asPromise();
                    } else {
                        throw new ServiceUnavailableException();
                    }
                case eval:
                    if (scriptEntry.isActive()) {
                        Script script = scriptEntry.getScript(context);
                        return newResultPromise(newActionResponse(
                                new JsonValue(script.eval(new SimpleBindings(bindings)))));
                    } else {
                        throw new ServiceUnavailableException();
                    }
                default:
                    throw new BadRequestException("Unrecognized action ID " + request.getAction());
            }
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (ScriptCompilationException e) {
            return new BadRequestException(e.getMessage(), e).asPromise();
        } catch (IllegalArgumentException e) { // from getActionAsEnum
            return new BadRequestException(e.getMessage(), e).asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    public Promise<QueryResponse, ResourceException> handleQuery(final Context context, final QueryRequest request,
            final QueryResourceHandler handler) {
        final ResourceException e = new NotSupportedException("Query operations are not supported");
        return e.asPromise();
    }

    public Promise<ResourceResponse, ResourceException> handleRead(final Context context, final ReadRequest request) {
        final ResourceException e = new NotSupportedException("Read operations are not supported");
        return e.asPromise();
    }

    public Promise<ResourceResponse, ResourceException> handleCreate(final Context context, final CreateRequest request) {
        final ResourceException e = new NotSupportedException("Create operations are not supported");
        return e.asPromise();
    }

    public Promise<ResourceResponse, ResourceException> handleDelete(final Context context, final DeleteRequest request) {
        final ResourceException e = new NotSupportedException("Delete operations are not supported");
        return e.asPromise();
    }

    public Promise<ResourceResponse, ResourceException> handlePatch(final Context context, final PatchRequest request) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        return e.asPromise();
    }

    public Promise<ResourceResponse, ResourceException> handleUpdate(final Context context, final UpdateRequest request) {
        final ResourceException e = new NotSupportedException("Update operations are not supported");
        return e.asPromise();
    }
    
    @Override
    public void execute(Context context, Map<String, Object> scheduledContext) throws ExecutionException {
        
        try {
            String scriptName = (String) scheduledContext.get(CONFIG_NAME);
            JsonValue params = new JsonValue(scheduledContext).get(CONFIGURED_INVOKE_CONTEXT);
            JsonValue scriptValue = params.get("script").expect(Map.class).clone();
            
            if (scriptValue.get(SourceUnit.ATTR_NAME).isNull()) {
                if (!scriptValue.get(SOURCE_FILE).isNull()) {
                    scriptValue.put(SourceUnit.ATTR_NAME, scriptValue.get(SOURCE_FILE).getObject());
                }
            }
            
            if (!scriptValue.isNull()) {
                ScriptEntry entry = takeScript(scriptValue);
                JsonValue input = params.get("input");
                execScript(context, entry, input);
            } else {
                throw new ExecutionException("No valid script '" + scriptName + "' configured in schedule.");
            }
        } catch (JsonValueException jve) {
            throw new ExecutionException(jve);
        } catch (ScriptException e) {
            throw new ExecutionException(e);
        } catch (ResourceException e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public void auditScheduledService(final Context context, final AuditEvent auditEvent)
            throws ExecutionException {
        try {
            if (connectionFactory != null) {
                connectionFactory.getConnection().create(
                        context, Requests.newCreateRequest("audit/access", auditEvent.getValue()));
            }
        } catch (ResourceException e) {
            logger.error("Unable to audit scheduled service {}", auditEvent.toString());
            throw new ExecutionException("Unable to audit scheduled service", e);
        }
    }

    private void execScript(Context context, ScriptEntry script, JsonValue value)
            throws ForbiddenException, InternalServerErrorException {
        if (null != script && script.isActive()) {
            Script executable = script.getScript(context);
            executable.put("object", value.getObject());
            try {
                executable.eval(); // allows direct modification to the object
            } catch (ScriptThrownException ste) {
                throw new ForbiddenException(ste.getValue().toString());
            } catch (ScriptException se) {
                throw new InternalServerErrorException("script encountered exception", se);
            }
        }
    }
}

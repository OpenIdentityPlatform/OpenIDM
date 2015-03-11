/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openidm.provisioner.openicf.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.metadata.MetaDataProvider;
import org.forgerock.openidm.metadata.MetaDataProviderCallback;
import org.forgerock.openidm.metadata.WaitForMetaData;
import org.forgerock.openidm.provisioner.ConnectorConfigurationHelper;
import org.forgerock.openidm.provisioner.openicf.ConnectorInfoProvider;
import org.forgerock.openidm.provisioner.openicf.ConnectorReference;
import org.forgerock.openidm.provisioner.openicf.commons.ConnectorUtil;
import org.forgerock.openidm.provisioner.openicf.internal.ConnectorFacadeCallback;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.Pair;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.Version;
import org.identityconnectors.common.VersionRange;
import org.identityconnectors.common.event.ConnectorEvent;
import org.identityconnectors.common.event.ConnectorEventHandler;
import org.identityconnectors.common.event.ConnectorEventPublisher;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConfigurationProperty;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.identityconnectors.framework.api.ConnectorKey;
import org.identityconnectors.framework.api.RemoteFrameworkConnectionInfo;
import org.identityconnectors.framework.api.operations.SchemaApiOp;
import org.identityconnectors.framework.api.operations.TestApiOp;
import org.identityconnectors.framework.common.FrameworkUtil;
import org.identityconnectors.framework.impl.api.APIConfigurationImpl;
import org.identityconnectors.framework.impl.api.AbstractConnectorInfo;
import org.identityconnectors.framework.impl.api.ConnectorInfoManagerFactoryImpl;
import org.identityconnectors.framework.impl.api.remote.RemoteConnectorInfoImpl;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ConnectorInfoProviderService initiates the the embedded <a
 * href="http://openicf.forgerock.org">OpenICF</a> and makes it available as a
 * service.
 * <p/>
 *
 */
@Component(name = ConnectorInfoProviderService.PID,
        policy = ConfigurationPolicy.OPTIONAL,
        metatype = true,
        description = "OpenICF Connector Info Service",
        immediate = true)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenICF Connector Info Service") })
@References({
    @Reference(name = "osgiConnectorEventPublisher",
            referenceInterface = ConnectorEventPublisher.class,
            bind = "bindConnectorEventPublisher",
            unbind = "unbindConnectorEventPublisher",
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy = ReferencePolicy.DYNAMIC) })
public class ConnectorInfoProviderService implements ConnectorInfoProvider, MetaDataProvider, ConnectorConfigurationHelper {
    /**
     * Setup logging for the {@link ConnectorInfoProviderService}.
     */
    private final static Logger logger = LoggerFactory.getLogger(ConnectorInfoProviderService.class);

    // Public Constants
    public static final String DEFAULT_CONNECTORS_LOCATION = "connectors";
    public static final String PROPERTY_OPENICF_CONNECTOR_URL = "connectorsLocation";
    public static final String PID = "org.forgerock.openidm.provisioner.openicf.connectorinfoprovider";
    private static final String SYSTEM_TYPE_OPENICF = "openicf";

    // Private
    private Map<String, Pair<RemoteFrameworkConnectionInfo, ConnectorEventHandler>> remoteFrameworkConnectionInfo =
            new HashMap<String, Pair<RemoteFrameworkConnectionInfo, ConnectorEventHandler>>();
    private ScheduledExecutorService scheduledExecutorService = null;
    private List<URL> connectorURLs = null;
    private ClassLoader bundleParentClassLoader = null;
    private final MetaDataProviderCallback[] callback = new MetaDataProviderCallback[1];

    private ConcurrentMap<ConnectorReference, Set<ConnectorFacadeCallback>> connectorEventHandler =
            new ConcurrentHashMap<ConnectorReference, Set<ConnectorFacadeCallback>>();
    private ConnectorEventHandler osgiConnectorEventHandler = null;

    private class ConnectorEventHandlerImpl implements ConnectorEventHandler {

        private ConnectorReference.ConnectorLocation connectorLocation;
        private String connectorHost;

        private ConnectorEventHandlerImpl() {
            connectorLocation = ConnectorReference.ConnectorLocation.OSGI;
            connectorHost = null;
        }

        private ConnectorEventHandlerImpl(String connectorHost) {
            connectorLocation = ConnectorReference.ConnectorLocation.REMOTE;
            this.connectorHost = connectorHost;
        }

        public void handleEvent(ConnectorEvent connectorEvent) {
            if (null == connectorEvent)
                return;
            logger.trace("ConnectorEvent received. Topic: {}, Source: {}", connectorEvent.getTopic(),
                    connectorEvent.getSource());

            MetaDataProviderCallback cb = callback[0];
            if (null != cb && ConnectorEvent.CONNECTOR_REGISTERED.equals(connectorEvent.getTopic())) {
                callback[0].refresh();
            }

            Object source = connectorEvent.getSource();
            if (source instanceof ConnectorKey) {
                synchronized (this) {
                    for (Map.Entry<ConnectorReference, Set<ConnectorFacadeCallback>> entry : connectorEventHandler
                            .entrySet()) {
                        if (entry.getKey().getConnectorLocation().equals(connectorLocation)
                                && (connectorLocation
                                        .equals(ConnectorReference.ConnectorLocation.OSGI) ^ entry
                                        .getKey().getConnectorHost().equals(connectorHost))
                                && ((ConnectorKey) source)
                                        .equals(getConnectorInfo(entry.getKey(), getConnectorInfoManager(entry.getKey()))
                                        .getConnectorKey())) {
                            // TODO What to do if it's null? Throw NPE now
                            ConnectorInfo ci = findConnectorInfo(entry.getKey());

                            for (ConnectorFacadeCallback listener : entry.getValue()) {
                                try {
                                    if (ConnectorEvent.CONNECTOR_REGISTERED.equals(connectorEvent
                                            .getTopic())) {
                                        if (connectorLocation
                                                .equals(ConnectorReference.ConnectorLocation.OSGI)) {
                                            listener.addingConnectorInfo(ci,
                                                    connectorFacadeFactory);
                                        } else {
                                            listener.addingConnectorInfo(ci, ConnectorFacadeFactory
                                                    .getInstance());
                                        }
                                    } else {
                                        listener.removedConnectorInfo(ci);
                                    }
                                } catch (Throwable t) {
                                    /* ignore */
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /*
     * If this newBuilder was instantiated for MetaDataProvider by
     * Class#newInstance then this is false. If this newBuilder was activated by
     * OSGi SCR then this is true.
     */
    private boolean isOSGiServiceInstance = false;

    /**
     * ConnectorInfoManager service.
     */
    @Reference(referenceInterface = ConnectorInfoManager.class,
            cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.STATIC)
    private ConnectorInfoManager connectorInfoManager = null;

    /**
     * ConnectorFacadeFactory service.
     */
    @Reference(referenceInterface = ConnectorFacadeFactory.class,
            cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.STATIC)
    private ConnectorFacadeFactory connectorFacadeFactory = null;

    /**
     * Cryptographic service.
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected CryptoService cryptoService = null;

    /**
     * ConnectorEventPublisher service.
     */
    public synchronized void bindConnectorEventPublisher(ConnectorEventPublisher service) {
        if (null == osgiConnectorEventHandler) {
            osgiConnectorEventHandler = new ConnectorEventHandlerImpl();
        }
        service.addConnectorEventHandler(osgiConnectorEventHandler);
    }

    public void unbindConnectorEventPublisher(ConnectorEventPublisher service) {
        service.deleteConnectorEventHandler(osgiConnectorEventHandler);
    }

    @Activate
    public void activate(ComponentContext context) {
        logger.trace("Activating Service with configuration {}", context.getProperties());
        JsonValue configuration = JSONEnhancedConfig.newInstance().getConfigurationAsJson(context);

        try {
            // String connectorLocation = DEFAULT_CONNECTORS_LOCATION;
            String connectorLocation =
                    configuration.get(PROPERTY_OPENICF_CONNECTOR_URL).defaultTo(
                            DEFAULT_CONNECTORS_LOCATION).asString();
            // Initialise Local ConnectorInfoManager
            initialiseLocalManager(connectorLocation);
        } catch (JsonValueException e) {
            logger.error("Invalid configuration {}", configuration.getObject(), e);
            throw new ComponentException("Invalid configuration, service can not be started", e);
        }

        JsonValue remoteConnectorHosts = null;
        try {
            remoteConnectorHosts =
                    configuration.get(ConnectorUtil.OPENICF_REMOTE_CONNECTOR_SERVERS).expect(
                            List.class);
            if (!remoteConnectorHosts.isNull()) {
                initialiseRemoteManager(remoteConnectorHosts);
            }
        } catch (JsonValueException e) {
            logger.error("Invalid configuration remoteConnectorHosts must be list or null. {}",
                    remoteConnectorHosts, e);
            throw new ComponentException("Invalid configuration, service can not be started", e);
        }
        isOSGiServiceInstance = true;
        logger.info("ConnectorInfoProviderService with OpenICF {} is activated.", FrameworkUtil
                .getFrameworkVersion());
    }

    protected void initialiseRemoteManager(JsonValue remoteConnectorHosts)
            throws JsonValueException {
        for (JsonValue info : remoteConnectorHosts) {
            try {
                RemoteFrameworkConnectionInfo rfi =
                        ConnectorUtil.getRemoteFrameworkConnectionInfo(info.expect(Map.class));
                String name = info.get("name").required().asString();
                if (StringUtil.isNotBlank(name)) {
                    Pair<RemoteFrameworkConnectionInfo, ConnectorEventHandler> pair =
                            new Pair<RemoteFrameworkConnectionInfo, ConnectorEventHandler>(rfi,
                                    null);
                    remoteFrameworkConnectionInfo.put(name, pair);
                    ConnectorInfoManager connectorInfoManager =
                            ConnectorInfoManagerFactory.getInstance()
                                    .getUnCheckedRemoteManager(rfi);
                    if (connectorInfoManager instanceof Runnable
                            && connectorInfoManager instanceof ConnectorEventPublisher) {
                        if (null == scheduledExecutorService) {
                            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                        }
                        pair.second = new ConnectorEventHandlerImpl(name);
                        ((ConnectorEventPublisher) connectorInfoManager)
                                .addConnectorEventHandler(pair.second);
                        /*
                         * Specifies the interval, in seconds, at which
                         * heartbeat packets are transmitted.
                         */
                        JsonValue heartbeatInterval =
                                info.get("heartbeatInterval").defaultTo(60l).expect(Number.class);
                        /*
                         * Specifies the number of missed heartbeat intervals
                         * after which a broker is considered suspect of
                         * failure. JsonValue heartbeatThreshold =
                         * remoteConnectorHosts
                         * .get("heartbeatThreshold").defaultTo
                         * (1).expect(Number.class);
                         */
                        scheduledExecutorService.scheduleWithFixedDelay(
                                (Runnable) connectorInfoManager,
                                0,
                                heartbeatInterval.asLong(),
                                TimeUnit.SECONDS);
                    }
                } else {
                    logger.error("RemoteFrameworkConnectionInfo has no name");
                }
            } catch (IllegalArgumentException e) {
                logger.error("RemoteFrameworkConnectionInfo can not be read", e);
            }
        }
    }

    protected void initialiseLocalManager(String connectorsArea) {
        try {
            String connectorsDir = URLDecoder.decode(connectorsArea, "UTF-8");
            logger.debug("Using connectors from [{}]", connectorsDir);
            File dir = IdentityServer.getFileForInstallPath(connectorsDir);
            // This is a fix to support absolute path on OSX
            if (!dir.exists()) {
                String absolutePath = dir.getAbsolutePath();
                if (!absolutePath.endsWith(File.separator)) {
                    dir = new File(absolutePath.concat(File.separator));
                }
            }

            if (!dir.exists()) {
                String absolutePath = dir.getAbsolutePath();
                logger.error("Configuration area [{}] does not exist. Unable to load connectors.",
                        absolutePath);
            } else {
                try {
                    logger.debug("Looking for connectors in {} directory.", dir.getAbsoluteFile()
                            .toURI().toURL());
                    // Create a single newBuilder of ConnectorInfoManagerFactory
                    ((ConnectorInfoManagerFactoryImpl) ConnectorInfoManagerFactory.getInstance())
                            .getLocalManager(
                                    getConnectorURLs(dir.getAbsoluteFile().toURI().toURL()),
                                    getBundleParentClassLoader());
                } catch (MalformedURLException e) {
                    logger.error("How can this happen?", e);
                }

            }
        } catch (UnsupportedEncodingException e) {
            // Should never happen.
            throw new UndeclaredThrowableException(e);
        } catch (Throwable t) {
            logger.error("LocalManager initialisation for {} failed.", connectorsArea, t);
            throw new ComponentException("LocalManager initialisation failed.", t);
        }
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        logger.trace("Deactivating Component: {}", context.getProperties().get(
                ComponentConstants.COMPONENT_NAME));
        if (null != scheduledExecutorService) {
            scheduledExecutorService.shutdown();
            scheduledExecutorService = null;
        }
        connectorEventHandler.clear();
        ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();

        for (Pair<RemoteFrameworkConnectionInfo, ConnectorEventHandler> pair : remoteFrameworkConnectionInfo
                .values()) {
            if (null != pair.first && null != pair.second) {
                try {
                    ConnectorInfoManager manager = factory.getUnCheckedRemoteManager(pair.first);
                    ((ConnectorEventPublisher) manager).deleteConnectorEventHandler(pair.second);
                } catch (Exception e) {
                    /* ignore */
                }
            }
        }
        remoteFrameworkConnectionInfo.clear();
        factory.clearRemoteCache();
        connectorURLs = null;
        bundleParentClassLoader = null;
        factory.clearLocalCache();
        logger.info("ConnectorInfoProviderService is deactivated.");
    }

    // @Modified
    // protected void update(ComponentContext context) {
    // }

    // ----- Implementation of ConnectorConfigurationHelper interface

    /**
     * {@inheritDoc}
     */
    public String getProvisionerType() {
        return SYSTEM_TYPE_OPENICF;
    }

    /**
     * Validates that the connectorRef is defined in the connector configuration
     * @param params connector configuration
     * @return true if connectorRef is not null and configurationProperties is null; false otherwise
     */
    private boolean isGenerateConnectorCoreConfig(JsonValue params) {
        return !params.get(ConnectorUtil.OPENICF_CONNECTOR_REF).isNull()
                && params.get(ConnectorUtil.OPENICF_CONFIGURATION_PROPERTIES).isNull();
    }

    /**
     * Validates that connectorRef and configurationProperties
     * inside the connector configuration are both not null
     * @param params connector configuration
     * @return true if both connectorRef and configurationProperties are not null; false otherwise
     */
    private boolean isGenerateFullConfig(JsonValue params) {
        return !params.get(ConnectorUtil.OPENICF_CONNECTOR_REF).isNull()
                && !params.get(ConnectorUtil.OPENICF_CONFIGURATION_PROPERTIES).isNull();
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue getAvailableConnectors() throws ResourceException {
        try {
            return json(object(field(ConnectorUtil.OPENICF_CONNECTOR_REF, listAllConnectorInfo())));
        } catch (JsonValueException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue generateConnectorCoreConfig(JsonValue params) throws ResourceException {
        if (!isGenerateConnectorCoreConfig(params)) {
            return new JsonValue(new HashMap<String, Object>());
        }
        try {
            ConnectorReference ref = ConnectorUtil.getConnectorReference(params);
            ConnectorInfo info = findConnectorInfo(ref);
            if (null == info) {
                throw new NotFoundException("Connector not found: " + ref.getConnectorKey());
            }
            return ConnectorUtil.createSystemConfigurationFromAPIConfiguration(info
                    .createDefaultAPIConfiguration(), params.copy(), cryptoService);
        } catch (JsonCryptoException e) {
            throw new InternalServerErrorException(e);
        } catch (JsonValueException e) {
            throw new BadRequestException(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue generateConnectorFullConfig(JsonValue params) throws ResourceException {
        if (!isGenerateFullConfig(params)) {
            return new JsonValue(new HashMap<String, Object>());
        }
        try {
            ConnectorReference ref = ConnectorUtil.getConnectorReference(params);
            ConnectorInfo info = findConnectorInfo(ref);
            if (null == info) {
                throw new NotFoundException("Connector not found: " + ref.getConnectorKey());
            }
            APIConfiguration configuration = info.createDefaultAPIConfiguration();
            ConnectorUtil.configureDefaultAPIConfiguration(params, configuration, cryptoService);
            return new JsonValue(createSystemConfiguration(configuration, false));
        } catch (JsonValueException e) {
            throw new BadRequestException(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> test(JsonValue params) throws ResourceException {
        JsonValue jv = json(object());

        jv.put("ok", false);
        jv.put("name", params.get("name").required().asString());
        params.get(ConnectorUtil.OPENICF_CONNECTOR_REF).required();
        params.get(ConnectorUtil.OPENICF_CONFIGURATION_PROPERTIES).required();

        ConnectorReference connectorReference = null;
        try {
            connectorReference = ConnectorUtil.getConnectorReference(params);
        } catch (JsonValueException e) {
            jv.put("error", "OpenICF Provisioner Service jsonConfiguration has errors: " + e.getMessage());
            return jv.asMap();
        }

        ConnectorInfo connectorInfo = findConnectorInfo(connectorReference);
        if (null != connectorInfo) {
            APIConfiguration configuration = connectorInfo.createDefaultAPIConfiguration();
            ConnectorUtil.configureDefaultAPIConfiguration(params, configuration, cryptoService);
            ConnectorFacadeFactory connectorFacadeFactory = ConnectorFacadeFactory.getInstance();
            ConnectorFacade facade = connectorFacadeFactory.newInstance(configuration);
            if (null == facade && null != connectorInfoManager) {
                try {
                    facade = this.connectorFacadeFactory.newInstance(configuration);
                } catch (Exception e) {
                    logger.warn("OSGi ConnectorManager can not create ConnectorFacade", e);
                    jv.put("error", "OpenICF connector jsonConfiguration has errors: " + e.getMessage());
                    return jv.asMap();
                }
            }

            if (null == facade) {
                jv.put("error", "OpenICF ConnectorFacade of " + connectorReference + " is not available");
            } else if (facade.getSupportedOperations().contains(TestApiOp.class)) {
                try {
                    facade.test();
                    jv.put("ok", true);
                } catch (UnsupportedOperationException e) {
                    jv.put("reason", "TEST UnsupportedOperation");
                    jv.put("ok", true);
                } catch (Throwable e) {
                    jv.put("error", e.toString());
                    // exception -- leave "ok" : false
                }
            } else {
                jv.put("reason", "OpenICF connector of " + connectorReference + " does not support test.");
                jv.put("ok", true);
            }
        } else if (connectorReference.getConnectorLocation().equals(ConnectorReference.ConnectorLocation.LOCAL)) {
            jv.put("error", "OpenICF ConnectorInfo can not be loaded for " + connectorReference + " from #LOCAL");
        } else {
            jv.put("error", "OpenICF ConnectorInfo for " + connectorReference + " is not available yet.");
        }
        return jv.asMap();
    }

    // ----- Implementation of ConnectorInfoProvider interface

    /**
     * {@inheritDoc}
     */
    public ConnectorInfo findConnectorInfo(ConnectorReference connectorReference) {
        return getConnectorInfo(connectorReference, getConnectorInfoManager(connectorReference));
    }

    private ConnectorInfoManager getConnectorInfoManager(ConnectorReference connectorReference) {
        ConnectorInfoManager connectorInfoManager = null;
        switch (connectorReference.getConnectorLocation()) {
        case LOCAL:
            connectorInfoManager =
                    ((ConnectorInfoManagerFactoryImpl) ConnectorInfoManagerFactory.getInstance())
                            .getLocalManager(getConnectorURLs(), getBundleParentClassLoader());
            break;
        case OSGI:
            connectorInfoManager = this.connectorInfoManager;
            break;
        case REMOTE:
            Pair<RemoteFrameworkConnectionInfo, ConnectorEventHandler> rfci =
                    remoteFrameworkConnectionInfo.get(connectorReference.getConnectorHost());
            if (null != rfci) {
                connectorInfoManager =
                        ConnectorInfoManagerFactory.getInstance().getUnCheckedRemoteManager(
                                rfci.first);
            }
        }
        return connectorInfoManager;
    }

    private ConnectorInfo getConnectorInfo(ConnectorReference connectorReference, ConnectorInfoManager connectorInfoManager) {
        ConnectorInfo connectorInfo = null;
        if (null != connectorInfoManager) {
            try {
                // Check if the version is an interval.
                if (connectorReference.getConnectorKey().getBundleVersion().indexOf(',') > 1) {
                    VersionRange range =
                            VersionRange.parse(connectorReference.getConnectorKey()
                                    .getBundleVersion());
                    Version latest = null;
                    for (ConnectorInfo ci : connectorInfoManager.getConnectorInfos()) {
                        if (connectorReference.getConnectorKey().getBundleName().equals(
                                ci.getConnectorKey().getBundleName())
                                && connectorReference.getConnectorKey().getConnectorName().equals(
                                        ci.getConnectorKey().getConnectorName())) {
                            // Check id the version is in interval.
                            Version current =
                                    Version.parse(ci.getConnectorKey().getBundleVersion());
                            if (range.isInRange(current)) {
                                logger.trace("Version {} is in range of {}", ci.getConnectorKey()
                                        .getBundleVersion(), connectorReference.getConnectorKey()
                                        .getBundleVersion());
                                if (null == latest || current.compareTo(latest) > 0) {
                                    connectorInfo = ci;
                                    latest = current;
                                }
                            }
                        }
                    }
                } else {
                    connectorInfo =
                            connectorInfoManager.findConnectorInfo(connectorReference
                                    .getConnectorKey());
                }
            } catch (Exception e) {
                logger.error("Can not find ConnectorInfo for {}", connectorReference, e);
            }
        }
        return connectorInfo;
    }

    /**
     * {@inheritDoc}
     */
    public void addConnectorFacadeCallback(ConnectorReference connectorReference,
            ConnectorFacadeCallback handler) {
        /*
         * This implementation is not safe. An other thread can register or
         * remove ConnectorInfo meanwhile findConnectorInfo and putIfAbsent.
         * This will be fixed with
         * ConnectorFacadeFactory#getConnectorFacadeAsync()
         */
        if (null != connectorReference && null != handler) {
            CopyOnWriteArraySet<ConnectorFacadeCallback> tmp =
                    new CopyOnWriteArraySet<ConnectorFacadeCallback>();
            ConnectorInfo ci = findConnectorInfo(connectorReference);
            Set<ConnectorFacadeCallback> listeners =
                    connectorEventHandler.putIfAbsent(connectorReference, tmp);

            if (null == listeners) {
                listeners = tmp;
            }
            listeners.add(handler);

            if (null != ci) {
                if (connectorReference.equals(ConnectorReference.ConnectorLocation.OSGI)) {
                    handler.addingConnectorInfo(ci, connectorFacadeFactory);
                } else {
                    handler.addingConnectorInfo(ci, ConnectorFacadeFactory.getInstance());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void deleteConnectorFacadeCallback(ConnectorFacadeCallback handler) {
        if (null != handler) {
            for (Set<ConnectorFacadeCallback> listeners : connectorEventHandler.values()) {
                listeners.remove(handler);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<ConnectorInfo> getAllConnectorInfo() {
        ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();
        ConnectorInfoManager connectorInfoManager =
                ((ConnectorInfoManagerFactoryImpl) factory).getLocalManager(getConnectorURLs(),
                        getBundleParentClassLoader());

        List<ConnectorInfo> result =
                new ArrayList<ConnectorInfo>(connectorInfoManager.getConnectorInfos());

        if (null != this.connectorInfoManager) {
            result.addAll(this.connectorInfoManager.getConnectorInfos());
        }
        for (Pair<RemoteFrameworkConnectionInfo, ConnectorEventHandler> entry : remoteFrameworkConnectionInfo
                .values()) {
            try {
                ConnectorInfoManager remoteConnectorInfoManager =
                        factory.getUnCheckedRemoteManager(entry.first);
                result.addAll(remoteConnectorInfoManager.getConnectorInfos());
            } catch (Exception e) {
                logger.error("Remote Connector Server is not available for {}", entry, e);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private List<Map<String, Object>> listAllConnectorInfo() {
        ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();
        ConnectorInfoManager connectorInfoManager =
                ((ConnectorInfoManagerFactoryImpl) factory).getLocalManager(getConnectorURLs(),
                        getBundleParentClassLoader());

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (ConnectorInfo info : connectorInfoManager.getConnectorInfos()) {
            Map<String, Object> connectorReference =
                    ConnectorUtil.getConnectorKey(info.getConnectorKey());
            connectorReference.put("displayName", info.getConnectorDisplayName());
            result.add(connectorReference);
        }

        if (null != this.connectorInfoManager) {
            for (ConnectorInfo info : this.connectorInfoManager.getConnectorInfos()) {
                Map<String, Object> connectorReference =
                        ConnectorUtil.getConnectorKey(info.getConnectorKey());
                connectorReference.put("displayName", info.getConnectorDisplayName());
                connectorReference.put(ConnectorUtil.OPENICF_CONNECTOR_HOST_REF,
                        ConnectorReference.OSGI_SERVICE_CONNECTOR_MANAGER);
                result.add(connectorReference);
            }
        }

        for (Map.Entry<String, Pair<RemoteFrameworkConnectionInfo, ConnectorEventHandler>> entry : remoteFrameworkConnectionInfo
                .entrySet()) {
            try {
                ConnectorInfoManager remoteConnectorInfoManager =
                        factory.getUnCheckedRemoteManager(entry.getValue().first);
                for (ConnectorInfo info : remoteConnectorInfoManager.getConnectorInfos()) {
                    Map<String, Object> connectorReference =
                            ConnectorUtil.getConnectorKey(info.getConnectorKey());
                    connectorReference.put("displayName", info.getConnectorDisplayName());
                    connectorReference
                            .put(ConnectorUtil.OPENICF_CONNECTOR_HOST_REF, entry.getKey());
                    result.add(connectorReference);
                }
            } catch (Exception e) {
                logger.error("Remote Connector Server is not available for {}", entry, e);
            }

        }
        return Collections.unmodifiableList(result);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.identityconnectors.framework.common.exceptions.ConnectorException
     *             if OpenICF failed to create new connector facade
     */
    public void testConnector(APIConfiguration configuration) throws ResourceException {
        try {
            ConnectorFacadeFactory connectorFacadeFactory = ConnectorFacadeFactory.getInstance();
            ConnectorFacade facade = connectorFacadeFactory.newInstance(configuration);
            if (null == facade && null != connectorInfoManager) {
                try {
                    facade = this.connectorFacadeFactory.newInstance(configuration);
                } catch (Exception e) {
                    logger.warn("OSGi ConnectorManager can not create ConnectorFacade", e);
                }
            }
            if (null != facade) {
                TestApiOp operation = (TestApiOp) facade.getOperation(TestApiOp.class);
                if (null != operation) {
                    operation.test();
                } else {
                    return;
                }
            }
        } catch (Throwable t) {
            // TODO Use the utility to adopt the exception
        }
        throw new ServiceUnavailableException("ConnectorFacade can not be initialised");
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue createSystemConfiguration(APIConfiguration configuration, boolean validate) throws ResourceException {
        ConnectorFacadeFactory connectorFacadeFactory = ConnectorFacadeFactory.getInstance();
        ConnectorFacade facade = connectorFacadeFactory.newInstance(configuration);
        if (null == facade && null != connectorInfoManager) {
            try {
                facade = this.connectorFacadeFactory.newInstance(configuration);
            } catch (Exception e) {
                logger.warn("OSGi ConnectorManager can not create ConnectorFacade", e);
            }
        }
        if (null != facade) {
            JsonValue jsonConfiguration = new JsonValue(new LinkedHashMap<String, Object>());
            APIConfigurationImpl impl = (APIConfigurationImpl) configuration;
            AbstractConnectorInfo connectorInfo = impl.getConnectorInfo();
            ConnectorReference connectorReference = null;
            if (connectorInfo instanceof RemoteConnectorInfoImpl) {
                RemoteConnectorInfoImpl remoteInfo = (RemoteConnectorInfoImpl) connectorInfo;
                for (Map.Entry<String, Pair<RemoteFrameworkConnectionInfo, ConnectorEventHandler>> entry : remoteFrameworkConnectionInfo
                        .entrySet()) {
                    if (entry.getValue().first.equals(remoteInfo.getRemoteConnectionInfo())) {
                        connectorReference =
                                new ConnectorReference(connectorInfo.getConnectorKey(), entry
                                        .getKey());
                        break;
                    }
                }

            } else {
                connectorReference = new ConnectorReference(connectorInfo.getConnectorKey());
            }
            ConnectorUtil.setConnectorReference(connectorReference, jsonConfiguration);
            try {
                ConnectorUtil.createSystemConfigurationFromAPIConfiguration(configuration,
                        jsonConfiguration, cryptoService);
            } catch (JsonCryptoException e) {
                logger.debug("Error decrypting configuration", e);
                throw new InternalServerErrorException(e);
            }

            if (validate && facade.getSupportedOperations().contains(TestApiOp.class)) {
                facade.test();
            }
            if (facade.getSupportedOperations().contains(SchemaApiOp.class)) {
                ConnectorUtil
                        .setObjectAndOperationConfiguration(facade.schema(), jsonConfiguration);
            }
            return jsonConfiguration;
        }
        throw new UnsupportedOperationException("ConnectorFacade can not be initialised");
    }

    /**
     * {@inheritDoc}
     */
    public List<JsonPointer> getPropertiesToEncrypt(String pidOrFactory, String instanceAlias,
            JsonValue config) throws WaitForMetaData {
        List<JsonPointer> result = null;
        if (null != pidOrFactory && null != config) {
            if (PID.equals(pidOrFactory)) {
                try {
                    JsonValue remoteConnectorHosts =
                            config.get(ConnectorUtil.OPENICF_REMOTE_CONNECTOR_SERVERS).expect(
                                    List.class);
                    if (!remoteConnectorHosts.isNull()) {
                        result = new ArrayList<JsonPointer>(remoteConnectorHosts.size());
                        for (JsonValue hostConfig : remoteConnectorHosts) {
                            result.add(hostConfig.get(ConnectorUtil.OPENICF_KEY).getPointer());
                        }
                    }
                } catch (JsonValueException e) {
                    logger.error(
                            "Invalid configuration remoteConnectorHosts must be list or null.", e);
                }
            } else if (OpenICFProvisionerService.PID.equals(pidOrFactory)) {
                if (isOSGiServiceInstance) {
                    ConfigurationProperties properties = null;
                    try {
                        ConnectorReference connectorReference =
                                ConnectorUtil.getConnectorReference(config);
                        ConnectorInfo ci = findConnectorInfo(connectorReference);
                        if (null == ci) {
                            throw new WaitForMetaData("ConnectorInfo is not available");
                        }
                        properties = ci.createDefaultAPIConfiguration().getConfigurationProperties();
                    } catch (RuntimeException e) {
                        logger.error("Failed to parse the config of {}-{}: {}", new Object[] {
                                pidOrFactory, instanceAlias, e.getMessage()}, e);
                        throw e;
                    }

                    if (null == properties) {
                        throw new WaitForMetaData(pidOrFactory);
                    }

                    JsonPointer configurationProperties =
                            new JsonPointer(ConnectorUtil.OPENICF_CONFIGURATION_PROPERTIES);
                    result = new ArrayList<JsonPointer>(properties.getPropertyNames().size());
                    for (String name : properties.getPropertyNames()) {
                        ConfigurationProperty property = properties.getProperty(name);
                        if (property.isConfidential()
                                || property.getType().equals(GuardedString.class)
                                || property.getType().equals(GuardedByteArray.class)) {
                            result.add(configurationProperties.child(name));
                        }
                    }
                } else {
                    throw new WaitForMetaData("Wait for the MetaDataProvider service newBuilder");
                }
            }
        }
        return result;
    }

    private ClassLoader getBundleParentClassLoader() {
        if (null == bundleParentClassLoader) {
            List<File> urlList = null;
            File classes = IdentityServer.getFileForInstallPath("classes/");
            if (classes.isDirectory()) {
                urlList = new ArrayList<File>();
                urlList.add(classes);
            } else {
                logger.trace("BundleParentClassLoader does not use classes from {}", classes
                        .getAbsolutePath());
            }

            File lib = IdentityServer.getFileForInstallPath("lib");
            if (lib.isDirectory()) {
                File[] files = lib.listFiles(new FileFilter() {
                    public boolean accept(File f) {
                        return (f.getName().endsWith(".jar"));
                    }
                });
                for (File jarFile : files) {
                    if (null == urlList) {
                        urlList = new ArrayList<File>(files.length);
                    }
                    urlList.add(jarFile);
                }
            } else {
                logger.trace("BundleParentClassLoader does not use lib from {}", lib
                        .getAbsolutePath());
            }

            if (null != urlList) {
                URL[] urls = new URL[urlList.size()];
                for (int i = 0; i < urls.length; i++) {
                    try {
                        urls[i] = urlList.get(i).toURI().toURL();
                        logger.trace("Add URL to bundle parent classloader: {}", urls[i]);
                    } catch (MalformedURLException e) {
                        if (logger.isDebugEnabled()) {
                            logger.error("Failed toURL on File: {}", urlList.get(i)
                                    .getAbsolutePath(), e);
                        }
                    }
                }
                bundleParentClassLoader =
                        new URLClassLoader(urls, ConnectorInfoManagerFactory.class.getClassLoader());
            } else {
                bundleParentClassLoader = ConnectorInfoManagerFactory.class.getClassLoader();
            }
        }
        return bundleParentClassLoader;
    }

    private List<URL> getConnectorURLs(URL... resourceURLs) {
        if (null == connectorURLs) {
            List<URL> _bundleURLs = new ArrayList<URL>();
            for (URL resourceURL : resourceURLs) {
                try {
                    Vector<URL> urls = null;
                    if ("file".equals(resourceURL.getProtocol())) {
                        File file = new File(resourceURL.toURI());
                        if (file.isDirectory()) {
                            FileFilter filter = new FileFilter() {
                                public boolean accept(File f) {
                                    File fManifest = new File(f.getPath(), "META-INF/MANIFEST.MF");
                                    return ((f.isDirectory()) && fManifest.isFile()) || (f.getName().endsWith(".jar"));
                                }
                            };
                            File[] files = file.listFiles(filter);
                            urls = new Vector<URL>(files.length);
                            for (File subFile : files) {
                                String fname = subFile.getName();
                                logger.trace("Load Connector Bundle: {}", fname);
                                urls.add(new URL(resourceURL, fname));
                            }
                        }
                    } else if (("jar".equals(resourceURL.getProtocol()))
                            || ("wsjar".equals(resourceURL.getProtocol()))) {
                        urls =
                                getJarFileListing(resourceURL, "^META-INF/"
                                        + DEFAULT_CONNECTORS_LOCATION + "/(.*).jar$");

                    } else {
                        logger.info(
                                "Local connector support disabled.  No support for bundle URLs with protocol {}",
                                resourceURL.getProtocol());
                    }
                    if ((urls == null) || (urls.size() == 0)) {
                        logger.info("No local connector bundles found within {}", resourceURL);
                    }
                    if (null != urls) {
                        _bundleURLs.addAll(urls);
                    }
                } catch (IOException ex) {
                    // TODO Add Message
                    logger.error("XXX", ex);
                } catch (URISyntaxException e) {
                    logger.error("URL newBuilder does not comply with RFC 2396", e);
                }
            }
            if (logger.isDebugEnabled()) {
                for (URL u : _bundleURLs) {
                    logger.debug("Connector URL: {}", u);
                }
            }
            connectorURLs = CollectionUtil.newReadOnlyList(_bundleURLs);
        }
        return connectorURLs;
    }

    /**
     * <p>
     * Retrieve a list of filepaths from a given directory within a jar file. If
     * filtered results are needed, you can supply a |filter| regular expression
     * which will match each entry.
     *
     * @param jarLocation
     * @param filter
     *            to filter the results within a regular expression.
     * @return a list of files within the jar |file|
     */
    private static Vector<URL> getJarFileListing(URL jarLocation, String filter) {
        Vector<URL> files = new Vector<URL>();
        if (jarLocation == null) {
            return files; // Empty.
        }

        // strip out the file: and the !META-INF/bundles so only the JAR file
        // left
        String jarPath = jarLocation.getPath().substring(5, jarLocation.getPath().indexOf("!"));

        try {
            // Lets stream the jar file
            JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarPath));
            JarEntry jarEntry;

            // Iterate the jar entries within that jar. Then make sure it
            // follows the
            // filter given from the user.
            do {
                jarEntry = jarInputStream.getNextJarEntry();
                if (jarEntry != null) {
                    String fileName = jarEntry.getName();

                    // The filter could be null or has a matching regular
                    // expression.
                    if (filter == null || fileName.matches(filter)) {
                        files.add(new URL(jarLocation, fileName.replace(
                                DEFAULT_CONNECTORS_LOCATION, "")));
                    }
                }
            } while (jarEntry != null);
            jarInputStream.close();
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to get Jar input stream from '" + jarLocation + "'",
                    ioe);
        }
        return files;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCallback(MetaDataProviderCallback callback) {
        this.callback[0] = callback;
    }
}

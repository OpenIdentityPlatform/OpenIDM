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

package org.forgerock.openidm.metadata.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.impl.DefaultPrettyPrinter;
import org.codehaus.jackson.impl.Indenter;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.PrettyPrinter;

import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.openidm.config.InternalErrorException;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.config.installer.JSONConfigInstaller;
import org.forgerock.openidm.config.installer.JSONPrettyPrint;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.metadata.MetaDataProvider;
import org.forgerock.openidm.osgi.ServiceTrackerNotifier;
import org.forgerock.openidm.osgi.ServiceTrackerListener;


import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keep track of meta data providers, either declared in bundle meta-data, or 
 * registered as services
 * 
 * @author aegloff
 *
 */
public class ProviderTracker implements ServiceTrackerListener {
    final static Logger logger = LoggerFactory.getLogger(ProviderTracker.class);
    
    static ServiceTracker providerTracker;
    ProviderListener providerListener;
    BundleContext context;
    ObjectMapper mapper = new ObjectMapper();
    
    // Map from bundle IDs to MetaDataProvider
    // Long type key are bundle identifiers
    // String keys are service pids
    Map<Object, MetaDataProvider> providers = new HashMap<Object, MetaDataProvider>();
    
    /**
     * Constructor
     * @param context a bundle context to access OSGi
     * @param listener the listener to notify when a provider change was detecte
     * @param notifyDuringInit whether to notify the listener during the ProviderTracker construction.
     */
    public ProviderTracker(BundleContext context, ProviderListener listener, boolean notifyDuringInit) {
        this.context = context;
        providerListener = listener;
        initBundleProviders(context, notifyDuringInit);
        providerTracker = initServiceTracker(context);
        // TODO: add bundle listeners to track new installs and remove uninstalls
    }
    
    private void initBundleProviders(BundleContext context, boolean notifyDuringInit) { 
        Bundle[] bundles = context.getBundles();
        for (Bundle bundle : bundles) {
            if (logger.isTraceEnabled()) {
                logger.trace("Scanning bundle {} for metadata file", bundle.getBundleId());
            }
            Enumeration<URL> entries = bundle.findEntries("org/forgerock/metadata", "bundle.json", true);
            try { 
                if (entries != null && entries.hasMoreElements()) {
                    URL entryUrl = entries.nextElement();
                    logger.trace("Found metadata file, load and parse {}", entryUrl);
                    InputStream in = entryUrl.openStream();
                    Map metaConfig = mapper.readValue(in, Map.class);
                    in.close();
                    String providerClazzName = (String) metaConfig.get("metaDataProvider");
                    logger.trace("Loading declared MetaDataProvider {}", providerClazzName);
                    if (providerClazzName == null) {
                        logger.trace("No MetaDataProvider class declared in meta data file {} for {}", entryUrl, bundle.getSymbolicName());
                    } else {
                        logger.trace("Loading declared MetaDataProvider {}", providerClazzName);
                        Class providerClazz = bundle.loadClass(providerClazzName);
                        MetaDataProvider provider = (MetaDataProvider) providerClazz.newInstance();
                        addProvider(Long.valueOf(bundle.getBundleId()), provider, notifyDuringInit);
                        logger.debug("Registered MetaDataProvider {} for {}", providerClazzName, bundle.getSymbolicName());
                    }
                }
            } catch (Exception ex) {
                logger.warn("Failed to obtain meta-data on handling configuration for {}", bundle.getSymbolicName(), ex);
            }
        }
    }
    
    private void addProvider(Object originId, MetaDataProvider provider, boolean notify) {
        providers.put(originId, provider);
        if (providerListener != null && notify) {
            logger.debug("Notifying listener of added MetaDataProvider {}", originId);
            providerListener.addedProvider(originId, provider);
        }
    }
    
    private ServiceTracker initServiceTracker(BundleContext context) {
        ServiceTracker tracker = new ServiceTrackerNotifier(context, MetaDataProvider.class.getName(), null, this);
        tracker.open();
        return tracker;
        
    }
    
    public void addedService(ServiceReference reference, Object service) {
        String pid = (String) reference.getProperty(Constants.SERVICE_PID);
        addProvider(pid, (MetaDataProvider) service, true);
    }

    public void removedService(ServiceReference reference, Object service) {
        String pid = (String) reference.getProperty(Constants.SERVICE_PID);
        providers.remove(pid);
    }
    
    public void modifiedService(ServiceReference reference, Object service) {
        String pid = (String) reference.getProperty(Constants.SERVICE_PID);
        addProvider(pid, (MetaDataProvider) service, true);
    }
    
    /**
     * Get the current registered providers
     * Providers can be backed by an OSGi services, 
     * or created from a bundle factory
     * @return meta data providers
     */
    public Collection<MetaDataProvider> getProviders() {
        return providers.values();
    }
}

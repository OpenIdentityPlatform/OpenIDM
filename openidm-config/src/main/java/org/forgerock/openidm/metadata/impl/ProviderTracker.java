/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS. All Rights Reserved
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
 *
 */
package org.forgerock.openidm.metadata.impl;

import static org.forgerock.json.JsonValue.json;

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.metadata.MetaDataProvider;
import org.forgerock.openidm.metadata.MetaDataProviderCallback;
import org.forgerock.openidm.osgi.ServiceTrackerListener;
import org.forgerock.openidm.osgi.ServiceTrackerNotifier;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Keep track of meta data providers, either declared in bundle meta-data, or registered as services.
 */
public class ProviderTracker implements ServiceTrackerListener<MetaDataProvider, MetaDataProvider> {
    final static Logger logger = LoggerFactory.getLogger(ProviderTracker.class);

    static ServiceTracker<MetaDataProvider, MetaDataProvider> providerTracker;
    ProviderListener providerListener;
    BundleContext context;
    ObjectMapper mapper = new ObjectMapper();

    // Map from origin identifiers to MetaDataProvider
    // Long type key are bundle identifiers
    // String keys are service pids
    // This map MUST be thread safe to avoid the java.util.ConcurrentModificationException
    Map<String, MetaDataProvider> providers = new ConcurrentSkipListMap<>();

    /**
     * Constructor
     * @param context a bundle context to access OSGi
     * @param listener the listener to notify when a provider change was detecte
     * @param notifyDuringInit whether to notify the listener during the ProviderTracker construction. 
     * Setting it to false allows to query the providers acquired during init with getProviders, and to process additional 
     * providers via the listener.
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
                    JsonValue metaConfig = json(mapper.readValue(in, Map.class));
                    in.close();
                    String providerClazzName = metaConfig.get("metaDataProvider").asString();
                    logger.trace("Loading declared MetaDataProvider {}", providerClazzName);
                    if (providerClazzName == null) {
                        logger.trace("No MetaDataProvider class declared in meta data file {} for {}",
                                entryUrl, bundle.getSymbolicName());
                    } else {
                        logger.trace("Loading declared MetaDataProvider {}", providerClazzName);
                        Class<?> providerClazz = bundle.loadClass(providerClazzName);
                        MetaDataProvider provider = (MetaDataProvider) providerClazz.newInstance();
                        String id = Long.valueOf(bundle.getBundleId()).toString();
                        // Instantiate and set the provider callback
                        provider.setCallback(new ProviderTrackerCallback(provider, id));
                        // Add the provider to the listener
                        addProvider(id, provider, notifyDuringInit);
                        logger.debug("Registered MetaDataProvider {} for {}",
                                providerClazzName, bundle.getSymbolicName());
                    }
                }
            } catch (Exception ex) {
                logger.warn("Failed to obtain meta-data on handling configuration for {}",
                        bundle.getSymbolicName(), ex);
            }
        }
    }

    private void addProvider(String originId, MetaDataProvider provider, boolean notify) {
        providers.put(originId, provider);
        if (providerListener != null && notify) {
            logger.debug("Notifying listener of added MetaDataProvider {}", originId);
            providerListener.addedProvider(originId, provider);
        }
    }

    private ServiceTracker<MetaDataProvider, MetaDataProvider> initServiceTracker(BundleContext context) {
        ServiceTracker<MetaDataProvider, MetaDataProvider> tracker =
                new ServiceTrackerNotifier<>(context, MetaDataProvider.class.getName(), null, this);
        tracker.open();
        return tracker;
    }

    public void addedService(ServiceReference<MetaDataProvider> reference, MetaDataProvider service) {
        String pid = (String) reference.getProperty(Constants.SERVICE_PID);
        // Instantiate and set the provider callback
        service.setCallback(new ProviderTrackerCallback(service, pid));
        // Add the provider to the listener
        addProvider(pid, service, true);
    }

    public void removedService(ServiceReference<MetaDataProvider> reference, MetaDataProvider service) {
        String pid = (String) reference.getProperty(Constants.SERVICE_PID);
        providers.remove(pid);
    }

    public void modifiedService(ServiceReference<MetaDataProvider> reference, MetaDataProvider service) {
        String pid = (String) reference.getProperty(Constants.SERVICE_PID);
        modifiedProvider(pid, service, true);
    }
    
    public void modifiedProvider(String pid, MetaDataProvider provider, boolean notify) {
        addProvider(pid, provider, notify);
    }

    /**
     * Get the current registered providers
     * Providers can be backed by an OSGi services, 
     * or created from a bundle factory
     * @return meta data providers
     */
    public Collection<MetaDataProvider> getProviders() {
        //The returned value MUST be thread safe!
        return providers.values();
    }

    /**
     * A MetaDataProviderCallback implementation that is used to notify the ProvicerListener
     * when a MetaDataProvider calls refresh()
     */
    private class ProviderTrackerCallback implements MetaDataProviderCallback {

        private MetaDataProvider provider = null;
        private String originId = null;

        /**
         * Constructor
         * @param provider the MetaDataProvider instance
         * @param originId the MetaDataProvider id
         */
        public ProviderTrackerCallback(MetaDataProvider provider, String originId) {
            this.provider = provider;
            this.originId = originId;
        }

        @Override
        public void refresh() {
            modifiedProvider(originId, provider, true);
        }

    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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

package org.forgerock.commons.launcher;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.startlevel.BundleStartLevel;

/**
 * An AbstractOSGiContainerService is a prototype of how to start an OSGi
 * {@link Framework}.
 * 
 * @author Laszlo Hordos
 */
public abstract class AbstractOSGiFrameworkService implements OSGiFramework {

    private final AtomicReference<Framework> framework = new AtomicReference<Framework>();

    private final AtomicBoolean started = new AtomicBoolean(Boolean.FALSE);

    private FrameworkListener frameworkListener = null;

    /**
     * @return list of startup bundle handlers.
     */
    protected abstract List<BundleHandler> listBundleHandlers(BundleContext context)
            throws MalformedURLException;

    /**
     * @return map of configuration properties.
     */
    protected abstract Map<String, String> getConfigurationProperties();

    protected abstract long getStopTimeout();

    protected abstract boolean isNewThread();

    protected abstract boolean isVerbose();

    protected abstract void init() throws Exception;

    protected abstract void registerServices(BundleContext bundleContext) throws Exception;

    public void start() throws Exception {
        // Create an instance of the framework.
        FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
        final Framework fmw = factory.newFramework(getConfigurationProperties());
        // Initialize the framework, but don't start it yet.
        fmw.init();
        // Use the system bundle context to process auto- install,
        // update, uninstall and start bundles.
        process(fmw.getBundleContext());
        registerServices(fmw.getBundleContext());

        Callable<Void> container = new Callable<Void>() {
            public Void call() throws Exception {
                FrameworkEvent event;
                do {
                    // Start the framework.
                    framework.set(fmw);
                    fmw.start();
                    // Wait for framework to stop to exit the VM.
                    event = fmw.waitForStop(0);
                }
                // If the framework was updated, then restart it.
                while (event.getType() == FrameworkEvent.STOPPED_UPDATE);
                return null;
            }
        };

        if (isNewThread()) {
            if (started.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
                Executors.newSingleThreadExecutor().submit(container);
            } else if (isVerbose()) {
                System.out.println("OSGi Framework has been started already!");
            }
        } else {
            container.call();
        }
    }

    public void stop() throws Exception {
        Framework fw = framework.getAndSet(null);
        if (null != fw) {
            // Stop the framework
            fw.stop();
            // Wait for the framework to stop completely
            FrameworkEvent event = fw.waitForStop(getStopTimeout());
            if (event.getType() == FrameworkEvent.WAIT_TIMEDOUT) {
                // TODO what?
                if (isVerbose()) {
                    System.err.println("OSGi Framework stop timed out!");
                }
                System.exit(1);
            }
            started.set(Boolean.FALSE);
        }
    }

    public void setFrameworkListener(FrameworkListener frameworkListener) {
        this.frameworkListener = frameworkListener;
    }

    public FrameworkListener getFrameworkListener() {
        return frameworkListener;
    }

    protected Framework getFramework() {
        return framework.get();
    }

    protected void process(BundleContext context) throws Exception {
        process(context, listBundleHandlers(context));
    }

    protected void process(BundleContext context, List<BundleHandler> bundleHandlers)
            throws Exception {
        // Get list of already installed bundles as a map.
        Map<String, Bundle> installedBundle = new HashMap<String, Bundle>();
        for (Bundle bundle : context.getBundles()) {
            installedBundle.put(bundle.getLocation(), bundle);
        }

        for (BundleHandler handler : bundleHandlers) {
            Bundle currentBundle = installedBundle.get(handler.getBundleUrl().toString());

            if (currentBundle == null
                    && ((handler.getActions().contains(BundleHandler.Action.update) || (handler
                            .getActions().contains(BundleHandler.Action.install))))) {
                currentBundle = context.installBundle(handler.getBundleUrl().toString());
                if (null != handler.getStartLevel()) {
                    currentBundle.adapt(BundleStartLevel.class).setStartLevel(
                            handler.getStartLevel());
                }

            } else if (handler.getActions().contains(BundleHandler.Action.update)) {
                currentBundle.update();
            } else if (handler.getActions().contains(BundleHandler.Action.uninstall)
                    && null != currentBundle) {
                currentBundle.uninstall();
                continue;
            }
            if (handler.getActions().contains(BundleHandler.Action.start) && null != currentBundle
                    && BundleHandler.isNotFragment(currentBundle)) {
                currentBundle.start();
            }
        }

        FrameworkListener listener = getFrameworkListener();
        if (null != listener) {
            context.addFrameworkListener(listener);
        }
    }
}

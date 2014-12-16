/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.forgerock.openidm.shell.felixgogo;

import org.apache.felix.service.command.CommandProcessor;
import org.forgerock.openidm.shell.CustomCommandScope;
import org.forgerock.openidm.shell.felixgogo.debug.DebugCommands;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Felix GoGo shell adapter activator.
 *
 * Based on Apache License 2.0 licensed osgilab org.knowhowlab.osgi.shell.felixgogo
 */
public class Activator implements BundleActivator {
    private static final Logger LOG = Logger.getLogger(Activator.class.getName());

    private static final String COMMANDS_DESCRIPTION_PROPERTY = "openidm.osgi.shell.commands";
    private static final String GROUP_ID_PROPERTY = "openidm.osgi.shell.group.id";

    /**
     * Felix GoGo shell API supports groups.
     * Filter requires shell commands description and group id
     */
    private static final String SHELL_COMMANDS_SERVICE_FILTER = "(&"
            + "(" + COMMANDS_DESCRIPTION_PROPERTY + "=*)"
            + "(" + GROUP_ID_PROPERTY + "=*)"
            + ")";


    /**
     * Bundle Context.
     */
    private BundleContext bc;

    /**
     * Command provides service tracker.
     */
    private ServiceTracker<CustomCommandScope, CustomCommandScope> shellCommandsTracker;

    private Map<ServiceReference<CustomCommandScope>, ServiceRegistration<?>> commandRegistrations =
            new HashMap<ServiceReference<CustomCommandScope>, ServiceRegistration<?>>();

    /**
     * Start the bundle.
     *
     * @param context the bundle context
     * @throws Exception on failure to create the service tracker for the provided filter
     */
    public void start(BundleContext context) throws Exception {
        bc = context;
        shellCommandsTracker = new ServiceTracker<CustomCommandScope, CustomCommandScope>(
                bc, bc.createFilter("(objectClass=" + CustomCommandScope.class.getName() + ")"),
                new ShellCommandsCustomizer());
        shellCommandsTracker.open();

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(CommandProcessor.COMMAND_SCOPE, "debug");
        props.put(CommandProcessor.COMMAND_FUNCTION, DebugCommands.FUNCTIONS);
        bc.registerService(DebugCommands.class.getName(), new DebugCommands(bc), props);
    }

    /**
     * Stop the bundle.
     *
     * @param bundleContext the bundle context
     * @throws Exception on errors
     */
    public void stop(BundleContext bundleContext) throws Exception {
        shellCommandsTracker.close();
        shellCommandsTracker = null;

        bc = null;
    }

    /**
     * Validate Command method.
     *
     * @param service     service newBuilder
     * @param commandName command method name
     * @return <code>true</code> if method is peresent in service, <code>public</code> and
     *         has params <code>PrintStream</code> and <code>String[]</code>, otherwise - <code>false</code>
     */
    private boolean isValidCommandMethod(Object service, String commandName) {
        try {
            service.getClass().getMethod(commandName, InputStream.class, PrintStream.class, String[].class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Command provides service tracker customizer.
     */
    private class ShellCommandsCustomizer implements ServiceTrackerCustomizer<CustomCommandScope, CustomCommandScope> {

        public CustomCommandScope addingService(ServiceReference<CustomCommandScope> reference) {
            CustomCommandScope service = bc.getService(reference);
            Object groupId = service.getScope();
            // if property value null or not String - ignore service
            if (groupId == null || !(groupId instanceof String)) {
                LOG.warning(GROUP_ID_PROPERTY + " property is null or invalid. Ignore service");
                return null;
            }
            // get service ranking propety. if not null - use it on Command services registration
            Map<String, String> commandMap = service.getFunctionMap();
            if (!commandMap.isEmpty()) {
                Dictionary<String, Object> props = new Hashtable<String, Object>();
                Integer ranking = (Integer) reference.getProperty(Constants.SERVICE_RANKING);
                Long serviceId = (Long) reference.getProperty(Constants.SERVICE_ID);
                if (ranking != null) {
                    props.put(Constants.SERVICE_RANKING, ranking);
                }
                props.put(CommandProcessor.COMMAND_SCOPE, groupId);
                props.put(CommandProcessor.COMMAND_FUNCTION,
                        commandMap.keySet().toArray(new String[commandMap.size()]));
                try {
                    // generate class
                    Object commandsProvider =
                            FelixGogoCommandsServiceGenerator.generate(service, commandMap, serviceId.toString());
                    commandRegistrations.put(reference,
                            bc.registerService(commandsProvider.getClass().getName(), commandsProvider, props));
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Unable to initialize group: " + groupId, e);
                }
                return service;
            } else {
                return null;
            }
        }

        public void modifiedService(ServiceReference<CustomCommandScope> reference, CustomCommandScope service) {
            // ignore
        }

        public void removedService(ServiceReference<CustomCommandScope> reference, CustomCommandScope service) {
            // unregister CommandGroup services that belongs to this service registration
            Long serviceId = (Long) reference.getProperty(Constants.SERVICE_ID);
            // detach class
            FelixGogoCommandsServiceGenerator.clean(serviceId.toString());
            ServiceRegistration<?> registration = commandRegistrations.remove(reference);
            if (registration != null) {
                registration.unregister();
            }
            bc.ungetService(reference);
        }
    }
}


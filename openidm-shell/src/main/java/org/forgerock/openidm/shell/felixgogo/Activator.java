/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
 * $Id$
 */
package org.forgerock.openidm.shell.felixgogo;

import org.apache.felix.service.command.CommandProcessor;
import org.forgerock.openidm.shell.CustomCommandScope;
import org.forgerock.openidm.shell.felixgogo.debug.DebugCommands;
import org.osgi.framework.*;
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
 * Felix GoGo shell adapter activator
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class Activator implements BundleActivator {
    private static final Logger LOG = Logger.getLogger(Activator.class.getName());

    private static final String COMMANDS_DESCRIPTION_PROPERTY = "openidm.osgi.shell.commands";
    private static final String GROUP_ID_PROPERTY = "openidm.osgi.shell.group.id";

    /**
     * Felix GoGo shell API supports groups.
     * Filter requires shell commands description and group id
     */
    private static final String SHELL_COMMANDS_SERVICE_FILTER = "(&" +
            "(" + COMMANDS_DESCRIPTION_PROPERTY + "=*)" +
            "(" + GROUP_ID_PROPERTY + "=*)" +
            ")";


    /**
     * Bundle Context
     */
    private BundleContext bc;

    /**
     * Command provides service tracker
     */
    private ServiceTracker shellCommandsTracker;

    private Map<ServiceReference, ServiceRegistration> commandRegistrations = new HashMap<ServiceReference, ServiceRegistration>();

    public void start(BundleContext context) throws Exception {
        bc = context;
        shellCommandsTracker = new ServiceTracker(bc, bc.createFilter("(objectClass=" + CustomCommandScope.class.getName() + ")"),
                new ShellCommandsCustomizer());
        shellCommandsTracker.open();

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(CommandProcessor.COMMAND_SCOPE, "debug");
        props.put(CommandProcessor.COMMAND_FUNCTION, DebugCommands.functions);
        bc.registerService(DebugCommands.class.getName(), new DebugCommands(bc), props);
    }

    public void stop(BundleContext bundleContext) throws Exception {
        shellCommandsTracker.close();
        shellCommandsTracker = null;

        bc = null;
    }

    /**
     * Validate Command method
     *
     * @param service     service instance
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
     * Command provides service tracker customizer
     */
    private class ShellCommandsCustomizer implements ServiceTrackerCustomizer {

        public Object addingService(ServiceReference reference) {
            CustomCommandScope service = (CustomCommandScope) bc.getService(reference);
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
                props.put(CommandProcessor.COMMAND_FUNCTION, commandMap.keySet().toArray(new String[commandMap.size()]));
                try {
                    // generate class
                    Object commandsProvider = FelixGogoCommandsServiceGenerator.generate(service, commandMap, serviceId.toString());
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

        public void modifiedService(ServiceReference reference, Object service) {
            // ignore
        }

        public void removedService(ServiceReference reference, Object service) {
            // unregister CommandGroup services that belongs to this service registration
            Long serviceId = (Long) reference.getProperty(Constants.SERVICE_ID);
            // detach class
            FelixGogoCommandsServiceGenerator.clean(serviceId.toString());
            ServiceRegistration registration = commandRegistrations.remove(reference);
            if (registration != null) {
                registration.unregister();
            }
            bc.ungetService(reference);
        }
    }
}


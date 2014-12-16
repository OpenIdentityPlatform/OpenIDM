/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.shell.felixgogo.debug;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.openidm.core.ServerConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Debug commands.
 */
public class DebugCommands {

    /** the supported functions. */
    public static final String[] FUNCTIONS = {"mockservice", "help"};

    /** Bundle Context. */
    private BundleContext context;

    /**
     * Construct the debug commands.
     * @param context the bundle context.
     */
    public DebugCommands(BundleContext context) {
        this.context = context;
    }


    /**
     * Starts a new object service and registers it with the router.
     *
     * @param session the command session
     * @param params the parameters
     */
    @Descriptor("Start a new object service and register into the router service")
    public void mockservice(CommandSession session, @Descriptor("URL Prefix") String[] params) {
        try {
            InteractiveObjectSetService debugRouter =
                    new InteractiveObjectSetService(Thread.currentThread(), context, session);
            context.addServiceListener(debugRouter, InteractiveObjectSetService.ROUTER_SERVICE_FILTER);
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(ServerConstants.ROUTER_PREFIX, params.length > 0 ? params[0] : "debugrouter");
            ServiceRegistration<?> srv = context.registerService(RequestHandler.class.getName(), debugRouter, props);
            boolean run = true;
            while (run) {
                try {
                    Thread.sleep(1000000000);
                } catch (InterruptedException e) {
                    run = false;
                    session.getConsole().println("Interrupt debug server.");
                }
            }
            context.removeServiceListener(debugRouter);
            srv.unregister();
            session.getConsole().println("Unregister debug server.");
        } catch (InvalidSyntaxException e) {
            //This should never happen
        }
    }

    /**
     * Displays help message.
     *
     * @return the help string
     */
    @Descriptor("Display help messages")
    public String help() {
        return "mockservice\t Start new service";
    }
}

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
package org.forgerock.openidm.shell.felixgogo.debug;


import org.apache.felix.service.command.CommandSession;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class InteractiveObjectSetService implements JsonResource, ServiceListener {

    private final static Logger TRACE = LoggerFactory.getLogger(InteractiveObjectSetService.class);

    public static final String ROUTER_SERVICE_FILTER = "(&(objectClass=" + JsonResource.class.getName() + ")(service.pid=org.forgerock.openidm.router))";

    private JsonResource router = null;

    private CommandSession session;
    private final ObjectMapper mapper = new ObjectMapper();

    private final BundleContext context;
    private final PrintStream console;
    private final InputStream keyboard;
    private final Thread parent;

    public InteractiveObjectSetService(final Thread parent, BundleContext context, CommandSession session) {
        this.parent = parent;
        this.context = context;
        this.session = session;
        ServiceReference ref = context.getServiceReference(ROUTER_SERVICE_FILTER);
        if (null != ref) {
            router = (JsonResource) context.getService(ref);
        }
        //TODO block the console read
        this.console = session.getConsole();
        this.keyboard = session.getKeyboard();
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue handle(JsonValue request) throws JsonResourceException {
        if (null == request) {
            console.println("null)");
        } else {
            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(console, request.getObject());
                console.println(")");
            } catch (IOException e) {
                console.append("Input object serialization exception: ").println(e.getMessage());
            }
        }
        if ("exit".equals(request.get("id").asString())) {
            parent.interrupt();
            return new JsonValue(new HashMap<String, Object>());
        }
        try {
            return readConsole();
        } catch (Exception e) {
            throw new JsonResourceException(500, "sdf", e);
        }
    }

    private JsonResource getRouter() {
        return router;
    }

    private void printInputHelp() {
        PrintWriter out = new PrintWriter(console);
        out.append("router");
        out.append("file");
        out.append("console");
        out.append("exception");
        out.flush();
    }

    private JsonValue readFile(File inputFile) throws Exception {
        try {
            if (inputFile.exists()) {
                Object map = mapper.readValue(inputFile, Map.class);
                return new JsonValue(map);
            }
        } catch (Exception e) {
            console.format("Error reading file: %s. Exception: %s", inputFile.getAbsolutePath(), e.getMessage());
        }
        return new JsonValue(new HashMap());
    }

    private JsonValue readConsole() throws Exception {
        StringBuilder input = new StringBuilder();
        Scanner in = new Scanner(keyboard);
        boolean dataLine = true;
        do {
            String data = in.nextLine();
            dataLine = !".".equalsIgnoreCase(data);
            if (dataLine) {
                input.append(data);
            }
        } while (dataLine);
        Object map = mapper.readValue(input.toString(), Map.class);
        return new JsonValue(map);
    }


    private void printExceptionHelp() {
        console.println("Throw exception:");
        console.println("exception:BadRequestException <error message>");
        console.println("exception:ConflictException <error message>");
        console.println("exception:ForbiddenException <error message>");
        console.println("exception:InternalServerErrorException <error message>");
        console.println("exception:NotFoundException <error message>");
        console.println("exception:ObjectSetException <error message>");
        console.println("exception:PreconditionFailedException <error message>");
        console.println("exception:ServiceUnavailableException <error message>");
    }

    private void throwException(String cmd) {
        int firstSpace = cmd.indexOf(" ");
        String exception;
        String msg = "Default Error Message";
        if (firstSpace > 0) {
            exception = cmd.substring(10, firstSpace);
            msg = cmd.substring(firstSpace);
        } else {
            exception = cmd.substring(10);
        }

    }

    private final String SCOPE_EXCEPTION = "exception";


    @Override
    public void serviceChanged(ServiceEvent event) {
        ServiceReference sr = event.getServiceReference();
        switch (event.getType()) {
            case ServiceEvent.REGISTERED: {
                router = (JsonResource) context.getService(sr);
                break;
            }
            case ServiceEvent.MODIFIED: {
                router = (JsonResource) context.getService(sr);
                break;
            }
            case ServiceEvent.UNREGISTERING: {
                router = null;
                break;
            }
            default:
                break;
        }
    }
}

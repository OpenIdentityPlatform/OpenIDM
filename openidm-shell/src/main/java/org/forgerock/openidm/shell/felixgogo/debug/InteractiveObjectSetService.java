/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS.
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

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.felix.service.command.CommandSession;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ResourceException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ServiceListener.
 */
class InteractiveObjectSetService implements /* RequestHandler, */ServiceListener {

    private final static Logger TRACE = LoggerFactory.getLogger(InteractiveObjectSetService.class);

    public static final String ROUTER_SERVICE_FILTER = "(service.pid=org.forgerock.openidm.router)";

    private ConnectionFactory router = null;

    private CommandSession session;
    private final ObjectMapper mapper = new ObjectMapper();

    private final BundleContext context;
    private final Thread parent;

    public InteractiveObjectSetService(final Thread parent, BundleContext context,
            CommandSession session) {
        this.parent = parent;
        this.context = context;
        this.session = session;
        try {
            ServiceReference<?>[] ref =
                    context.getServiceReferences(ConnectionFactory.class.getName(), ROUTER_SERVICE_FILTER);
            if (null != ref && ref.length > 0) {
                router = (ConnectionFactory) context.getService(ref[0]);
            }
        } catch (InvalidSyntaxException e) {
            // couldn't attach router
        }
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue handle(JsonValue request) throws ResourceException {
        synchronized (this) {
            session.getConsole().println("Incoming request:");
            if (null == request) {
                session.getConsole().println("null");
            } else {
                try {
                    StringWriter wr = new StringWriter();
                    mapper.writerWithDefaultPrettyPrinter().writeValue(wr, request.getObject());
                    session.getConsole().println(wr.toString());
                } catch (IOException e) {
                    session.getConsole().append("Input object serialization exception: ").println(
                            e.getMessage());
                }
            }
            JsonValue response = null;
            Scanner input = new Scanner(session.getKeyboard());
            do {
                switch (printInputHelp(input)) {
                case 1:
                    if (null != router) {
                        return redirect(request, input);
                    } else {
                        session.getConsole().println(
                                "Router is not available, please select something else.");
                    }
                    break;
                case 2:
                    break;
                case 3:
                    response = loadFromConsole(input);
                    break;
                case 4:
                    return null;
                case 5:
                    printExceptionHelp(input);
                    break;
                case 6:
                    parent.interrupt();
                    return new JsonValue(new HashMap<String, Object>());
                default:
                    session.getConsole().println("Your should select something [1..6]");
                }
            } while (null == response);
            return response;
        }
    }

    private ConnectionFactory getRouter() {
        return router;
    }

    private int printInputHelp(Scanner input) {
        session.getConsole().println("Chose one from the following option and type in the number:");
        session.getConsole().println("1: Redirect to a router call");
        session.getConsole().println("2: Read response from file");
        session.getConsole().println("3: Read response from console");
        session.getConsole().println("4: Return null");
        session.getConsole().println("5: Throw JsonResourceException");
        session.getConsole().println("6: Exit and shutdown the service");
        return input.nextInt();
    }

    private JsonValue redirect(JsonValue request, Scanner input) throws ResourceException {
        session.getConsole().append("Current id: ").println(request.get("id").asString());
        session.getConsole().print("Type in the new id: ");
        String id = input.next();
        request.put("id", id);
        return null;  // getRouter().handle(request);
    }

    private JsonValue readFile(File inputFile) throws Exception {
        try {
            if (inputFile.exists()) {
                Object map = mapper.readValue(inputFile, Map.class);
                return new JsonValue(map);
            }
        } catch (Exception e) {
            session.getConsole().format("Error reading file: %s. Exception: %s",
                    inputFile.getAbsolutePath(), e.getMessage());
        }
        return json(object());
    }

    private JsonValue loadFromConsole(Scanner scanner) {
        session.getConsole().println();
        session.getConsole().println("> Press ctrl-D to finish input");
        session.getConsole().println("Start data input:");
        String input = null;
        StringBuilder stringBuilder = new StringBuilder();
        while (scanner.hasNext()) {
            input = scanner.next();
            if (null == input) {
                // control-D pressed
                break;
            } else {
                stringBuilder.append(input);
            }
        }
        try {
            Object map = mapper.readValue(stringBuilder.toString(), Map.class);
            return new JsonValue(map);
        } catch (IOException e) {
            session.getConsole().append("[Exception] Failed to read input from console. Reason: ")
                    .println(e);
            return null;
        }
    }

    private void printExceptionHelp(Scanner scanner) throws ResourceException {
        session.getConsole().println("Throw a JsonResourceException");
        session.getConsole().print("Type in the numeric code of the exception: ");
        throw ResourceException.getException(scanner.nextInt(), "Test exception");
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        ServiceReference<?> sr = event.getServiceReference();
        switch (event.getType()) {
        case ServiceEvent.REGISTERED: {
            router = (ConnectionFactory) context.getService(sr);
            break;
        }
        case ServiceEvent.MODIFIED: {
            router = (ConnectionFactory) context.getService(sr);
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
